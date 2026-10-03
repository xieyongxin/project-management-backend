package com.ruoyi.web.domain.project;

import java.util.List;
import com.ruoyi.system.domain.Task;

public class TaskView
{
    private Long taskId;
    private Long projectId;
    private Long requirementId;
    private Long creatorId;
    private Integer currentVersionNo;
    private Long requirementVersionId;
    private Integer requirementVersionNo;
    private Integer requirementVersionOutdated;
    private String title;
    private String description;
    private String status;
    private String statusLabel;
    private java.util.Date createTime;
    private java.util.Date updateTime;
    private List<TaskCategoryView> categories;
    private List<TaskOwnerView> owners;

    public static TaskView from(Task task)
    {
        TaskView view = new TaskView();
        view.setTaskId(task.getTaskId());
        view.setProjectId(task.getProjectId());
        view.setRequirementId(task.getRequirementId());
        view.setCreatorId(task.getCreatorId());
        view.setCurrentVersionNo(task.getCurrentVersionNo());
        view.setRequirementVersionId(task.getRequirementVersionId());
        view.setRequirementVersionNo(task.getRequirementVersionNo());
        view.setRequirementVersionOutdated(task.getRequirementVersionOutdated());
        view.setTitle(task.getTitle());
        view.setDescription(task.getDescription());
        view.setStatus(task.getStatus());
        view.setStatusLabel(task.getStatusLabel());
        view.setCreateTime(task.getCreateTime());
        view.setUpdateTime(task.getUpdateTime());
        view.setCategories(task.getCategories() == null ? List.of()
            : task.getCategories().stream().map(TaskCategoryView::from).toList());
        view.setOwners(task.getOwners() == null ? List.of()
            : task.getOwners().stream().map(TaskOwnerView::from).toList());
        return view;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Integer getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(Integer currentVersionNo) { this.currentVersionNo = currentVersionNo; }
    public Long getRequirementVersionId() { return requirementVersionId; }
    public void setRequirementVersionId(Long requirementVersionId) { this.requirementVersionId = requirementVersionId; }
    public Integer getRequirementVersionNo() { return requirementVersionNo; }
    public void setRequirementVersionNo(Integer requirementVersionNo) { this.requirementVersionNo = requirementVersionNo; }
    public Integer getRequirementVersionOutdated() { return requirementVersionOutdated; }
    public void setRequirementVersionOutdated(Integer requirementVersionOutdated) { this.requirementVersionOutdated = requirementVersionOutdated; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
    public java.util.Date getCreateTime() { return createTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public java.util.Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.util.Date updateTime) { this.updateTime = updateTime; }
    public List<TaskCategoryView> getCategories() { return categories; }
    public void setCategories(List<TaskCategoryView> categories) { this.categories = categories; }
    public List<TaskOwnerView> getOwners() { return owners; }
    public void setOwners(List<TaskOwnerView> owners) { this.owners = owners; }
}
