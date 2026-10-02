package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Task;

public interface ITaskService
{
    Task createTask(Long projectId, Long creatorId, Long requirementId, String title, String description,
        String status, List<String> categoryValues, List<Long> ownerIds);

    List<SysDictData> selectActiveStatuses(Long projectId, Long userId);

    List<SysDictData> selectActiveCategories(Long projectId, Long userId);
}
