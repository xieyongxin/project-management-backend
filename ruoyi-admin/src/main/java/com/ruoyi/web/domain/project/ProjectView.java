package com.ruoyi.web.domain.project;

import java.util.Date;
import com.ruoyi.system.domain.Project;

public class ProjectView
{
    private Long projectId;
    private String projectName;
    private Long creatorId;
    private String status;
    private Date createTime;
    private Date updateTime;

    public static ProjectView from(Project project)
    {
        ProjectView view = new ProjectView();
        view.setProjectId(project.getProjectId());
        view.setProjectName(project.getProjectName());
        view.setCreatorId(project.getCreatorId());
        view.setStatus(project.getStatus());
        view.setCreateTime(project.getCreateTime());
        view.setUpdateTime(project.getUpdateTime());
        return view;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
