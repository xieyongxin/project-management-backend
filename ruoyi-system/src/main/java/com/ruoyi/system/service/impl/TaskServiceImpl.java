package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementOwner;
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskCategory;
import com.ruoyi.system.domain.TaskOwner;
import com.ruoyi.system.domain.TaskVersion;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.mapper.RequirementMapper;
import com.ruoyi.system.mapper.TaskMapper;
import com.ruoyi.system.service.ITaskService;

@Service
public class TaskServiceImpl implements ITaskService
{
    private static final String TASK_STATUS_TYPE = "pm_task_status";
    private static final String TASK_CATEGORY_TYPE = "pm_task_category";

    private final ProjectMapper projectMapper;
    private final RequirementMapper requirementMapper;
    private final TaskMapper taskMapper;
    private final ProjectOperationLogMapper projectOperationLogMapper;

    public TaskServiceImpl(ProjectMapper projectMapper, RequirementMapper requirementMapper,
        TaskMapper taskMapper, ProjectOperationLogMapper projectOperationLogMapper)
    {
        this.projectMapper = projectMapper;
        this.requirementMapper = requirementMapper;
        this.taskMapper = taskMapper;
        this.projectOperationLogMapper = projectOperationLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(Long projectId, Long creatorId, Long requirementId, String title, String description,
        String status, List<String> categoryValues, List<Long> ownerIds)
    {
        Project project = projectId == null || creatorId == null
            ? null : projectMapper.selectProjectForUser(projectId, creatorId);
        if (project == null)
        {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Project.STATUS_ARCHIVED.equals(project.getStatus()))
        {
            throw new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST);
        }

        Requirement requirement = requirementId == null
            ? null : requirementMapper.selectRequirementForUser(projectId, requirementId, creatorId);
        if (requirement == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(requirement.getIsDeleted()))
        {
            throw new ServiceException("已删除需求不能新建任务关联", HttpStatus.BAD_REQUEST);
        }
        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty())
        {
            throw new ServiceException("任务标题不能为空", HttpStatus.BAD_REQUEST);
        }
        if (normalizedTitle.length() > 255)
        {
            throw new ServiceException("任务标题长度不能超过255个字符", HttpStatus.BAD_REQUEST);
        }
        String normalizedDescription = description == null ? "" : description.trim();
        if (normalizedDescription.isEmpty())
        {
            throw new ServiceException("任务说明不能为空", HttpStatus.BAD_REQUEST);
        }
        String normalizedStatus = status == null ? "" : status.trim();
        SysDictData statusData = findActiveValue(taskMapper.selectActiveTaskStatuses(), normalizedStatus);
        if (statusData == null)
        {
            throw new ServiceException("任务状态不存在或已停用", HttpStatus.BAD_REQUEST);
        }

        List<String> normalizedCategories = normalizeCategories(categoryValues);
        if (normalizedCategories.isEmpty())
        {
            throw new ServiceException("任务至少需要一个分类", HttpStatus.BAD_REQUEST);
        }
        Set<String> activeCategories = values(taskMapper.selectActiveTaskCategories());
        if (!activeCategories.containsAll(normalizedCategories))
        {
            throw new ServiceException("任务分类不存在或已停用", HttpStatus.BAD_REQUEST);
        }

        List<Long> normalizedOwnerIds = normalizeOwnerIds(ownerIds);
        if (normalizedOwnerIds.isEmpty())
        {
            throw new ServiceException("任务至少需要一名负责人", HttpStatus.BAD_REQUEST);
        }
        List<RequirementOwner> members = requirementMapper.selectProjectMembersByIds(projectId, normalizedOwnerIds);
        if (members == null || members.size() != normalizedOwnerIds.size())
        {
            throw new ServiceException("任务负责人必须是当前项目成员", HttpStatus.BAD_REQUEST);
        }

        Date now = new Date();
        Task task = new Task();
        task.setProjectId(projectId);
        task.setRequirementId(requirementId);
        task.setCreatorId(creatorId);
        task.setStatus(normalizedStatus);
        task.setIsDeleted(0);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        if (taskMapper.insertTask(task) != 1)
        {
            throw new ServiceException("创建任务失败");
        }

        TaskVersion version = new TaskVersion();
        version.setTaskId(task.getTaskId());
        version.setVersionNo(1);
        version.setTitle(normalizedTitle);
        version.setDescription(normalizedDescription);
        version.setRequirementVersionId(requirement.getCurrentVersionId());
        version.setCreatedBy(creatorId);
        version.setCreateTime(now);
        if (taskMapper.insertTaskVersion(version) != 1
            || taskMapper.updateCurrentVersion(task.getTaskId(), version.getVersionId()) != 1)
        {
            throw new ServiceException("创建任务版本失败");
        }
        for (String category : normalizedCategories)
        {
            if (taskMapper.insertTaskCategory(task.getTaskId(), category) != 1)
            {
                throw new ServiceException("保存任务分类失败");
            }
        }
        for (Long ownerId : normalizedOwnerIds)
        {
            if (taskMapper.insertTaskOwner(task.getTaskId(), ownerId) != 1)
            {
                throw new ServiceException("保存任务负责人失败");
            }
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(creatorId);
        log.setOperationType("TASK_CREATE");
        log.setDetail("创建任务");
        log.setCreateTime(now);
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectTaskForUserInternal(projectId, task.getTaskId(), creatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task updateTaskStatus(Long projectId, Long taskId, Long operatorId, String status)
    {
        Project project = projectId == null || operatorId == null
            ? null : projectMapper.selectProjectForUser(projectId, operatorId);
        if (project == null)
        {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Project.STATUS_ARCHIVED.equals(project.getStatus()))
        {
            throw new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST);
        }
        if (taskId == null)
        {
            throw new ServiceException("任务不存在或无权访问", HttpStatus.NOT_FOUND);
        }

        String normalizedStatus = status == null ? "" : status.trim();
        SysDictData statusData = findActiveValue(taskMapper.selectActiveTaskStatuses(), normalizedStatus);
        if (statusData == null)
        {
            throw new ServiceException("任务状态不存在或已停用", HttpStatus.BAD_REQUEST);
        }

        Task current = taskMapper.selectTaskForUser(projectId, taskId, operatorId);
        if (current == null)
        {
            throw new ServiceException("任务不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("已删除任务不能修改状态", HttpStatus.BAD_REQUEST);
        }
        if (normalizedStatus.equals(current.getStatus()))
        {
            return enrichTask(current);
        }
        if (taskMapper.updateTaskStatus(projectId, taskId, normalizedStatus) != 1)
        {
            throw new ServiceException("更新任务状态失败");
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("TASK_STATUS_UPDATE");
        log.setDetail("更新任务状态：" + statusLabel(current.getStatus(), current.getStatusLabel())
            + " -> " + statusLabel(normalizedStatus, statusData.getDictLabel()));
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectTaskForUserInternal(projectId, taskId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task deleteTask(Long projectId, Long taskId, Long operatorId)
    {
        Project project = projectId == null || operatorId == null
            ? null : projectMapper.selectProjectForUser(projectId, operatorId);
        if (project == null)
        {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Project.STATUS_ARCHIVED.equals(project.getStatus()))
        {
            throw new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST);
        }
        Task current = taskId == null ? null : taskMapper.selectTaskForUser(projectId, taskId, operatorId);
        if (current == null)
        {
            throw new ServiceException("任务不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("任务已删除", HttpStatus.BAD_REQUEST);
        }
        if (taskMapper.logicalDeleteTask(projectId, taskId) != 1)
        {
            throw new ServiceException("删除任务失败");
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("TASK_DELETE");
        log.setDetail("删除任务 #" + taskId);
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectTaskForUserInternal(projectId, taskId, operatorId);
    }

    @Override
    public List<Task> selectTasksForUser(Long projectId, Long userId)
    {
        if (projectId == null || userId == null)
        {
            return null;
        }
        List<Task> tasks = taskMapper.selectTasksForUser(projectId, userId);
        if (projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        if (tasks != null)
        {
            tasks.forEach(task -> {
                task.setCategories(taskMapper.selectTaskCategories(task.getTaskId()));
                task.setOwners(taskMapper.selectTaskOwners(task.getTaskId()));
            });
        }
        return tasks;
    }

    @Override
    public Task selectTaskForUser(Long projectId, Long taskId, Long userId)
    {
        if (!isProjectMember(projectId, userId) || taskId == null)
        {
            return null;
        }
        return enrichTask(taskMapper.selectTaskForUser(projectId, taskId, userId));
    }

    @Override
    public List<TaskVersion> selectTaskVersionsForUser(Long projectId, Long taskId, Long userId)
    {
        if (!isProjectMember(projectId, userId) || taskId == null
            || taskMapper.selectTaskForUser(projectId, taskId, userId) == null)
        {
            return null;
        }
        return taskMapper.selectTaskVersionsForUser(projectId, taskId, userId);
    }

    @Override
    public List<TaskVersion> compareTaskVersionsForUser(Long projectId, Long taskId, Long leftVersionId,
        Long rightVersionId, Long userId)
    {
        List<TaskVersion> versions = selectTaskVersionsForUser(projectId, taskId, userId);
        if (versions == null)
        {
            return null;
        }
        if (leftVersionId == null || rightVersionId == null || leftVersionId.equals(rightVersionId))
        {
            throw new ServiceException("任务版本对比必须选择两个不同版本", HttpStatus.BAD_REQUEST);
        }
        TaskVersion left = versions.stream().filter(version -> leftVersionId.equals(version.getVersionId()))
            .findFirst().orElse(null);
        TaskVersion right = versions.stream().filter(version -> rightVersionId.equals(version.getVersionId()))
            .findFirst().orElse(null);
        if (left == null || right == null)
        {
            return null;
        }
        return List.of(left, right);
    }

    @Override
    public List<SysDictData> selectActiveStatuses(Long projectId, Long userId)
    {
        if (!isProjectMember(projectId, userId))
        {
            return null;
        }
        return taskMapper.selectActiveTaskStatuses();
    }

    @Override
    public List<SysDictData> selectActiveCategories(Long projectId, Long userId)
    {
        if (!isProjectMember(projectId, userId))
        {
            return null;
        }
        return taskMapper.selectActiveTaskCategories();
    }

    @Override
    public List<Requirement> selectAvailableRequirements(Long projectId, Long userId)
    {
        if (!isProjectMember(projectId, userId))
        {
            return null;
        }
        List<Requirement> requirements = requirementMapper.selectRequirementsForUser(projectId, userId);
        if (requirements == null)
        {
            return List.of();
        }
        return requirements.stream()
            .filter(requirement -> !Integer.valueOf(1).equals(requirement.getIsDeleted()))
            .toList();
    }

    private Task selectTaskForUserInternal(Long projectId, Long taskId, Long userId)
    {
        Task task = taskMapper.selectTaskForUser(projectId, taskId, userId);
        return enrichTask(task);
    }

    private Task enrichTask(Task task)
    {
        if (task != null)
        {
            task.setCategories(taskMapper.selectTaskCategories(task.getTaskId()));
            task.setOwners(taskMapper.selectTaskOwners(task.getTaskId()));
        }
        return task;
    }

    private boolean isProjectMember(Long projectId, Long userId)
    {
        return projectId != null && userId != null && projectMapper.selectProjectForUser(projectId, userId) != null;
    }

    private SysDictData findActiveValue(List<SysDictData> values, String value)
    {
        if (value == null || value.isEmpty() || values == null)
        {
            return null;
        }
        return values.stream().filter(item -> value.equals(item.getDictValue())).findFirst().orElse(null);
    }

    private String statusLabel(String status, String label)
    {
        return label == null || label.trim().isEmpty() ? status : label;
    }

    private Set<String> values(List<SysDictData> values)
    {
        Set<String> result = new LinkedHashSet<>();
        if (values != null)
        {
            values.forEach(item -> {
                if (item != null && item.getDictValue() != null)
                {
                    result.add(item.getDictValue());
                }
            });
        }
        return result;
    }

    private List<String> normalizeCategories(List<String> categories)
    {
        if (categories == null)
        {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String category : categories)
        {
            if (category == null || category.trim().isEmpty())
            {
                throw new ServiceException("任务分类不能为空", HttpStatus.BAD_REQUEST);
            }
            unique.add(category.trim());
        }
        return new ArrayList<>(unique);
    }

    private List<Long> normalizeOwnerIds(List<Long> ownerIds)
    {
        if (ownerIds == null)
        {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long ownerId : ownerIds)
        {
            if (ownerId == null)
            {
                throw new ServiceException("任务负责人不能为空", HttpStatus.BAD_REQUEST);
            }
            unique.add(ownerId);
        }
        return new ArrayList<>(unique);
    }
}
