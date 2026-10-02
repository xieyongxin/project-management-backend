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
import com.ruoyi.system.domain.RequirementVersion;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.mapper.RequirementMapper;
import com.ruoyi.system.service.IRequirementService;

@Service
public class RequirementServiceImpl implements IRequirementService
{
    private static final String REQUIREMENT_STATUS_TYPE = "pm_requirement_status";

    private final ProjectMapper projectMapper;
    private final RequirementMapper requirementMapper;
    private final ProjectOperationLogMapper projectOperationLogMapper;

    public RequirementServiceImpl(ProjectMapper projectMapper, RequirementMapper requirementMapper,
        ProjectOperationLogMapper projectOperationLogMapper)
    {
        this.projectMapper = projectMapper;
        this.requirementMapper = requirementMapper;
        this.projectOperationLogMapper = projectOperationLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Requirement createRequirement(Long projectId, Long creatorId, String title, String content,
        String status, List<Long> ownerIds)
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

        String normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isEmpty())
        {
            throw new ServiceException("需求标题不能为空", HttpStatus.BAD_REQUEST);
        }
        if (normalizedTitle.length() > 255)
        {
            throw new ServiceException("需求标题长度不能超过255个字符", HttpStatus.BAD_REQUEST);
        }
        String normalizedContent = content == null ? "" : content.trim();
        if (!hasContent(normalizedContent))
        {
            throw new ServiceException("需求正文不能为空", HttpStatus.BAD_REQUEST);
        }
        if (status == null || status.trim().isEmpty())
        {
            throw new ServiceException("需求状态不能为空", HttpStatus.BAD_REQUEST);
        }
        String normalizedStatus = status.trim();
        SysDictData statusData = requirementMapper.selectActiveRequirementStatus(normalizedStatus);
        if (statusData == null)
        {
            throw new ServiceException("需求状态不存在或已停用", HttpStatus.BAD_REQUEST);
        }

        List<Long> normalizedOwnerIds = normalizeOwnerIds(ownerIds);
        if (normalizedOwnerIds.isEmpty())
        {
            throw new ServiceException("需求至少需要一名负责人", HttpStatus.BAD_REQUEST);
        }
        List<RequirementOwner> members = requirementMapper.selectProjectMembersByIds(projectId, normalizedOwnerIds);
        if (members == null || members.size() != normalizedOwnerIds.size())
        {
            throw new ServiceException("需求负责人必须是当前项目成员", HttpStatus.BAD_REQUEST);
        }

        Date now = new Date();
        Requirement requirement = new Requirement();
        requirement.setProjectId(projectId);
        requirement.setCreatorId(creatorId);
        requirement.setStatus(normalizedStatus);
        requirement.setIsDeleted(0);
        requirement.setCreateTime(now);
        requirement.setUpdateTime(now);
        if (requirementMapper.insertRequirement(requirement) != 1)
        {
            throw new ServiceException("创建需求失败");
        }

        RequirementVersion version = new RequirementVersion();
        version.setRequirementId(requirement.getRequirementId());
        version.setVersionNo(1);
        version.setTitle(normalizedTitle);
        version.setContent(normalizedContent);
        version.setAttachmentSnapshot("[]");
        version.setCreatedBy(creatorId);
        version.setCreateTime(now);
        if (requirementMapper.insertRequirementVersion(version) != 1
            || requirementMapper.updateCurrentVersion(requirement.getRequirementId(), version.getVersionId()) != 1)
        {
            throw new ServiceException("创建需求版本失败");
        }
        for (Long ownerId : normalizedOwnerIds)
        {
            if (requirementMapper.insertRequirementOwner(requirement.getRequirementId(), ownerId) != 1)
            {
                throw new ServiceException("保存需求负责人失败");
            }
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(creatorId);
        log.setOperationType("REQUIREMENT_CREATE");
        log.setDetail("创建需求");
        log.setCreateTime(now);
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectRequirementForUser(projectId, requirement.getRequirementId(), creatorId);
    }

    @Override
    public List<Requirement> selectRequirementsForUser(Long projectId, Long userId)
    {
        if (projectId == null || userId == null || projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        List<Requirement> requirements = requirementMapper.selectRequirementsForUser(projectId, userId);
        if (requirements != null)
        {
            requirements.forEach(requirement -> requirement.setOwners(
                requirementMapper.selectRequirementOwners(requirement.getRequirementId())));
        }
        return requirements;
    }

    @Override
    public List<SysDictData> selectActiveStatuses(Long projectId, Long userId)
    {
        if (projectId == null || userId == null || projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        return requirementMapper.selectActiveRequirementStatuses();
    }

    private Requirement selectRequirementForUser(Long projectId, Long requirementId, Long userId)
    {
        Requirement requirement = requirementMapper.selectRequirementForUser(projectId, requirementId, userId);
        if (requirement != null)
        {
            requirement.setOwners(requirementMapper.selectRequirementOwners(requirementId));
        }
        return requirement;
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
                throw new ServiceException("需求负责人不能为空", HttpStatus.BAD_REQUEST);
            }
            unique.add(ownerId);
        }
        return new ArrayList<>(unique);
    }

    private boolean hasContent(String content)
    {
        return content != null && !content.replaceAll("<[^>]*>", "")
            .replace("&nbsp;", "").trim().isEmpty();
    }
}
