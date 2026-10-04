package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AgentCall;
import com.ruoyi.system.domain.Requirement;

public interface IAgentService
{
    Requirement preview(Long projectId, Long requirementId, Long userId);

    AgentCall call(Long projectId, Long requirementId, Long userId, List<Long> attachmentIds,
        String idempotencyKey, boolean confirmed);

    AgentCall retry(Long projectId, Long requirementId, Long callId, Long userId,
        String idempotencyKey, boolean confirmed);

    List<AgentCall> listCalls(Long projectId, Long requirementId, Long userId);
}
