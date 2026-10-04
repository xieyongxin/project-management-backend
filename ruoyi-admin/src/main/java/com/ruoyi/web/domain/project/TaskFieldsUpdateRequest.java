package com.ruoyi.web.domain.project;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public class TaskFieldsUpdateRequest
{
    @NotEmpty(message = "任务至少需要一个分类")
    private List<String> categoryValues;

    @NotEmpty(message = "任务至少需要一名负责人")
    private List<Long> ownerIds;

    public List<String> getCategoryValues()
    {
        return categoryValues;
    }

    public void setCategoryValues(List<String> categoryValues)
    {
        this.categoryValues = categoryValues;
    }

    public List<Long> getOwnerIds()
    {
        return ownerIds;
    }

    public void setOwnerIds(List<Long> ownerIds)
    {
        this.ownerIds = ownerIds;
    }
}
