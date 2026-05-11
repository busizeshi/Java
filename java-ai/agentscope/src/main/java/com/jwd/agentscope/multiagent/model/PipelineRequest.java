package com.jwd.agentscope.multiagent.model;

public record PipelineRequest(
        String input,
        Integer maxIterations
) {
}
