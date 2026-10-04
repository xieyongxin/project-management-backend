package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.RequirementAttachment;

public class RequirementAttachmentView
{
    private Long attachmentId;
    private Long requirementId;
    private Long versionId;
    private String originalName;
    private String extension;
    private String contentType;
    private Long fileSize;
    private boolean previewable;

    public static RequirementAttachmentView from(RequirementAttachment attachment)
    {
        RequirementAttachmentView view = new RequirementAttachmentView();
        view.setAttachmentId(attachment.getAttachmentId());
        view.setRequirementId(attachment.getRequirementId());
        view.setVersionId(attachment.getVersionId());
        view.setOriginalName(attachment.getOriginalName());
        view.setExtension(attachment.getExtension());
        view.setContentType(attachment.getContentType());
        view.setFileSize(attachment.getFileSize());
        String extension = attachment.getExtension() == null ? "" : attachment.getExtension().toLowerCase();
        view.setPreviewable("pdf".equals(extension) || "png".equals(extension) || "jpg".equals(extension)
            || "jpeg".equals(extension));
        return view;
    }

    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public boolean isPreviewable() { return previewable; }
    public void setPreviewable(boolean previewable) { this.previewable = previewable; }
}
