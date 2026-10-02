package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.RequirementOwner;

public class RequirementOwnerView
{
    private Long userId;
    private String userName;
    private String nickName;
    private String email;

    public static RequirementOwnerView from(RequirementOwner owner)
    {
        RequirementOwnerView view = new RequirementOwnerView();
        view.setUserId(owner.getUserId());
        view.setUserName(owner.getUserName());
        view.setNickName(owner.getNickName());
        view.setEmail(owner.getEmail());
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
}
