package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotNull;

public class ProjectMemberAddRequest
{
    @NotNull(message = "用户不能为空")
    private Long userId;

    @NotNull(message = "项目角色不能为空")
    private Long roleId;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }
}
