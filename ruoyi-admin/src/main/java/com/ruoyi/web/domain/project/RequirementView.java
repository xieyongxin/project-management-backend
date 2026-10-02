package com.ruoyi.web.domain.project;

import java.util.List;
import com.ruoyi.system.domain.Requirement;

public class RequirementView
{
    private Long requirementId;
    private Long projectId;
    private Long creatorId;
    private Long currentVersionId;
    private Integer currentVersionNo;
    private String title;
    private String content;
    private String status;
    private String statusLabel;
    private java.util.Date createTime;
    private java.util.Date updateTime;
    private List<RequirementOwnerView> owners;

    public static RequirementView from(Requirement requirement)
    {
        RequirementView view = new RequirementView();
        view.setRequirementId(requirement.getRequirementId());
        view.setProjectId(requirement.getProjectId());
        view.setCreatorId(requirement.getCreatorId());
        view.setCurrentVersionId(requirement.getCurrentVersionId());
        view.setCurrentVersionNo(requirement.getCurrentVersionNo());
        view.setTitle(requirement.getTitle());
        view.setContent(requirement.getContent());
        view.setStatus(requirement.getStatus());
        view.setStatusLabel(requirement.getStatusLabel());
        view.setCreateTime(requirement.getCreateTime());
        view.setUpdateTime(requirement.getUpdateTime());
        view.setOwners(requirement.getOwners() == null ? List.of()
            : requirement.getOwners().stream().map(RequirementOwnerView::from).toList());
        return view;
    }

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getCurrentVersionId() { return currentVersionId; }
    public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
    public Integer getCurrentVersionNo() { return currentVersionNo; }
    public void setCurrentVersionNo(Integer currentVersionNo) { this.currentVersionNo = currentVersionNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
    public java.util.Date getCreateTime() { return createTime; }
    public void setCreateTime(java.util.Date createTime) { this.createTime = createTime; }
    public java.util.Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.util.Date updateTime) { this.updateTime = updateTime; }
    public List<RequirementOwnerView> getOwners() { return owners; }
    public void setOwners(List<RequirementOwnerView> owners) { this.owners = owners; }
}
