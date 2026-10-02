package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Requirement;

public interface IRequirementService
{
    Requirement createRequirement(Long projectId, Long creatorId, String title, String content,
        String status, List<Long> ownerIds);

    List<Requirement> selectRequirementsForUser(Long projectId, Long userId);

    List<SysDictData> selectActiveStatuses(Long projectId, Long userId);
}
