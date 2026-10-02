package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.ProjectOperationLog;

public interface ProjectOperationLogMapper
{
    int insertProjectOperationLog(ProjectOperationLog log);

    List<ProjectOperationLog> selectProjectOperationLogsForUser(@Param("projectId") Long projectId,
        @Param("userId") Long userId);
}
