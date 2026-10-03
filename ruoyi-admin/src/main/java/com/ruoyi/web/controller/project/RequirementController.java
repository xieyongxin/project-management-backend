package com.ruoyi.web.controller.project;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.service.IRequirementService;
import com.ruoyi.web.domain.project.RequirementCreateRequest;
import com.ruoyi.web.domain.project.RequirementContentUpdateRequest;
import com.ruoyi.web.domain.project.RequirementStatusUpdateRequest;
import com.ruoyi.web.domain.project.RequirementView;
import com.ruoyi.web.domain.project.RequirementVersionView;

@RestController
@RequestMapping("/project/{projectId}/requirements")
public class RequirementController extends BaseController
{
    private final IRequirementService requirementService;

    public RequirementController(IRequirementService requirementService)
    {
        this.requirementService = requirementService;
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:list')")
    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId)
    {
        Long parsedProjectId = parseId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        startPage();
        List<Requirement> requirements = requirementService.selectRequirementsForUser(parsedProjectId, getUserId());
        if (requirements == null)
        {
            return notFound();
        }
        TableDataInfo result = getDataTable(requirements);
        result.setRows(requirements.stream().map(RequirementView::from).toList());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:list')")
    @GetMapping("/{requirementId}")
    public ResponseEntity<?> detail(@PathVariable String projectId, @PathVariable String requirementId)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedRequirementId = parseId(requirementId);
        if (parsedProjectId == null || parsedRequirementId == null)
        {
            return notFound();
        }
        Requirement requirement = requirementService.selectRequirementForUser(parsedProjectId,
            parsedRequirementId, getUserId());
        if (requirement == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(RequirementView.from(requirement)));
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:list')")
    @GetMapping("/{requirementId}/versions")
    public ResponseEntity<?> versions(@PathVariable String projectId, @PathVariable String requirementId)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedRequirementId = parseId(requirementId);
        if (parsedProjectId == null || parsedRequirementId == null)
        {
            return notFound();
        }
        List<com.ruoyi.system.domain.RequirementVersion> versions = requirementService
            .selectRequirementVersionsForUser(parsedProjectId, parsedRequirementId, getUserId());
        if (versions == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(versions.stream().map(RequirementVersionView::from).toList()));
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:list')")
    @GetMapping("/{requirementId}/versions/compare")
    public ResponseEntity<?> compareVersions(@PathVariable String projectId, @PathVariable String requirementId,
        @RequestParam String leftVersionId, @RequestParam String rightVersionId)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedRequirementId = parseId(requirementId);
        Long parsedLeftVersionId = parseId(leftVersionId);
        Long parsedRightVersionId = parseId(rightVersionId);
        if (parsedProjectId == null || parsedRequirementId == null || parsedLeftVersionId == null
            || parsedRightVersionId == null)
        {
            return notFound();
        }
        try
        {
            List<com.ruoyi.system.domain.RequirementVersion> versions = requirementService
                .compareRequirementVersionsForUser(parsedProjectId, parsedRequirementId, parsedLeftVersionId,
                    parsedRightVersionId, getUserId());
            if (versions == null)
            {
                return notFound();
            }
            return ResponseEntity.ok(success(Map.of("left", RequirementVersionView.from(versions.get(0)),
                "right", RequirementVersionView.from(versions.get(1)))));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:status')")
    @PutMapping("/{requirementId}/status")
    public ResponseEntity<AjaxResult> updateStatus(@PathVariable String projectId, @PathVariable String requirementId,
        @Validated @RequestBody RequirementStatusUpdateRequest request)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedRequirementId = parseId(requirementId);
        if (parsedProjectId == null || parsedRequirementId == null)
        {
            return notFound();
        }
        try
        {
            Requirement requirement = requirementService.updateRequirementStatus(parsedProjectId,
                parsedRequirementId, getUserId(), request.getStatus());
            return ResponseEntity.ok(success(RequirementView.from(requirement)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:edit')")
    @PutMapping("/{requirementId}/content")
    public ResponseEntity<AjaxResult> updateContent(@PathVariable String projectId, @PathVariable String requirementId,
        @Validated @RequestBody RequirementContentUpdateRequest request)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedRequirementId = parseId(requirementId);
        if (parsedProjectId == null || parsedRequirementId == null)
        {
            return notFound();
        }
        try
        {
            Requirement requirement = requirementService.updateRequirementContent(parsedProjectId,
                parsedRequirementId, getUserId(), request.getTitle(), request.getContent());
            return ResponseEntity.ok(success(RequirementView.from(requirement)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:delete')")
    @DeleteMapping("/{requirementId}")
    public ResponseEntity<AjaxResult> delete(@PathVariable String projectId, @PathVariable String requirementId)
    {
        Long parsedProjectId = parseId(projectId);
        Long parsedRequirementId = parseId(requirementId);
        if (parsedProjectId == null || parsedRequirementId == null)
        {
            return notFound();
        }
        try
        {
            Requirement requirement = requirementService.deleteRequirement(parsedProjectId,
                parsedRequirementId, getUserId());
            return ResponseEntity.ok(success(RequirementView.from(requirement)));
        }
        catch (ServiceException e)
        {
            return serviceError(e);
        }
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:add')")
    @GetMapping("/statuses")
    public ResponseEntity<?> statuses(@PathVariable String projectId)
    {
        Long parsedProjectId = parseId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        List<SysDictData> statuses = requirementService.selectActiveStatuses(parsedProjectId, getUserId());
        if (statuses == null)
        {
            return notFound();
        }
        return ResponseEntity.ok(success(statuses));
    }

    @PreAuthorize("@ss.hasPermi('project:requirement:add')")
    @PostMapping
    public ResponseEntity<AjaxResult> create(@PathVariable String projectId,
        @Validated @RequestBody RequirementCreateRequest request)
    {
        Long parsedProjectId = parseId(projectId);
        if (parsedProjectId == null)
        {
            return notFound();
        }
        try
        {
            Requirement requirement = requirementService.createRequirement(parsedProjectId, getUserId(),
                request.getTitle(), request.getContent(), request.getStatus(), request.getOwnerIds());
            return ResponseEntity.ok(success(RequirementView.from(requirement)));
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
