package com.jwd.agentscope.multiagent.core;

import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;
import com.jwd.agentscope.multiagent.model.AgentStep;

import java.util.ArrayList;
import java.util.List;

public class SequentialPipeline {

    private final List<DemoAgent> agents;

    public SequentialPipeline(List<DemoAgent> agents) {
        this.agents = agents;
    }

    public List<AgentStep> run(AgentContext context) {
        List<AgentStep> steps = new ArrayList<>();
        for (int i = 0; i < agents.size(); i++) {
            DemoAgent agent = agents.get(i);
            long start = System.currentTimeMillis();
            AgentResult result = agent.execute(context);
            long elapsed = System.currentTimeMillis() - start;
            context.put("lastOutput", result.output());
            steps.add(new AgentStep(i + 1, agent.name(), "sequential", context.getOriginalInput(),
                    result.output(), result.score(), elapsed));
        }
        return steps;
    }
}
