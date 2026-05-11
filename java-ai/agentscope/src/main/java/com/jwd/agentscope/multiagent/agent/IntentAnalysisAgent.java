package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class IntentAnalysisAgent implements DemoAgent {

    @Override
    public String name() {
        return "意图分析 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String output = "已识别用户目标: " + context.getOriginalInput()
                + "。任务类型: 多 Agent 协作分析。建议拆分为技术、商业、风险三个视角。";
        context.put("intent", output);
        return new AgentResult(name(), output, 0.86);
    }
}
