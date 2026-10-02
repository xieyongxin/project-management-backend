package com.ruoyi.system.domain;

import java.util.Date;

public class Project
{
    private Long projectId;
    private String projectName;
    private String projectNameKey;
    private Long creatorId;
    private Date createTime;
    private Date updateTime;

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

    public String getProjectNameKey()
    {
        return projectNameKey;
    }

    public void setProjectNameKey(String projectNameKey)
    {
        this.projectNameKey = projectNameKey;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
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
