package com.ruoyi.system.domain;

import java.util.Date;

public class ProjectMember
{
    private Long projectId;
    private Long userId;
    private Integer isProjectAdmin;
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

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Integer getIsProjectAdmin()
    {
        return isProjectAdmin;
    }

    public void setIsProjectAdmin(Integer isProjectAdmin)
    {
        this.isProjectAdmin = isProjectAdmin;
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
