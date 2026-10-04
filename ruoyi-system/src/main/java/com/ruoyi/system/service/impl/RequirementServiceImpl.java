package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
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
import com.ruoyi.system.domain.RequirementAttachment;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.mapper.RequirementMapper;
import com.ruoyi.system.service.IRequirementService;

@Service
public class RequirementServiceImpl implements IRequirementService
{
    private static final String REQUIREMENT_STATUS_TYPE = "pm_requirement_status";
    private static final String[] ATTACHMENT_EXTENSIONS = { "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg" };
    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;
    private static final long MAX_VERSION_ATTACHMENT_SIZE = 100L * 1024 * 1024;
    private static final int MAX_ATTACHMENT_COUNT = 10;

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
        return selectRequirementForUserInternal(projectId, requirement.getRequirementId(), creatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Requirement updateRequirementStatus(Long projectId, Long requirementId, Long operatorId, String status)
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
        if (requirementId == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }

        String normalizedStatus = status == null ? "" : status.trim();
        SysDictData statusData = requirementMapper.selectActiveRequirementStatus(normalizedStatus);
        if (statusData == null)
        {
            throw new ServiceException("需求状态不存在或已停用", HttpStatus.BAD_REQUEST);
        }

        Requirement current = requirementMapper.selectRequirementForUser(projectId, requirementId, operatorId);
        if (current == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("已删除需求不能修改状态", HttpStatus.BAD_REQUEST);
        }
        if (normalizedStatus.equals(current.getStatus()))
        {
            return selectRequirementForUserInternal(projectId, requirementId, operatorId);
        }
        if (requirementMapper.updateRequirementStatus(projectId, requirementId, normalizedStatus) != 1)
        {
            throw new ServiceException("更新需求状态失败");
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("REQUIREMENT_STATUS_UPDATE");
        log.setDetail("更新需求状态：" + statusLabel(current.getStatus(), current.getStatusLabel())
            + " -> " + statusLabel(normalizedStatus, statusData.getDictLabel()));
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectRequirementForUserInternal(projectId, requirementId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Requirement updateRequirementOwners(Long projectId, Long requirementId, Long operatorId,
        List<Long> ownerIds)
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

        Requirement current = requirementId == null ? null
            : selectRequirementForUserInternal(projectId, requirementId, operatorId);
        if (current == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("已删除需求不能修改负责人", HttpStatus.BAD_REQUEST);
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

        Set<Long> currentOwnerIds = current.getOwners() == null ? Set.of()
            : current.getOwners().stream().map(RequirementOwner::getUserId).collect(Collectors.toSet());
        if (currentOwnerIds.equals(new LinkedHashSet<>(normalizedOwnerIds)))
        {
            return current;
        }
        if (requirementMapper.deleteRequirementOwners(requirementId) < 0)
        {
            throw new ServiceException("更新需求负责人失败");
        }
        for (Long ownerId : normalizedOwnerIds)
        {
            if (requirementMapper.insertRequirementOwner(requirementId, ownerId) != 1)
            {
                throw new ServiceException("更新需求负责人失败");
            }
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("REQUIREMENT_OWNER_UPDATE");
        log.setDetail("更新需求负责人");
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectRequirementForUserInternal(projectId, requirementId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Requirement updateRequirementContent(Long projectId, Long requirementId, Long operatorId,
        String title, String content)
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
        if (requirementId == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
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

        Requirement current = selectRequirementForUserInternal(projectId, requirementId, operatorId);
        if (current == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("已删除需求不能修改内容", HttpStatus.BAD_REQUEST);
        }
        if (normalizedTitle.equals(current.getTitle()) && normalizedContent.equals(current.getContent()))
        {
            return current;
        }

        List<RequirementVersion> versions = requirementMapper.selectRequirementVersionsForUser(
            projectId, requirementId, operatorId);
        RequirementVersion currentVersion = versions == null ? null : versions.stream()
            .filter(version -> current.getCurrentVersionId() != null
                && current.getCurrentVersionId().equals(version.getVersionId()))
            .findFirst().orElse(null);
        RequirementVersion nextVersion = new RequirementVersion();
        nextVersion.setRequirementId(requirementId);
        nextVersion.setVersionNo((current.getCurrentVersionNo() == null ? 0 : current.getCurrentVersionNo()) + 1);
        nextVersion.setTitle(normalizedTitle);
        nextVersion.setContent(normalizedContent);
        nextVersion.setAttachmentSnapshot(currentVersion == null || currentVersion.getAttachmentSnapshot() == null
            ? "[]" : currentVersion.getAttachmentSnapshot());
        nextVersion.setCreatedBy(operatorId);
        nextVersion.setCreateTime(new Date());
        if (requirementMapper.insertRequirementVersion(nextVersion) != 1
            || requirementMapper.updateCurrentVersion(requirementId, nextVersion.getVersionId()) != 1)
        {
            throw new ServiceException("更新需求内容失败");
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("REQUIREMENT_CONTENT_UPDATE");
        log.setDetail("更新需求内容：v" + current.getCurrentVersionNo() + " -> v" + nextVersion.getVersionNo());
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectRequirementForUserInternal(projectId, requirementId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Requirement deleteRequirement(Long projectId, Long requirementId, Long operatorId)
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
        if (requirementId == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }

        Requirement current = selectRequirementForUserInternal(projectId, requirementId, operatorId);
        if (current == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("需求已删除", HttpStatus.BAD_REQUEST);
        }
        if (requirementMapper.countActiveTasksByRequirement(projectId, requirementId) > 0)
        {
            throw new ServiceException("需求存在未删除任务，不能删除", HttpStatus.BAD_REQUEST);
        }
        if (requirementMapper.logicalDeleteRequirement(projectId, requirementId) != 1)
        {
            throw new ServiceException("删除需求失败");
        }

        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("REQUIREMENT_DELETE");
        log.setDetail("需求 #" + requirementId + " 已删除");
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectRequirementForUserInternal(projectId, requirementId, operatorId);
    }

    @Override
    public List<Requirement> selectRequirementsForUser(Long projectId, Long userId)
    {
        if (projectId == null || userId == null)
        {
            return null;
        }
        List<Requirement> requirements = requirementMapper.selectRequirementsForUser(projectId, userId);
        if (projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        if (requirements != null)
        {
            requirements.forEach(requirement -> requirement.setOwners(
                requirementMapper.selectRequirementOwners(requirement.getRequirementId())));
        }
        return requirements;
    }

    @Override
    public Requirement selectRequirementForUser(Long projectId, Long requirementId, Long userId)
    {
        return selectRequirementForUserInternal(projectId, requirementId, userId);
    }

    @Override
    public List<RequirementVersion> selectRequirementVersionsForUser(Long projectId, Long requirementId,
        Long userId)
    {
        if (projectId == null || requirementId == null || userId == null
            || projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        if (requirementMapper.selectRequirementForUser(projectId, requirementId, userId) == null)
        {
            return null;
        }
        List<RequirementVersion> versions = requirementMapper.selectRequirementVersionsForUser(projectId, requirementId, userId);
        if (versions != null)
        {
            versions.forEach(version -> version.setAttachments(selectRequirementAttachmentsForUser(projectId,
                requirementId, userId, version.getVersionId())));
        }
        return versions;
    }

    @Override
    public List<RequirementVersion> compareRequirementVersionsForUser(Long projectId, Long requirementId,
        Long leftVersionId, Long rightVersionId, Long userId)
    {
        List<RequirementVersion> versions = selectRequirementVersionsForUser(projectId, requirementId, userId);
        if (versions == null)
        {
            return null;
        }
        if (leftVersionId == null || rightVersionId == null || leftVersionId.equals(rightVersionId))
        {
            throw new ServiceException("需求版本对比必须选择两个不同版本", HttpStatus.BAD_REQUEST);
        }
        RequirementVersion left = versions.stream().filter(version -> leftVersionId.equals(version.getVersionId()))
            .findFirst().orElse(null);
        RequirementVersion right = versions.stream().filter(version -> rightVersionId.equals(version.getVersionId()))
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
        if (projectId == null || userId == null || projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        return requirementMapper.selectActiveRequirementStatuses();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Requirement updateRequirementAttachments(Long projectId, Long requirementId, Long operatorId,
        List<MultipartFile> files)
    {
        Project project = projectId == null || operatorId == null ? null
            : projectMapper.selectProjectForUser(projectId, operatorId);
        if (project == null)
        {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Project.STATUS_ARCHIVED.equals(project.getStatus()))
        {
            throw new ServiceException("归档项目不能开展新的业务操作", HttpStatus.BAD_REQUEST);
        }
        Requirement current = selectRequirementForUserInternal(projectId, requirementId, operatorId);
        if (current == null)
        {
            throw new ServiceException("需求不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(current.getIsDeleted()))
        {
            throw new ServiceException("已删除需求不能修改附件", HttpStatus.BAD_REQUEST);
        }
        List<RequirementAttachment> existing = requirementMapper.selectRequirementAttachments(requirementId);
        List<MultipartFile> actualFiles = files == null ? List.of() : files.stream()
            .filter(file -> file != null && !file.isEmpty()).toList();
        if (actualFiles.isEmpty())
        {
            return current;
        }
        List<RequirementVersion> versions = requirementMapper.selectRequirementVersionsForUser(projectId,
            requirementId, operatorId);
        RequirementVersion currentVersion = versions == null ? null : versions.stream()
            .filter(v -> current.getCurrentVersionId() != null && current.getCurrentVersionId().equals(v.getVersionId()))
            .findFirst().orElse(null);
        if (currentVersion == null)
        {
            throw new ServiceException("需求当前版本不存在", HttpStatus.BAD_REQUEST);
        }
        List<Long> existingIds = snapshotIds(currentVersion.getAttachmentSnapshot());
        if (existingIds.size() + actualFiles.size() > MAX_ATTACHMENT_COUNT)
        {
            throw new ServiceException("每个需求版本最多保存10个附件", HttpStatus.BAD_REQUEST);
        }
        long existingSize = existing.stream().filter(item -> existingIds.contains(item.getAttachmentId()))
            .mapToLong(item -> item.getFileSize() == null ? 0 : item.getFileSize()).sum();
        long newSize = 0;
        Date now = new Date();
        List<RequirementAttachment> uploaded = new ArrayList<>();
        for (MultipartFile file : actualFiles)
        {
            if (file.getSize() > MAX_ATTACHMENT_SIZE)
            {
                throw new ServiceException("单个附件不能超过20MB", HttpStatus.BAD_REQUEST);
            }
            String extension = FileUploadUtils.getExtension(file).toLowerCase();
            if (!FileUploadUtils.isAllowedExtension(extension, ATTACHMENT_EXTENSIONS))
            {
                throw new ServiceException("附件格式仅支持 PDF、DOC、DOCX、XLS、XLSX、PNG、JPG、JPEG", HttpStatus.BAD_REQUEST);
            }
            newSize += file.getSize();
            String baseDir = RuoYiConfig.getProfile() + "/requirement/" + projectId + "/" + requirementId;
            String storagePath;
            try
            {
                storagePath = FileUploadUtils.upload(baseDir, file, ATTACHMENT_EXTENSIONS, true);
            }
            catch (Exception e)
            {
                throw new ServiceException("保存需求附件失败：" + e.getMessage(), HttpStatus.BAD_REQUEST);
            }
            RequirementAttachment attachment = new RequirementAttachment();
            attachment.setRequirementId(requirementId);
            attachment.setOriginalName(file.getOriginalFilename());
            attachment.setStoragePath(storagePath);
            attachment.setExtension(extension);
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setCreatedBy(operatorId);
            attachment.setCreateTime(now);
            if (requirementMapper.insertRequirementAttachment(attachment) != 1)
            {
                throw new ServiceException("保存需求附件失败");
            }
            uploaded.add(attachment);
        }
        if (existingSize + newSize > MAX_VERSION_ATTACHMENT_SIZE)
        {
            throw new ServiceException("需求版本附件合计不能超过100MB", HttpStatus.BAD_REQUEST);
        }
        List<Long> attachmentIds = new ArrayList<>(existingIds);
        attachmentIds.addAll(uploaded.stream().map(RequirementAttachment::getAttachmentId).toList());
        RequirementVersion next = new RequirementVersion();
        next.setRequirementId(requirementId);
        next.setVersionNo((current.getCurrentVersionNo() == null ? 0 : current.getCurrentVersionNo()) + 1);
        next.setTitle(currentVersion.getTitle());
        next.setContent(currentVersion.getContent());
        next.setAttachmentSnapshot(JSON.toJSONString(attachmentIds));
        next.setCreatedBy(operatorId);
        next.setCreateTime(now);
        if (requirementMapper.insertRequirementVersion(next) != 1
            || requirementMapper.updateCurrentVersion(requirementId, next.getVersionId()) != 1
            || requirementMapper.updateRequirementAttachmentVersion(next.getVersionId(), uploaded.stream()
                .map(RequirementAttachment::getAttachmentId).toList()) != uploaded.size())
        {
            throw new ServiceException("更新需求附件版本失败");
        }
        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setOperationType("REQUIREMENT_ATTACHMENT_UPDATE");
        log.setDetail("更新需求附件并生成版本 v" + next.getVersionNo());
        log.setCreateTime(now);
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return selectRequirementForUserInternal(projectId, requirementId, operatorId);
    }

    @Override
    public List<RequirementAttachment> selectRequirementAttachmentsForUser(Long projectId, Long requirementId,
        Long userId, Long versionId)
    {
        Requirement requirement = requirementMapper.selectRequirementForUser(projectId, requirementId, userId);
        if (requirement == null)
        {
            return null;
        }
        if (versionId == null)
        {
            versionId = requirement.getCurrentVersionId();
        }
        final Long selectedVersionId = versionId;
        List<RequirementVersion> versions = requirementMapper.selectRequirementVersionsForUser(projectId,
            requirementId, userId);
        RequirementVersion version = versions == null ? null : versions.stream()
            .filter(item -> selectedVersionId != null && selectedVersionId.equals(item.getVersionId())).findFirst().orElse(null);
        if (version == null)
        {
            return null;
        }
        Set<Long> ids = new LinkedHashSet<>(snapshotIds(version.getAttachmentSnapshot()));
        return requirementMapper.selectRequirementAttachments(requirementId).stream()
            .filter(item -> ids.contains(item.getAttachmentId())).collect(Collectors.toList());
    }

    @Override
    public RequirementAttachment selectRequirementAttachmentForUser(Long projectId, Long requirementId,
        Long attachmentId, Long userId)
    {
        List<RequirementAttachment> attachments = selectRequirementAttachmentsForUser(projectId, requirementId,
            userId, currentVersionId(projectId, requirementId, userId));
        return attachments == null ? null : attachments.stream()
            .filter(item -> attachmentId != null && attachmentId.equals(item.getAttachmentId())).findFirst().orElse(null);
    }

    private Long currentVersionId(Long projectId, Long requirementId, Long userId)
    {
        Requirement requirement = requirementMapper.selectRequirementForUser(projectId, requirementId, userId);
        return requirement == null ? null : requirement.getCurrentVersionId();
    }

    private List<Long> snapshotIds(String snapshot)
    {
        if (snapshot == null || snapshot.trim().isEmpty() || "[]".equals(snapshot.trim())) return List.of();
        try { return JSON.parseArray(snapshot, Long.class); }
        catch (Exception e) { return List.of(); }
    }

    private Requirement selectRequirementForUserInternal(Long projectId, Long requirementId, Long userId)
    {
        Requirement requirement = requirementMapper.selectRequirementForUser(projectId, requirementId, userId);
        if (requirement != null)
        {
            requirement.setOwners(requirementMapper.selectRequirementOwners(requirementId));
            List<RequirementAttachment> attachments = selectRequirementAttachmentsForUser(projectId, requirementId,
                userId, requirement.getCurrentVersionId());
            requirement.setAttachments(attachments == null ? List.of() : attachments);
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

    private String statusLabel(String status, String label)
    {
        return label == null || label.trim().isEmpty() ? status : label;
    }

    private boolean hasContent(String content)
    {
        return content != null && !content.replaceAll("<[^>]*>", "")
            .replace("&nbsp;", "").trim().isEmpty();
    }
}
