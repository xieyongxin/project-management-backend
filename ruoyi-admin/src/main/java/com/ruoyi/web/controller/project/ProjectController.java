package com.ruoyi.web.controller.project;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.domain.ProjectOperationLog;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.service.IProjectService;
import com.ruoyi.web.domain.project.ProjectCreateRequest;
import com.ruoyi.web.domain.project.ProjectView;
import com.ruoyi.web.domain.project.ProjectMemberView;
import com.ruoyi.web.domain.project.ProjectMemberRoleRequest;
import com.ruoyi.web.domain.project.ProjectMemberAdminRequest;
import com.ruoyi.web.domain.project.ProjectNameUpdateRequest;
import com.ruoyi.web.domain.project.ProjectRoleOptionView;
import com.ruoyi.web.domain.project.ProjectOperationLogView;
import com.ruoyi.web.domain.project.ProjectArchiveRequest;

@RestController
@RequestMapping("/project")
public class ProjectController extends BaseController
{
    private final IProjectService projectService;

    public ProjectController(IProjectService projectService)
    {
        this.projectService = projectService;
    }

    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String projectName)
    {
        startPage();
        List<Project> projects = projectService.selectProjectsForUser(getUserId(), projectName);
        TableDataInfo result = getDataTable(projects);
        result.setRows(projects.stream().map(ProjectView::from).toList());
        return result;
    }

    @PreAuthorize("@ss.hasPermi('project:create')")
    @PostMapping
    public AjaxResult create(@Validated @RequestBody ProjectCreateRequest request)
    {
        Project project = projectService.createProject(request.getProjectName(), getUserId());
        return success(ProjectView.from(project));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<AjaxResult> update(@PathVariable String projectId,
        @Validated @RequestBody ProjectNameUpdateRequest request)
    {
        Long parsedProjectId = parseProjectId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        try
        {
            Project project = projectService.updateProjectName(parsedProjectId, getUserId(), request.getProjectName());
            return ResponseEntity.ok(success(ProjectView.from(project)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PutMapping("/{projectId}/status")
    public ResponseEntity<AjaxResult> updateStatus(@PathVariable String projectId,
        @Validated @RequestBody ProjectArchiveRequest request)
    {
        Long parsedProjectId = parseProjectId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        try
        {
            Project project = projectService.updateProjectStatus(parsedProjectId, getUserId(), request.getArchived());
            return ResponseEntity.ok(success(ProjectView.from(project)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<AjaxResult> getInfo(@PathVariable String projectId)
    {
        Long parsedProjectId;
        try
        {
            parsedProjectId = Long.valueOf(projectId);
        }
        catch (NumberFormatException e)
        {
            return notFound();
        }

        Project project = projectService.selectProjectForUser(parsedProjectId, getUserId());
        if (StringUtils.isNull(project))
        {
            return notFound();
        }
        return ResponseEntity.ok(success(ProjectView.from(project)));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<AjaxResult> members(@PathVariable String projectId)
    {
        Long parsedProjectId;
        try
        {
            parsedProjectId = Long.valueOf(projectId);
        }
        catch (NumberFormatException e)
        {
            return notFound();
        }

        List<ProjectMember> members = projectService.selectProjectMembersForUser(parsedProjectId, getUserId());
        if (members == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(members.stream().map(ProjectMemberView::from).toList()));
    }

    @GetMapping("/{projectId}/members/roles")
    public ResponseEntity<AjaxResult> memberRoleOptions(@PathVariable String projectId)
    {
        Long parsedProjectId = parseProjectId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        try
        {
            List<SysRole> roles = projectService.selectProjectRoleOptionsForAdmin(parsedProjectId, getUserId());
            return ResponseEntity.ok(success(roles.stream().map(ProjectRoleOptionView::from).toList()));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PutMapping("/{projectId}/members/{memberUserId}/role")
    public ResponseEntity<AjaxResult> updateMemberRole(@PathVariable String projectId,
        @PathVariable String memberUserId, @Validated @RequestBody ProjectMemberRoleRequest request)
    {
        Long parsedProjectId = parseProjectId(projectId);
        Long parsedMemberUserId = parseProjectId(memberUserId);
        if (parsedProjectId == null || parsedMemberUserId == null)
        {
            return notFound();
        }
        try
        {
            ProjectMember member = projectService.updateProjectMemberRole(parsedProjectId, getUserId(),
                parsedMemberUserId, request.getRoleId());
            return ResponseEntity.ok(success(ProjectMemberView.from(member)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PutMapping("/{projectId}/members/{memberUserId}/admin")
    public ResponseEntity<AjaxResult> updateMemberAdmin(@PathVariable String projectId,
        @PathVariable String memberUserId, @Validated @RequestBody ProjectMemberAdminRequest request)
    {
        Long parsedProjectId = parseProjectId(projectId);
        Long parsedMemberUserId = parseProjectId(memberUserId);
        if (parsedProjectId == null || parsedMemberUserId == null)
        {
            return notFound();
        }
        try
        {
            ProjectMember member = projectService.updateProjectMemberAdmin(parsedProjectId, getUserId(),
                parsedMemberUserId, request.getProjectAdmin());
            return ResponseEntity.ok(success(ProjectMemberView.from(member)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PreAuthorize("@ss.hasPermi('project:log:list')")
    @GetMapping("/{projectId}/logs")
    public ResponseEntity<?> operationLogs(@PathVariable String projectId)
    {
        Long parsedProjectId = parseProjectId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        startPage();
        List<ProjectOperationLog> logs = projectService.selectProjectOperationLogsForUser(parsedProjectId, getUserId());
        if (logs == null)
        {
            return notFound();
        }
        TableDataInfo result = getDataTable(logs);
        result.setRows(logs.stream().map(ProjectOperationLogView::from).toList());
        return ResponseEntity.ok(result);
    }

    private Long parseProjectId(String projectId)
    {
        try
        {
            return Long.valueOf(projectId);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private ResponseEntity<AjaxResult> notFound()
    {
        AjaxResult result = AjaxResult.error(HttpStatus.NOT_FOUND, "项目不存在或无权访问");
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(result);
    }

    private ResponseEntity<AjaxResult> serviceError(ServiceException error)
    {
        int code = error.getCode() == null ? HttpStatus.ERROR : error.getCode();
        AjaxResult result = AjaxResult.error(code, error.getMessage());
        int status = code >= 400 && code < 600 ? code : org.springframework.http.HttpStatus.OK.value();
        return ResponseEntity.status(status).body(result);
    }
}
