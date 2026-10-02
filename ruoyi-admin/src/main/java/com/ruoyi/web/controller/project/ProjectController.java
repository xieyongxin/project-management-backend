package com.ruoyi.web.controller.project;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.ProjectMember;
import com.ruoyi.system.service.IProjectService;
import com.ruoyi.web.domain.project.ProjectCreateRequest;
import com.ruoyi.web.domain.project.ProjectView;
import com.ruoyi.web.domain.project.ProjectMemberView;

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

    private ResponseEntity<AjaxResult> notFound()
    {
        AjaxResult result = AjaxResult.error(HttpStatus.NOT_FOUND, "项目不存在或无权访问");
        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(result);
    }
}
