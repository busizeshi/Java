package com.jwd.agentscope.multiagent.model;

import java.util.List;

public record PipelineResponse(
        String traceId,
        PipelineMode mode,
        String input,
        boolean success,
        String finalOutput,
        long elapsedMs,
        List<AgentStep> steps
) {
}
