package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Project;

public interface IProjectService
{
    Project createProject(String projectName, Long creatorId);

    List<Project> selectProjectsForUser(Long userId, String projectName);

    Project selectProjectForUser(Long projectId, Long userId);
}
