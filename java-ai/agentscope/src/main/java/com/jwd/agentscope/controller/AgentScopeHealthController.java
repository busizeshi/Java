package com.jwd.agentscope.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/agentscope")
@Tag(name = "AgentScope 健康检查", description = "AgentScope 模块健康检查接口")
public class AgentScopeHealthController {

    @Operation(summary = "检查 AgentScope 模块状态", description = "用于确认 AgentScope 模块是否正常启动")
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "module", "agentscope",
                "status", "ok",
                "timestamp", OffsetDateTime.now().toString()
        );
    }
}
