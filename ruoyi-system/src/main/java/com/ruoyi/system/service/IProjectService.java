package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectOperationLog;

public interface IProjectService
{
    Project createProject(String projectName, Long creatorId);

    Project updateProjectName(Long projectId, Long operatorId, String projectName);

    Project updateProjectStatus(Long projectId, Long operatorId, Boolean archived);

    List<Project> selectProjectsForUser(Long userId, String projectName);

    Project selectProjectForUser(Long projectId, Long userId);

    List<ProjectMember> selectProjectMembersForUser(Long projectId, Long userId);

    List<SysRole> selectProjectRoleOptionsForAdmin(Long projectId, Long userId);

    ProjectMember addProjectMember(Long projectId, Long operatorId, Long memberUserId, Long roleId);

    ProjectMember updateProjectMemberRole(Long projectId, Long operatorId, Long memberUserId, Long roleId);

    ProjectMember updateProjectMemberAdmin(Long projectId, Long operatorId, Long memberUserId,
        Boolean projectAdmin);

    void removeProjectMember(Long projectId, Long operatorId, Long memberUserId);

    List<ProjectOperationLog> selectProjectOperationLogsForUser(Long projectId, Long userId);
}
