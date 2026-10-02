package com.ruoyi.web.domain.project;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TaskCreateRequest
{
    @NotNull(message = "所属需求不能为空")
    private Long requirementId;

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 255, message = "任务标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "任务说明不能为空")
    private String description;

    @NotBlank(message = "任务状态不能为空")
    private String status;

    @NotEmpty(message = "任务至少需要一个分类")
    private List<String> categoryValues;

    @NotEmpty(message = "任务至少需要一名负责人")
    private List<Long> ownerIds;

    public Long getRequirementId() { return requirementId; }
    public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getCategoryValues() { return categoryValues; }
    public void setCategoryValues(List<String> categoryValues) { this.categoryValues = categoryValues; }
    public List<Long> getOwnerIds() { return ownerIds; }
    public void setOwnerIds(List<Long> ownerIds) { this.ownerIds = ownerIds; }
}
