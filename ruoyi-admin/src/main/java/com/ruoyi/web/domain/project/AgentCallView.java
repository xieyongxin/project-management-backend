package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.AgentCall;

public class AgentCallView
{
    private Long callId;
    private Long requirementId;
    private Long requirementVersionId;
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
    private java.util.Date createTime;

    public static AgentCallView from(AgentCall call)
    {
        AgentCallView view = new AgentCallView();
        view.callId = call.getCallId(); view.requirementId = call.getRequirementId(); view.requirementVersionId = call.getRequirementVersionId();
        view.provider = call.getProvider(); view.model = call.getModel(); view.externalEnabled = call.getExternalEnabled();
        view.status = call.getStatus(); view.idempotencyKey = call.getIdempotencyKey(); view.selectedAttachmentSnapshot = call.getSelectedAttachmentSnapshot();
        view.inputContent = call.getInputContent(); view.parsedAttachmentContent = call.getParsedAttachmentContent(); view.draftTasks = call.getDraftTasks();
        view.errorMessage = call.getErrorMessage(); view.createTime = call.getCreateTime();
        return view;
    }
    public Long getCallId() { return callId; } public Long getRequirementId() { return requirementId; }
    public Long getRequirementVersionId() { return requirementVersionId; } public String getProvider() { return provider; }
    public String getModel() { return model; } public Integer getExternalEnabled() { return externalEnabled; }
    public String getStatus() { return status; } public String getIdempotencyKey() { return idempotencyKey; }
    public String getSelectedAttachmentSnapshot() { return selectedAttachmentSnapshot; } public String getInputContent() { return inputContent; }
    public String getParsedAttachmentContent() { return parsedAttachmentContent; } public String getDraftTasks() { return draftTasks; }
    public String getErrorMessage() { return errorMessage; } public java.util.Date getCreateTime() { return createTime; }
}
