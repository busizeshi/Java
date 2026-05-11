package com.jwd.agentscope.multiagent.core;

import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;
import com.jwd.agentscope.multiagent.model.AgentStep;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ParallelPipeline {

    private final List<DemoAgent> agents;
    private final Executor executor;

    public ParallelPipeline(List<DemoAgent> agents, Executor executor) {
        this.agents = agents;
        this.executor = executor;
    }

    public List<AgentStep> run(AgentContext context) {
        List<CompletableFuture<AgentStep>> futures = new ArrayList<>();
        for (int i = 0; i < agents.size(); i++) {
            int index = i + 1;
            DemoAgent agent = agents.get(i);
            futures.add(CompletableFuture.supplyAsync(() -> {
                AgentContext childContext = new AgentContext(context.getOriginalInput());
                childContext.getVariables().putAll(context.getVariables());
                long start = System.currentTimeMillis();
                AgentResult result = agent.execute(childContext);
                long elapsed = System.currentTimeMillis() - start;
                return new AgentStep(index, agent.name(), "parallel", context.getOriginalInput(),
                        result.output(), result.score(), elapsed);
            }, executor));
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }
}
