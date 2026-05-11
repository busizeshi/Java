package com.jwd.agentscope.multiagent.controller;

import com.jwd.agentscope.multiagent.model.PipelineRequest;
import com.jwd.agentscope.multiagent.model.PipelineResponse;
import com.jwd.agentscope.multiagent.service.MultiAgentPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/agentscope/multi-agent")
@Tag(name = "AgentScope 多 Agent 编排", description = "Sequential、Parallel、Loop 三种多 Agent 协作模式演示接口")
public class MultiAgentPipelineController {

    private final MultiAgentPipelineService pipelineService;

    public MultiAgentPipelineController(MultiAgentPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @Operation(summary = "顺序编排", description = "多个 Agent 按顺序执行，前一个 Agent 的输出会影响后一个 Agent")
    @PostMapping("/sequential")
    public PipelineResponse sequential(@RequestBody PipelineRequest request) {
        return pipelineService.runSequential(requiredInput(request));
    }

    @Operation(summary = "并行编排", description = "多个 Agent 基于同一输入并行分析不同视角，最后合并结果")
    @PostMapping("/parallel")
    public PipelineResponse parallel(@RequestBody PipelineRequest request) {
        return pipelineService.runParallel(requiredInput(request));
    }

    @Operation(summary = "循环编排", description = "生成 Agent 与评分 Agent 反复协作，直到质量分达标或达到最大迭代次数")
    @PostMapping("/loop")
    public PipelineResponse loop(@RequestBody PipelineRequest request) {
        int maxIterations = request.maxIterations() == null ? 3 : request.maxIterations();
        return pipelineService.runLoop(requiredInput(request), maxIterations);
    }

    private String requiredInput(PipelineRequest request) {
        if (request == null || request.input() == null || request.input().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "input 不能为空");
        }
        return request.input();
    }
}
