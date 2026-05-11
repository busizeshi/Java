package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class DraftSqlAgent implements DemoAgent {

    @Override
    public String name() {
        return "SQL 生成 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        int iteration = (int) context.get("iteration");
        String output = iteration == 1
                ? "select * from sales where region = 'east'"
                : "select region, sum(amount) as total_amount from sales where region = 'east' group by region";
        return new AgentResult(name(), output, iteration == 1 ? 0.62 : 0.86);
    }
}
