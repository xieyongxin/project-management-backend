package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskVersion;
import com.ruoyi.system.domain.Requirement;

public interface ITaskService
{
    Task createTask(Long projectId, Long creatorId, Long requirementId, String title, String description,
        String status, List<String> categoryValues, List<Long> ownerIds);

    Task updateTaskStatus(Long projectId, Long taskId, Long operatorId, String status);

    Task updateTaskToLatestRequirement(Long projectId, Long taskId, Long operatorId,
        String title, String description);

    Task deleteTask(Long projectId, Long taskId, Long operatorId);

    List<Task> selectTasksForUser(Long projectId, Long userId);

    Task selectTaskForUser(Long projectId, Long taskId, Long userId);

    List<TaskVersion> selectTaskVersionsForUser(Long projectId, Long taskId, Long userId);

    List<TaskVersion> compareTaskVersionsForUser(Long projectId, Long taskId, Long leftVersionId,
        Long rightVersionId, Long userId);

    List<SysDictData> selectActiveStatuses(Long projectId, Long userId);

    List<SysDictData> selectActiveCategories(Long projectId, Long userId);

    List<Requirement> selectAvailableRequirements(Long projectId, Long userId);
}
