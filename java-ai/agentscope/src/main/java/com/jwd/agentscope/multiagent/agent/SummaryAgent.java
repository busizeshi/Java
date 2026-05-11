package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class SummaryAgent implements DemoAgent {

    @Override
    public String name() {
        return "总结 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String output = "最终结论: " + context.getString("intent")
                + " " + context.getString("plan")
                + "建议先用顺序编排跑通依赖链路，再用并行编排提升多视角分析效率。";
        context.put("summary", output);
        return new AgentResult(name(), output, 0.92);
    }
}
