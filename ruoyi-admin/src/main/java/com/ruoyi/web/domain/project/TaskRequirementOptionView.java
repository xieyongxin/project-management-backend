package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.Requirement;

public class TaskRequirementOptionView
{
    private Long requirementId;
    private String title;
    private Integer currentVersionNo;
    private String status;
    private String statusLabel;

    public static TaskRequirementOptionView from(Requirement requirement)
    {
        TaskRequirementOptionView view = new TaskRequirementOptionView();
        view.setRequirementId(requirement.getRequirementId());
        view.setTitle(requirement.getTitle());
        view.setCurrentVersionNo(requirement.getCurrentVersionNo());
        view.setStatus(requirement.getStatus());
        view.setStatusLabel(requirement.getStatusLabel());
        return view;
    }

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(Integer currentVersionNo) { this.currentVersionNo = currentVersionNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
}
