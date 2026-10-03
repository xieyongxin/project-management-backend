package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementVersion;

public interface IRequirementService
{
    Requirement createRequirement(Long projectId, Long creatorId, String title, String content,
        String status, List<Long> ownerIds);

    Requirement updateRequirementStatus(Long projectId, Long requirementId, Long operatorId, String status);

    Requirement updateRequirementContent(Long projectId, Long requirementId, Long operatorId,
        String title, String content);

    List<Requirement> selectRequirementsForUser(Long projectId, Long userId);

    Requirement selectRequirementForUser(Long projectId, Long requirementId, Long userId);

    List<RequirementVersion> selectRequirementVersionsForUser(Long projectId, Long requirementId, Long userId);

    List<RequirementVersion> compareRequirementVersionsForUser(Long projectId, Long requirementId,
        Long leftVersionId, Long rightVersionId, Long userId);

    List<SysDictData> selectActiveStatuses(Long projectId, Long userId);
}
