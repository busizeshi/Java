package com.jwd.agentscope.multiagent.model;

public record AgentStep(
        int index,
        String agentName,
        String stage,
        String input,
        String output,
        double score,
        long elapsedMs
) {
}
