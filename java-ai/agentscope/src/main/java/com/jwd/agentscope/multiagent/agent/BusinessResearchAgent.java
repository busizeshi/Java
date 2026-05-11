package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class BusinessResearchAgent implements DemoAgent {

    @Override
    public String name() {
        return "商业分析 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String output = "商业视角: 多 Agent 协作适合调研、审查、报告生成等高复杂度任务，价值在于分工明确和结果可复核。";
        return new AgentResult(name(), output, 0.84);
    }
}
