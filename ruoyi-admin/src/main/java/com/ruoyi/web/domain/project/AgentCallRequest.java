package com.ruoyi.web.domain.project;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public class AgentCallRequest
{
    private List<Long> attachmentIds;
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
    private boolean confirmed;

    public List<Long> getAttachmentIds() { return attachmentIds; }
    public void setAttachmentIds(List<Long> attachmentIds) { this.attachmentIds = attachmentIds; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
}
