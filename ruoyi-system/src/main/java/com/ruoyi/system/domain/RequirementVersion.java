package com.ruoyi.system.domain;

import java.util.Date;

public class RequirementVersion
{
    private Long versionId;
    private Long requirementId;
    private Integer versionNo;
    private String title;
    private String content;
    private String attachmentSnapshot;
    private Long createdBy;
    private Date createTime;

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
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
