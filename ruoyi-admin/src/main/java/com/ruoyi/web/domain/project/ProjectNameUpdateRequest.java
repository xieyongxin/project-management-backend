package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProjectNameUpdateRequest
{
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 255, message = "项目名称长度不能超过255个字符")
    private String projectName;

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }
}
