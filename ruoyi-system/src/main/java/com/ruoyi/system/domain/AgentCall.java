package com.ruoyi.system.domain;

import java.util.Date;

public class AgentCall
{
    private Long callId;
    private Long projectId;
    private Long requirementId;
    private Long requirementVersionId;
    private Long initiatorId;
    private Long retryOfCallId;
    private Integer retryCount;
    private String inputTitle;
    private String provider;
    private String model;
    private Integer externalEnabled;
    private String status;
    private String idempotencyKey;
    private String selectedAttachmentSnapshot;
    private String inputContent;
    private String parsedAttachmentContent;
    private String draftTasks;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;

    public Long getCallId() { return callId; }
    public void setCallId(Long callId) { this.callId = callId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public Long getRequirementVersionId() { return requirementVersionId; }
    public void setRequirementVersionId(Long requirementVersionId) { this.requirementVersionId = requirementVersionId; }
    public Long getInitiatorId() { return initiatorId; }
    public void setInitiatorId(Long initiatorId) { this.initiatorId = initiatorId; }
    public Long getRetryOfCallId() { return retryOfCallId; }
    public void setRetryOfCallId(Long retryOfCallId) { this.retryOfCallId = retryOfCallId; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getInputTitle() { return inputTitle; }
    public void setInputTitle(String inputTitle) { this.inputTitle = inputTitle; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getExternalEnabled() { return externalEnabled; }
    public void setExternalEnabled(Integer externalEnabled) { this.externalEnabled = externalEnabled; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getSelectedAttachmentSnapshot() { return selectedAttachmentSnapshot; }
    public void setSelectedAttachmentSnapshot(String selectedAttachmentSnapshot) { this.selectedAttachmentSnapshot = selectedAttachmentSnapshot; }
    public String getInputContent() { return inputContent; }
    public void setInputContent(String inputContent) { this.inputContent = inputContent; }
    public String getParsedAttachmentContent() { return parsedAttachmentContent; }
    public void setParsedAttachmentContent(String parsedAttachmentContent) { this.parsedAttachmentContent = parsedAttachmentContent; }
    public String getDraftTasks() { return draftTasks; }
    public void setDraftTasks(String draftTasks) { this.draftTasks = draftTasks; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
