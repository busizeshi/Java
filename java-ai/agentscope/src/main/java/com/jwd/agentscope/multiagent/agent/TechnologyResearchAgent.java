package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class TechnologyResearchAgent implements DemoAgent {

    @Override
    public String name() {
        return "技术研究 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String output = "技术视角: " + context.getOriginalInput()
                + " 可拆成 Agent 抽象、Pipeline 编排、Hook 可观测、状态持久化四个工程点。";
        return new AgentResult(name(), output, 0.88);
    }
}
