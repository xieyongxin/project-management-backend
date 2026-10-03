package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotBlank;

public class TaskStatusUpdateRequest
{
    @NotBlank(message = "任务状态不能为空")
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
