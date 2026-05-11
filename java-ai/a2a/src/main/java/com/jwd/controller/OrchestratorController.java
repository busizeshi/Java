package com.jwd.controller;

import com.jwd.client.OrchestratorAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orchestrator")
@Tag(name = "A2A 编排器", description = "编排器同步问答接口")
public class OrchestratorController {

    private final OrchestratorAgent orchestratorAgent;

    public OrchestratorController(OrchestratorAgent orchestratorAgent) {
        this.orchestratorAgent = orchestratorAgent;
    }

    @Operation(summary = "同步询问编排器", description = "向编排器 Agent 发起同步请求并返回结果")
    @GetMapping("/ask")
    public String ask(@RequestParam("request") String request) {
        return orchestratorAgent.handle(request);
    }
}
