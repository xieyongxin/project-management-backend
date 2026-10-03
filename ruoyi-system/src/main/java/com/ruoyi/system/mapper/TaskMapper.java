package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskCategory;
import com.ruoyi.system.domain.TaskOwner;
import com.ruoyi.system.domain.TaskVersion;

public interface TaskMapper
{
    int insertTask(Task task);

    int insertTaskVersion(TaskVersion version);

    int updateCurrentVersion(@Param("taskId") Long taskId, @Param("versionId") Long versionId);

    int insertTaskCategory(@Param("taskId") Long taskId, @Param("categoryValue") String categoryValue);

    int insertTaskOwner(@Param("taskId") Long taskId, @Param("userId") Long userId);

    Task selectTaskForUser(@Param("projectId") Long projectId, @Param("taskId") Long taskId,
        @Param("userId") Long userId);

    List<TaskVersion> selectTaskVersionsForUser(@Param("projectId") Long projectId,
        @Param("taskId") Long taskId, @Param("userId") Long userId);

    List<Task> selectTasksForUser(@Param("projectId") Long projectId, @Param("userId") Long userId);

    List<TaskCategory> selectTaskCategories(@Param("taskId") Long taskId);

    List<TaskOwner> selectTaskOwners(@Param("taskId") Long taskId);

    List<SysDictData> selectActiveTaskStatuses();

    List<SysDictData> selectActiveTaskCategories();
}
