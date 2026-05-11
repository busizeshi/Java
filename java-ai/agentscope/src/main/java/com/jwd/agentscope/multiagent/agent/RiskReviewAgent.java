package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class RiskReviewAgent implements DemoAgent {

    @Override
    public String name() {
        return "风险审查 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String output = "风险视角: 需要控制 Agent 边界、记录每步输出、避免角色串场，并对关键动作加入人工确认。";
        return new AgentResult(name(), output, 0.87);
    }
}
