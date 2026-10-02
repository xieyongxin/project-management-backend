package com.ruoyi.web.domain.project;

import java.util.Date;
import com.ruoyi.system.domain.ProjectOperationLog;

public class ProjectOperationLogView
{
    private Long logId;
    private Long projectId;
    private Long operatorId;
    private String operatorName;
    private Long targetUserId;
    private String targetUserName;
    private String operationType;
    private String detail;
    private Date createTime;

    public static ProjectOperationLogView from(ProjectOperationLog log)
    {
        ProjectOperationLogView view = new ProjectOperationLogView();
        view.setLogId(log.getLogId());
        view.setProjectId(log.getProjectId());
        view.setOperatorId(log.getOperatorId());
        view.setOperatorName(log.getOperatorName());
        view.setTargetUserId(log.getTargetUserId());
        view.setTargetUserName(log.getTargetUserName());
        view.setOperationType(log.getOperationType());
        view.setDetail(log.getDetail());
        view.setCreateTime(log.getCreateTime());
        return view;
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public String getTargetUserName() { return targetUserName; }
    public void setTargetUserName(String targetUserName) { this.targetUserName = targetUserName; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
