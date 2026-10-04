package com.ruoyi.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AgentCall;
import com.ruoyi.system.domain.Project;
import com.ruoyi.system.domain.Requirement;
import com.ruoyi.system.mapper.AgentCallMapper;
import com.ruoyi.system.mapper.ProjectMapper;
import com.ruoyi.system.mapper.ProjectOperationLogMapper;
import com.ruoyi.system.service.IRequirementService;
import com.ruoyi.system.service.ISysConfigService;

class AgentServiceImplTest
{
    private final ProjectMapper projectMapper = org.mockito.Mockito.mock(ProjectMapper.class);
    private final IRequirementService requirementService = org.mockito.Mockito.mock(IRequirementService.class);
    private final AgentCallMapper agentCallMapper = org.mockito.Mockito.mock(AgentCallMapper.class);
    private final ProjectOperationLogMapper logMapper = org.mockito.Mockito.mock(ProjectOperationLogMapper.class);
    private final ISysConfigService configService = org.mockito.Mockito.mock(ISysConfigService.class);
    private final AgentServiceImpl service = new AgentServiceImpl(projectMapper, requirementService,
        agentCallMapper, logMapper, configService);
    private final Project project = new Project();
    private final Requirement requirement = new Requirement();

    @BeforeEach
    void setUp()
    {
        project.setProjectId(41L);
        project.setStatus(Project.STATUS_ACTIVE);
        requirement.setRequirementId(7L);
        requirement.setProjectId(41L);
        requirement.setCurrentVersionId(501L);
        requirement.setCurrentVersionNo(2);
        requirement.setTitle("登录需求");
        requirement.setContent("实现登录");
        when(projectMapper.selectProjectForUser(41L, 21L)).thenReturn(project);
        when(requirementService.selectRequirementForUser(41L, 7L, 21L)).thenReturn(requirement);
        when(configService.selectConfigByKey("project.agent.provider")).thenReturn("remote");
        when(configService.selectConfigByKey("project.agent.model")).thenReturn("remote-v1");
        when(configService.selectConfigByKey("project.agent.external.enabled")).thenReturn("false");
        when(configService.selectConfigByKey("project.agent.retry.max")).thenReturn("2");
        when(configService.selectConfigByKey("project.agent.file.parse.enabled")).thenReturn("true");
        when(agentCallMapper.insertAgentCall(any(AgentCall.class))).thenAnswer(invocation -> {
            AgentCall call = invocation.getArgument(0);
            if (call.getCallId() == null) call.setCallId(call.getRetryOfCallId() == null ? 10L : 11L);
            return 1;
        });
        when(logMapper.insertProjectOperationLog(any())).thenReturn(1);
    }

    @Test
    void unsupportedProviderPersistsFailureSnapshotAndLog()
    {
        AgentCall call = service.call(41L, 7L, 21L, List.of(), "failure-key", true);

        assertEquals(10L, call.getCallId());
        assertEquals("FAILED", call.getStatus());
        assertEquals("remote", call.getProvider());
        assertEquals("登录需求", call.getInputTitle());
        assertEquals(501L, call.getRequirementVersionId());
        verify(agentCallMapper).insertAgentCall(call);
        verify(logMapper).insertProjectOperationLog(any());
    }

    @Test
    void retryUsesOriginalInputAndKeepsRequirementVersion()
    {
        AgentCall source = new AgentCall();
        source.setCallId(10L);
        source.setRequirementId(7L);
        source.setRequirementVersionId(501L);
        source.setInputTitle("锁定标题");
        source.setInputContent("锁定正文");
        source.setSelectedAttachmentSnapshot("[1]");
        source.setParsedAttachmentContent("[{\"status\":\"PARSED\"}]");
        source.setStatus("FAILED");
        source.setRetryCount(0);
        when(agentCallMapper.selectAgentCallForUser(41L, 7L, 10L, 21L)).thenReturn(source);
        when(configService.selectConfigByKey("project.agent.provider")).thenReturn("local");

        AgentCall retry = service.retry(41L, 7L, 10L, 21L, "retry-key", true);

        assertEquals(11L, retry.getCallId());
        assertEquals("SUCCESS", retry.getStatus());
        assertEquals(10L, retry.getRetryOfCallId());
        assertEquals(1, retry.getRetryCount());
        assertEquals(501L, retry.getRequirementVersionId());
        assertEquals("锁定标题", retry.getInputTitle());
        assertEquals("锁定正文", retry.getInputContent());
        assertEquals("[1]", retry.getSelectedAttachmentSnapshot());
        assertEquals("[{\"status\":\"PARSED\"}]", retry.getParsedAttachmentContent());
    }

    @Test
    void retryRequiresConfirmationAndHonorsConfiguredLimit()
    {
        AgentCall source = new AgentCall();
        source.setCallId(10L);
        source.setRequirementId(7L);
        source.setRequirementVersionId(501L);
        source.setStatus("FAILED");
        source.setRetryCount(1);
        when(agentCallMapper.selectAgentCallForUser(41L, 7L, 10L, 21L)).thenReturn(source);
        when(configService.selectConfigByKey("project.agent.retry.max")).thenReturn("1");

        ServiceException unconfirmed = assertThrows(ServiceException.class,
            () -> service.retry(41L, 7L, 10L, 21L, "retry-key", false));
        assertEquals(HttpStatus.BAD_REQUEST, unconfirmed.getCode());

        ServiceException limited = assertThrows(ServiceException.class,
            () -> service.retry(41L, 7L, 10L, 21L, "retry-key", true));
        assertEquals(HttpStatus.BAD_REQUEST, limited.getCode());
    }
}
