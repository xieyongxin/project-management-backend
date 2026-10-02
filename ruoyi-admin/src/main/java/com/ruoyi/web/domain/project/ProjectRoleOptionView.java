package com.ruoyi.web.domain.project;

import com.ruoyi.common.core.domain.entity.SysRole;

public class ProjectRoleOptionView
{
    private Long roleId;
    private String roleName;

    public static ProjectRoleOptionView from(SysRole role)
    {
        ProjectRoleOptionView view = new ProjectRoleOptionView();
        view.setRoleId(role.getRoleId());
        view.setRoleName(role.getRoleName());
        return view;
    }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
}
