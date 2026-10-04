package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotBlank;

public class AgentRetryRequest
{
    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
    private boolean confirmed;

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
}
