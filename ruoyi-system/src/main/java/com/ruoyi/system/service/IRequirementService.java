package com.ruoyi.system.service;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.domain.RequirementVersion;
import com.ruoyi.system.domain.RequirementAttachment;

public interface IRequirementService
{
    Requirement createRequirement(Long projectId, Long creatorId, String title, String content,
        String status, List<Long> ownerIds);

    Requirement updateRequirementStatus(Long projectId, Long requirementId, Long operatorId, String status);

    Requirement updateRequirementOwners(Long projectId, Long requirementId, Long operatorId, List<Long> ownerIds);

    Requirement updateRequirementContent(Long projectId, Long requirementId, Long operatorId,
        String title, String content);

    Requirement deleteRequirement(Long projectId, Long requirementId, Long operatorId);

    List<Requirement> selectRequirementsForUser(Long projectId, Long userId);

    Requirement selectRequirementForUser(Long projectId, Long requirementId, Long userId);

    List<RequirementVersion> selectRequirementVersionsForUser(Long projectId, Long requirementId, Long userId);

    List<RequirementVersion> compareRequirementVersionsForUser(Long projectId, Long requirementId,
        Long leftVersionId, Long rightVersionId, Long userId);

    List<SysDictData> selectActiveStatuses(Long projectId, Long userId);

    Requirement updateRequirementAttachments(Long projectId, Long requirementId, Long operatorId,
        List<MultipartFile> files);

    List<RequirementAttachment> selectRequirementAttachmentsForUser(Long projectId, Long requirementId,
        Long userId, Long versionId);

    RequirementAttachment selectRequirementAttachmentForUser(Long projectId, Long requirementId,
        Long attachmentId, Long userId);
}
