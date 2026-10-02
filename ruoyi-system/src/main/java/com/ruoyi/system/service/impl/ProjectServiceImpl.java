package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectMemberMapper;
import com.ruoyi.system.service.IProjectService;

@Service
public class ProjectServiceImpl implements IProjectService
{
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    public ProjectServiceImpl(ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper)
    {
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
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
}
