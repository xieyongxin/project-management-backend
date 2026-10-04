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
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AgentCall;
import com.ruoyi.system.service.IAgentService;
import com.ruoyi.web.domain.project.AgentCallRequest;
import com.ruoyi.web.domain.project.AgentCallView;
import com.ruoyi.web.domain.project.RequirementView;

@RestController
@RequestMapping("/project/{projectId}/requirements/{requirementId}/agent")
public class AgentController extends BaseController
{
    private final IAgentService agentService;
    public AgentController(IAgentService agentService) { this.agentService = agentService; }

    @PreAuthorize("@ss.hasPermi('project:agent:split')")
    @GetMapping("/preview")
    public ResponseEntity<?> preview(@PathVariable String projectId, @PathVariable String requirementId)
    {
        Long project = parse(projectId), requirement = parse(requirementId);
        if (project == null || requirement == null) return notFound();
        try { return ResponseEntity.ok(success(RequirementView.from(agentService.preview(project, requirement, getUserId())))); }
        catch (ServiceException e) { return error(e); }
    }

    @PreAuthorize("@ss.hasPermi('project:agent:split')")
    @PostMapping("/calls")
    public ResponseEntity<AjaxResult> call(@PathVariable String projectId, @PathVariable String requirementId,
        @Validated @RequestBody AgentCallRequest request)
    {
        Long project = parse(projectId), requirement = parse(requirementId);
        if (project == null || requirement == null) return notFound();
        try
        {
            AgentCall call = agentService.call(project, requirement, getUserId(), request.getAttachmentIds(),
                request.getIdempotencyKey(), request.isConfirmed());
            return ResponseEntity.ok(success(AgentCallView.from(call)));
        }
        catch (ServiceException e) { return error(e); }
    }

    @PreAuthorize("@ss.hasPermi('project:agent:log')")
    @GetMapping("/calls")
    public ResponseEntity<?> calls(@PathVariable String projectId, @PathVariable String requirementId)
    {
        Long project = parse(projectId);
        Long requirement = parse(requirementId);
        if (project == null || requirement == null) return notFound();
        List<AgentCall> calls = agentService.listCalls(project, requirement, getUserId());
        if (calls == null) return notFound();
        return ResponseEntity.ok(success(calls.stream().map(AgentCallView::from).toList()));
    }

    private Long parse(String value) { try { return Long.valueOf(value); } catch (Exception e) { return null; } }
    private ResponseEntity<AjaxResult> notFound() { return ResponseEntity.status(404).body(AjaxResult.error(HttpStatus.NOT_FOUND, "项目不存在或无权访问")); }
    private ResponseEntity<AjaxResult> error(ServiceException e) { int code = e.getCode() == null ? HttpStatus.ERROR : e.getCode(); return ResponseEntity.status(code >= 400 && code < 600 ? code : 200).body(AjaxResult.error(code, e.getMessage())); }
}
