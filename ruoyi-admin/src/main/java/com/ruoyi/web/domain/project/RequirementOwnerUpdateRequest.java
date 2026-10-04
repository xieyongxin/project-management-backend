package com.ruoyi.web.domain.project;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public class RequirementOwnerUpdateRequest
{
    @NotEmpty(message = "需求至少需要一名负责人")
    private List<Long> ownerIds;

    public List<Long> getOwnerIds()
    {
        return ownerIds;
    }

    public void setOwnerIds(List<Long> ownerIds)
    {
        this.ownerIds = ownerIds;
    }
}
