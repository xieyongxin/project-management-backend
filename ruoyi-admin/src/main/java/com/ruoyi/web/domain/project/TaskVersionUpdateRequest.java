package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TaskVersionUpdateRequest
{
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 255, message = "任务标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "任务说明不能为空")
    private String description;

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
