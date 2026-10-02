package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.ProjectMember;

public interface ProjectMemberMapper
{
    int insertProjectMember(ProjectMember projectMember);

    List<ProjectMember> selectProjectMembersForUser(@Param("projectId") Long projectId,
        @Param("userId") Long userId);

    ProjectMember selectProjectMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    ProjectMember selectActiveProjectMemberUser(@Param("userId") Long userId);

    int updateProjectMemberRole(@Param("projectId") Long projectId, @Param("userId") Long userId,
        @Param("roleId") Long roleId);

    int updateProjectMemberAdmin(@Param("projectId") Long projectId, @Param("userId") Long userId,
        @Param("isProjectAdmin") Integer isProjectAdmin);

    int deleteProjectMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    int countProjectAdmins(@Param("projectId") Long projectId);

    SysRole selectActiveProjectRole(@Param("roleId") Long roleId);

    List<SysRole> selectActiveProjectRoles();
}
