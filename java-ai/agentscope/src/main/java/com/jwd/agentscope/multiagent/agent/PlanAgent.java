package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class PlanAgent implements DemoAgent {

    @Override
    public String name() {
        return "计划拆解 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String output = """
                执行计划:
                1. 明确业务目标和输入约束
                2. 拆分专业视角并分派给子 Agent
                3. 合并输出并做质量检查
                4. 输出可执行建议
                """;
        context.put("plan", output);
        return new AgentResult(name(), output, 0.9);
    }
}
