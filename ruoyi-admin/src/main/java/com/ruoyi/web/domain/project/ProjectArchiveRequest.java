package com.ruoyi.web.domain.project;

import jakarta.validation.constraints.NotNull;

public class ProjectArchiveRequest
{
    @NotNull(message = "项目状态不能为空")
    private Boolean archived;

    public Boolean getArchived()
    {
        return archived;
    }

    public void setArchived(Boolean archived)
    {
        this.archived = archived;
    }
}
