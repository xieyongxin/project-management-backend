package com.ruoyi.system.domain;

import java.util.Date;
import java.util.List;

public class Task
{
    private Long taskId;
    private Long projectId;
    private Long requirementId;
    private Long creatorId;
    private Long currentVersionId;
    private Integer currentVersionNo;
    private Long requirementVersionId;
    private Integer requirementVersionNo;
    private String title;
    private String description;
    private String status;
    private String statusLabel;
    private Integer isDeleted;
    private Date createTime;
    private Date updateTime;
    private List<TaskCategory> categories;
    private List<TaskOwner> owners;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
    public Integer getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(Integer currentVersionNo) { this.currentVersionNo = currentVersionNo; }
    public Long getRequirementVersionId() { return requirementVersionId; }
    public void setRequirementVersionId(Long requirementVersionId) { this.requirementVersionId = requirementVersionId; }
    public Integer getRequirementVersionNo() { return requirementVersionNo; }
    public void setRequirementVersionNo(Integer requirementVersionNo) { this.requirementVersionNo = requirementVersionNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public List<TaskCategory> getCategories() { return categories; }
    public void setCategories(List<TaskCategory> categories) { this.categories = categories; }
    public List<TaskOwner> getOwners() { return owners; }
    public void setOwners(List<TaskOwner> owners) { this.owners = owners; }
}
