package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotNull;

public class ProjectMemberRoleRequest
{
    @NotNull(message = "项目角色不能为空")
    private Long roleId;

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }
}
