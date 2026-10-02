package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.ProjectMember;

public class ProjectMemberView
{
    private Long userId;
    private String userName;
    private String nickName;
    private String email;
    private Long roleId;
    private String roleName;
    private Integer isProjectAdmin;

    public static ProjectMemberView from(ProjectMember member)
    {
        ProjectMemberView view = new ProjectMemberView();
        view.setUserId(member.getUserId());
        view.setUserName(member.getUserName());
        view.setNickName(member.getNickName());
        view.setEmail(member.getEmail());
        view.setRoleId(member.getRoleId());
        view.setRoleName(member.getRoleName());
        view.setIsProjectAdmin(member.getIsProjectAdmin());
        return view;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public Integer getIsProjectAdmin() { return isProjectAdmin; }
    public void setIsProjectAdmin(Integer isProjectAdmin) { this.isProjectAdmin = isProjectAdmin; }
}
