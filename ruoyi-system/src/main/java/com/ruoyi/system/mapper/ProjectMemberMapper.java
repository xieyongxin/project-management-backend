package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.ProjectMember;

public interface ProjectMemberMapper
{
    int insertProjectMember(ProjectMember projectMember);

    List<ProjectMember> selectProjectMembersForUser(@Param("projectId") Long projectId,
        @Param("userId") Long userId);
}
