package com.jwd.agentscope.multiagent.service;

import com.jwd.agentscope.multiagent.agent.BusinessResearchAgent;
import com.jwd.agentscope.multiagent.agent.DraftSqlAgent;
import com.jwd.agentscope.multiagent.agent.IntentAnalysisAgent;
import com.jwd.agentscope.multiagent.agent.PlanAgent;
import com.jwd.agentscope.multiagent.agent.RiskReviewAgent;
import com.jwd.agentscope.multiagent.agent.SqlReviewAgent;
import com.jwd.agentscope.multiagent.agent.SummaryAgent;
import com.jwd.agentscope.multiagent.agent.TechnologyResearchAgent;
import com.jwd.agentscope.multiagent.core.LoopPipeline;
import com.jwd.agentscope.multiagent.core.ParallelPipeline;
import com.jwd.agentscope.multiagent.core.SequentialPipeline;
import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentStep;
import com.jwd.agentscope.multiagent.model.PipelineMode;
import com.jwd.agentscope.multiagent.model.PipelineResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class MultiAgentPipelineService {

    private final Executor executor;

    public MultiAgentPipelineService(@Qualifier("agentScopeTaskExecutor") Executor executor) {
        this.executor = executor;
    }

    public PipelineResponse runSequential(String input) {
        long start = System.currentTimeMillis();
        AgentContext context = new AgentContext(input);
        SequentialPipeline pipeline = new SequentialPipeline(List.of(
                new IntentAnalysisAgent(),
                new PlanAgent(),
                new SummaryAgent()
        ));
        List<AgentStep> steps = pipeline.run(context);
        return response(PipelineMode.SEQUENTIAL, input, start, steps);
    }

    public PipelineResponse runParallel(String input) {
        long start = System.currentTimeMillis();
        AgentContext context = new AgentContext(input);
        ParallelPipeline pipeline = new ParallelPipeline(List.of(
                new TechnologyResearchAgent(),
                new BusinessResearchAgent(),
                new RiskReviewAgent()
        ), executor);
        List<AgentStep> steps = pipeline.run(context);
        return response(PipelineMode.PARALLEL, input, start, steps);
    }

    public PipelineResponse runLoop(String input, int maxIterations) {
        long start = System.currentTimeMillis();
        AgentContext context = new AgentContext(input);
        LoopPipeline pipeline = new LoopPipeline(new DraftSqlAgent(), new SqlReviewAgent(), 0.8);
        List<AgentStep> steps = pipeline.run(context, Math.max(1, maxIterations));
        return response(PipelineMode.LOOP, input, start, steps);
    }

    private PipelineResponse response(PipelineMode mode, String input, long start, List<AgentStep> steps) {
        String finalOutput = steps.isEmpty()
                ? ""
                : steps.stream().map(AgentStep::output).collect(Collectors.joining("\n\n"));
        boolean success = steps.isEmpty() || steps.getLast().score() >= 0.8;
        return new PipelineResponse(
                UUID.randomUUID().toString().replace("-", ""),
                mode,
                input,
                success,
                finalOutput,
                System.currentTimeMillis() - start,
                steps
        );
    }
}
