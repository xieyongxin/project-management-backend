package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequirementContentUpdateRequest
{
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 255, message = "需求标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "需求正文不能为空")
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
