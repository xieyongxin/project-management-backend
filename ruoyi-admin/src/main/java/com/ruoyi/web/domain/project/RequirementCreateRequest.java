package com.ruoyi.web.domain.project;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class RequirementCreateRequest
{
    @NotBlank(message = "需求标题不能为空")
    @Size(max = 255, message = "需求标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "需求正文不能为空")
    private String content;

    @NotBlank(message = "需求状态不能为空")
    private String status;

    @NotEmpty(message = "需求至少需要一名负责人")
    private List<Long> ownerIds;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Long> getOwnerIds() { return ownerIds; }
    public void setOwnerIds(List<Long> ownerIds) { this.ownerIds = ownerIds; }
}
