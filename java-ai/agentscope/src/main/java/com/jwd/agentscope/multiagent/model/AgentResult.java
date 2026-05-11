package com.jwd.agentscope.multiagent.model;

public record AgentResult(
        String agentName,
        String output,
        double score
) {
}
