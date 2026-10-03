package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotBlank;

public class RequirementStatusUpdateRequest
{
    @NotBlank(message = "需求状态不能为空")
    private String status;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
