package com.jwd.agentscope.multiagent.agent;

import com.jwd.agentscope.multiagent.core.DemoAgent;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public class SqlReviewAgent implements DemoAgent {

    @Override
    public String name() {
        return "SQL 评分 Agent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String draft = context.getString("draft");
        boolean passed = draft.contains("sum(") && draft.contains("group by");
        double score = passed ? 0.9 : 0.58;
        String output = passed
                ? "SQL 评分通过，包含聚合指标和分组条件。score=" + score
                : "SQL 评分未通过，缺少聚合指标或分组条件，需要重新生成。score=" + score;
        return new AgentResult(name(), output, score);
    }
}
