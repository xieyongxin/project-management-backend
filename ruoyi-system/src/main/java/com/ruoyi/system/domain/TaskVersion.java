package com.ruoyi.system.domain;

import java.util.Date;

public class TaskVersion
{
    private Long versionId;
    private Long taskId;
    private Integer versionNo;
    private String title;
    private String description;
    private Long requirementVersionId;
    private Integer requirementVersionNo;
    private Long createdBy;
    private Date createTime;
    private Integer isDeleted;

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getRequirementVersionId() { return requirementVersionId; }
    public void setRequirementVersionId(Long requirementVersionId) { this.requirementVersionId = requirementVersionId; }
    public Integer getRequirementVersionNo() { return requirementVersionNo; }
    public void setRequirementVersionNo(Integer requirementVersionNo) { this.requirementVersionNo = requirementVersionNo; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
