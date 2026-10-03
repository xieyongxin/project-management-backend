package com.ruoyi.web.controller.project;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Task;
import com.ruoyi.system.domain.TaskVersion;
import com.ruoyi.system.service.ITaskService;
import com.ruoyi.web.domain.project.TaskCreateRequest;
import com.ruoyi.web.domain.project.TaskView;
import com.ruoyi.web.domain.project.TaskVersionView;

@RestController
@RequestMapping("/project/{projectId}/tasks")
public class TaskController extends BaseController
{
    private final ITaskService taskService;

    public TaskController(ITaskService taskService)
    {
        this.taskService = taskService;
    }

    @PreAuthorize("@ss.hasPermi('project:task:list')")
    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId)
    {
        Long parsedProjectId = parseId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        startPage();
        List<Task> tasks = taskService.selectTasksForUser(parsedProjectId, getUserId());
        if (tasks == null)
        {
            return notFound();
        }
        TableDataInfo result = getDataTable(tasks);
        result.setRows(tasks.stream().map(TaskView::from).toList());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@ss.hasPermi('project:task:list')")
    @GetMapping("/{taskId}")
    public ResponseEntity<?> detail(@PathVariable String projectId, @PathVariable String taskId)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedTaskId = parseId(taskId);
        if (parsedProjectId == null || parsedTaskId == null)
        {
            return notFound();
        }
        Task task = taskService.selectTaskForUser(parsedProjectId, parsedTaskId, getUserId());
        if (task == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(TaskView.from(task)));
    }

    @PreAuthorize("@ss.hasPermi('project:task:list')")
    @GetMapping("/{taskId}/versions")
    public ResponseEntity<?> versions(@PathVariable String projectId, @PathVariable String taskId)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedTaskId = parseId(taskId);
        if (parsedProjectId == null || parsedTaskId == null)
        {
            return notFound();
        }
        List<TaskVersion> versions = taskService.selectTaskVersionsForUser(parsedProjectId, parsedTaskId,
            getUserId());
        if (versions == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(versions.stream().map(TaskVersionView::from).toList()));
    }

    @PreAuthorize("@ss.hasPermi('project:task:add')")
    @GetMapping("/options")
    public ResponseEntity<?> options(@PathVariable String projectId)
    {
        Long parsedProjectId = parseId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        List<SysDictData> statuses = taskService.selectActiveStatuses(parsedProjectId, getUserId());
        List<SysDictData> categories = taskService.selectActiveCategories(parsedProjectId, getUserId());
        if (statuses == null || categories == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(Map.of("statuses", statuses, "categories", categories)));
    }

    @PreAuthorize("@ss.hasPermi('project:task:add')")
    @PostMapping
    public ResponseEntity<AjaxResult> create(@PathVariable String projectId,
        @Validated @RequestBody TaskCreateRequest request)
    {
        Long parsedProjectId = parseId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        try
        {
            Task task = taskService.createTask(parsedProjectId, getUserId(), request.getRequirementId(),
                request.getTitle(), request.getDescription(), request.getStatus(), request.getCategoryValues(),
                request.getOwnerIds());
            return ResponseEntity.ok(success(TaskView.from(task)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    private Long parseId(String value)
    {
        try
        {
            return Long.valueOf(value);
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
