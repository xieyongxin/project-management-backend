package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotNull;

public class ProjectMemberAdminRequest
{
    @NotNull(message = "管理员资格不能为空")
    private Boolean projectAdmin;

    public Boolean getProjectAdmin()
    {
        return projectAdmin;
    }

    public void setProjectAdmin(Boolean projectAdmin)
    {
        this.projectAdmin = projectAdmin;
    }
}
