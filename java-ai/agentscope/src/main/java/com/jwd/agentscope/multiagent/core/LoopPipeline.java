package com.jwd.agentscope.multiagent.core;

import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;
import com.jwd.agentscope.multiagent.model.AgentStep;

import java.util.ArrayList;
import java.util.List;

public class LoopPipeline {

    private final DemoAgent generator;
    private final DemoAgent reviewer;
    private final double passScore;

    public LoopPipeline(DemoAgent generator, DemoAgent reviewer, double passScore) {
        this.generator = generator;
        this.reviewer = reviewer;
        this.passScore = passScore;
    }

    public List<AgentStep> run(AgentContext context, int maxIterations) {
        List<AgentStep> steps = new ArrayList<>();
        for (int round = 1; round <= maxIterations; round++) {
            context.put("iteration", round);

            long generateStart = System.currentTimeMillis();
            AgentResult generated = generator.execute(context);
            long generateElapsed = System.currentTimeMillis() - generateStart;
            context.put("draft", generated.output());
            steps.add(new AgentStep(steps.size() + 1, generator.name(), "loop-generate",
                    context.getOriginalInput(), generated.output(), generated.score(), generateElapsed));

            long reviewStart = System.currentTimeMillis();
            AgentResult reviewed = reviewer.execute(context);
            long reviewElapsed = System.currentTimeMillis() - reviewStart;
            context.put("lastOutput", reviewed.output());
            context.put("lastScore", reviewed.score());
            steps.add(new AgentStep(steps.size() + 1, reviewer.name(), "loop-review",
                    generated.output(), reviewed.output(), reviewed.score(), reviewElapsed));

            if (reviewed.score() >= passScore) {
                break;
            }
        }
        return steps;
    }
}
