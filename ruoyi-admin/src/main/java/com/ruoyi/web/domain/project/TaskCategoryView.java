package com.ruoyi.web.domain.project;

import com.ruoyi.system.domain.TaskCategory;

public class TaskCategoryView
{
    private String categoryValue;
    private String categoryLabel;

    public static TaskCategoryView from(TaskCategory category)
    {
        TaskCategoryView view = new TaskCategoryView();
        view.setCategoryValue(category.getCategoryValue());
        view.setCategoryLabel(category.getCategoryLabel());
        return view;
    }

    public String getCategoryValue() { return categoryValue; }
    public void setCategoryValue(String categoryValue) { this.categoryValue = categoryValue; }
    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
}
