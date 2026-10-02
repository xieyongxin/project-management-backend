package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.ruoyi.common.core.domain.entity.SysRole;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.service.IProjectService;

@Service
public class ProjectServiceImpl implements IProjectService
{
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectOperationLogMapper projectOperationLogMapper;

    public ProjectServiceImpl(ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper,
        ProjectOperationLogMapper projectOperationLogMapper)
    {
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.projectOperationLogMapper = projectOperationLogMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Project createProject(String projectName, Long creatorId)
    {
        if (creatorId == null)
        {
            throw new ServiceException("未获取到当前用户", HttpStatus.UNAUTHORIZED);
        }

        String normalizedName = projectName == null ? "" : projectName.trim();
        if (normalizedName.isEmpty())
        {
            throw new ServiceException("项目名称不能为空", HttpStatus.BAD_REQUEST);
        }
        if (normalizedName.length() > 255)
        {
            throw new ServiceException("项目名称长度不能超过255个字符", HttpStatus.BAD_REQUEST);
        }

        Date now = new Date();
        Project project = new Project();
        project.setProjectName(normalizedName);
        project.setProjectNameKey(normalizedName.toLowerCase(Locale.ROOT));
        project.setCreatorId(creatorId);
        project.setCreateTime(now);
        project.setUpdateTime(now);

        try
        {
            if (projectMapper.insertProject(project) != 1)
            {
                throw new ServiceException("创建项目失败");
            }
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("项目名称已存在", HttpStatus.CONFLICT);
        }

        ProjectMember creator = new ProjectMember();
        creator.setProjectId(project.getProjectId());
        creator.setUserId(creatorId);
        creator.setIsProjectAdmin(1);
        creator.setCreateTime(now);
        creator.setUpdateTime(now);
        if (projectMemberMapper.insertProjectMember(creator) != 1)
        {
            throw new ServiceException("创建项目成员关系失败");
        }
        return project;
    }

    @Override
    public List<Project> selectProjectsForUser(Long userId, String projectName)
    {
        String keyword = projectName == null ? null : projectName.trim();
        return projectMapper.selectProjectListForUser(userId, keyword == null || keyword.isEmpty() ? null : keyword);
    }

    @Override
    public Project selectProjectForUser(Long projectId, Long userId)
    {
        return projectMapper.selectProjectForUser(projectId, userId);
    }

    @Override
    public List<ProjectMember> selectProjectMembersForUser(Long projectId, Long userId)
    {
        if (projectId == null || userId == null || projectMapper.selectProjectForUser(projectId, userId) == null)
        {
            return null;
        }
        return projectMemberMapper.selectProjectMembersForUser(projectId, userId);
    }

    @Override
    public List<SysRole> selectProjectRoleOptionsForAdmin(Long projectId, Long userId)
    {
        requireProjectAdmin(projectId, userId);
        return projectMemberMapper.selectActiveProjectRoles();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectMember updateProjectMemberRole(Long projectId, Long operatorId, Long memberUserId, Long roleId)
    {
        requireProjectAdmin(projectId, operatorId);
        if (memberUserId == null)
        {
            throw new ServiceException("项目成员不存在", HttpStatus.NOT_FOUND);
        }
        if (roleId == null)
        {
            throw new ServiceException("项目角色不能为空", HttpStatus.BAD_REQUEST);
        }
        ProjectMember member = projectMemberMapper.selectProjectMember(projectId, memberUserId);
        if (member == null)
        {
            throw new ServiceException("项目成员不存在", HttpStatus.NOT_FOUND);
        }
        if (projectMemberMapper.selectActiveProjectRole(roleId) == null)
        {
            throw new ServiceException("项目角色不存在或已停用", HttpStatus.BAD_REQUEST);
        }
        if (projectMemberMapper.updateProjectMemberRole(projectId, memberUserId, roleId) != 1)
        {
            throw new ServiceException("更新项目成员角色失败");
        }
        ProjectOperationLog log = new ProjectOperationLog();
        log.setProjectId(projectId);
        log.setOperatorId(operatorId);
        log.setTargetUserId(memberUserId);
        log.setPreviousRoleId(member.getRoleId());
        log.setNewRoleId(roleId);
        log.setOperationType("MEMBER_ROLE_UPDATE");
        log.setDetail("更新项目成员全局角色");
        log.setCreateTime(new Date());
        if (projectOperationLogMapper.insertProjectOperationLog(log) != 1)
        {
            throw new ServiceException("记录项目操作日志失败");
        }
        return projectMemberMapper.selectProjectMember(projectId, memberUserId);
    }

    private ProjectMember requireProjectAdmin(Long projectId, Long userId)
    {
        if (projectId == null || userId == null)
        {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        ProjectMember operator = projectMemberMapper.selectProjectMember(projectId, userId);
        if (operator == null)
        {
            throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(operator.getIsProjectAdmin()))
        {
            throw new ServiceException("只有项目管理员可以调整成员角色", HttpStatus.FORBIDDEN);
        }
        return operator;
    }
}
