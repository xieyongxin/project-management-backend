package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Project;

public interface ProjectMapper
{
    int insertProject(Project project);

    List<Project> selectProjectListForUser(@Param("userId") Long userId, @Param("projectName") String projectName);

    Project selectProjectForUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    Long lockProjectForUpdate(@Param("projectId") Long projectId);

    int updateProjectName(@Param("projectId") Long projectId, @Param("projectName") String projectName,
        @Param("projectNameKey") String projectNameKey);

    int updateProjectStatus(@Param("projectId") Long projectId, @Param("status") String status);
}
