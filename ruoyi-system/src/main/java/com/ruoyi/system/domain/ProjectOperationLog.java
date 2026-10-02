package com.ruoyi.system.domain;

import java.util.Date;

public class ProjectOperationLog
{
    private Long logId;
    private Long projectId;
    private Long operatorId;
    private Long targetUserId;
    private Long previousRoleId;
    private Long newRoleId;
    private String operationType;
    private String detail;
    private Date createTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public Long getPreviousRoleId() { return previousRoleId; }
    public void setPreviousRoleId(Long previousRoleId) { this.previousRoleId = previousRoleId; }
    public Long getNewRoleId() { return newRoleId; }
    public void setNewRoleId(Long newRoleId) { this.newRoleId = newRoleId; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
