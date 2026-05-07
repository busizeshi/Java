package com.jwd.controller;

import com.jwd.client.OrchestratorAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orchestrator")
public class OrchestratorController {

    private final OrchestratorAgent orchestratorAgent;

    public OrchestratorController(OrchestratorAgent orchestratorAgent) {
        this.orchestratorAgent = orchestratorAgent;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam("request") String request) {
        return orchestratorAgent.handle(request);
    }
}