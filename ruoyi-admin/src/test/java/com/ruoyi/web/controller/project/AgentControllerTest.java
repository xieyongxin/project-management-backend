package com.ruoyi.web.controller.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AgentCall;
import com.ruoyi.system.service.IAgentService;
import com.ruoyi.web.domain.project.AgentCallRequest;
import com.ruoyi.web.domain.project.AgentRetryRequest;

class AgentControllerTest
{
    private final IAgentService agentService = org.mockito.Mockito.mock(IAgentService.class);
    private final AgentController controller = new AgentController(agentService);

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agentEndpointsDeclareIndependentPermissions()
        throws NoSuchMethodException
    {
        PreAuthorize split = AgentController.class.getMethod("call", String.class, String.class,
            AgentCallRequest.class).getAnnotation(PreAuthorize.class);
        PreAuthorize log = AgentController.class.getMethod("calls", String.class, String.class)
            .getAnnotation(PreAuthorize.class);
        PreAuthorize retry = AgentController.class.getMethod("retry", String.class, String.class,
            String.class, AgentRetryRequest.class).getAnnotation(PreAuthorize.class);

        assertNotNull(split);
        assertNotNull(log);
        assertNotNull(retry);
        assertEquals("@ss.hasPermi('project:agent:split')", split.value());
        assertEquals("@ss.hasPermi('project:agent:log')", log.value());
        assertEquals("@ss.hasPermi('project:agent:split')", retry.value());
    }

    @Test
    void memberCanRetryFailedCall()
    {
        setCurrentUser(23L);
        AgentCall source = new AgentCall();
        source.setCallId(8L);
        source.setRequirementId(7L);
        AgentCall retry = new AgentCall();
        retry.setCallId(9L);
        retry.setRequirementId(7L);
        retry.setRetryOfCallId(8L);
        retry.setRetryCount(1);
        when(agentService.retry(41L, 7L, 8L, 23L, "retry-key", true)).thenReturn(retry);

        AgentRetryRequest request = new AgentRetryRequest();
        request.setIdempotencyKey("retry-key");
        request.setConfirmed(true);

        ResponseEntity<AjaxResult> response = controller.retry("41", "7", "8", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(9L, ((com.ruoyi.web.domain.project.AgentCallView) response.getBody().get("data"))
            .getCallId());
        verify(agentService).retry(41L, 7L, 8L, 23L, "retry-key", true);
    }

    @Test
    void nonMemberAndMalformedRetryIdsDoNotExposeServiceData()
    {
        setCurrentUser(23L);
        AgentRetryRequest request = new AgentRetryRequest();
        request.setIdempotencyKey("retry-key");
        request.setConfirmed(true);
        when(agentService.retry(41L, 7L, 8L, 23L, "retry-key", true))
            .thenThrow(new ServiceException("调用记录不存在或无权访问", HttpStatus.NOT_FOUND));

        ResponseEntity<AjaxResult> denied = controller.retry("41", "7", "8", request);
        ResponseEntity<AjaxResult> malformed = controller.retry("bad", "7", "8", request);
        ResponseEntity<AjaxResult> malformedCall = controller.retry("41", "7", "bad", request);

        assertEquals(404, denied.getStatusCode().value());
        assertEquals(404, malformed.getStatusCode().value());
        assertEquals(404, malformedCall.getStatusCode().value());
        verify(agentService).retry(41L, 7L, 8L, 23L, "retry-key", true);
    }

    @Test
    void retryServiceErrorsUseBusinessStatus()
    {
        setCurrentUser(23L);
        AgentRetryRequest request = new AgentRetryRequest();
        request.setIdempotencyKey("retry-key");
        request.setConfirmed(true);
        doThrow(new ServiceException("已达到 Agent 最大重试次数", HttpStatus.BAD_REQUEST))
            .when(agentService).retry(41L, 7L, 8L, 23L, "retry-key", true);

        ResponseEntity<AjaxResult> response = controller.retry("41", "7", "8", request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(HttpStatus.BAD_REQUEST, response.getBody().get("code"));
    }

    private void setCurrentUser(Long userId)
    {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setPermissions(Set.of("project:agent:split", "project:agent:log"));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, "", List.of()));
    }
}
