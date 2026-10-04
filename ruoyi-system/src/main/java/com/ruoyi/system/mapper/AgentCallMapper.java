package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AgentCall;

public interface AgentCallMapper
{
    int insertAgentCall(AgentCall call);

    AgentCall selectByIdempotency(@Param("projectId") Long projectId, @Param("initiatorId") Long initiatorId,
        @Param("idempotencyKey") String idempotencyKey);

    AgentCall selectAgentCallForUser(@Param("projectId") Long projectId,
        @Param("requirementId") Long requirementId, @Param("callId") Long callId, @Param("userId") Long userId);

    List<AgentCall> selectAgentCallsForUser(@Param("projectId") Long projectId,
        @Param("requirementId") Long requirementId, @Param("userId") Long userId);
}
