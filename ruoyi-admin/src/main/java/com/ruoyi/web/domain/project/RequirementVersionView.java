package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.RequirementVersion;

public class RequirementVersionView
{
    private Long versionId;
    private Long requirementId;
    private Integer versionNo;
    private String title;
    private String content;
    private String attachmentSnapshot;
    private Long createdBy;
    private java.util.Date createTime;
    private Integer isDeleted;

    public static RequirementVersionView from(RequirementVersion version)
    {
        RequirementVersionView view = new RequirementVersionView();
        view.setVersionId(version.getVersionId());
        view.setRequirementId(version.getRequirementId());
        view.setVersionNo(version.getVersionNo());
        view.setTitle(version.getTitle());
        view.setContent(version.getContent());
        view.setAttachmentSnapshot(version.getAttachmentSnapshot());
        view.setCreatedBy(version.getCreatedBy());
        view.setCreateTime(version.getCreateTime());
        view.setIsDeleted(version.getIsDeleted());
        return view;
    }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAttachmentSnapshot() { return attachmentSnapshot; }
    public void setAttachmentSnapshot(String attachmentSnapshot) { this.attachmentSnapshot = attachmentSnapshot; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public java.util.Date getCreateTime() { return createTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
