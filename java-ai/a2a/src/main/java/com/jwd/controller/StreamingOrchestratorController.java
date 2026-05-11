package com.jwd.controller;

import com.jwd.client.StreamingA2aClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/orchestrator")
@Tag(name = "A2A 流式编排器", description = "编排器 SSE 流式问答接口")
public class StreamingOrchestratorController {

    private final StreamingA2aClient streamingA2aClient;

    public StreamingOrchestratorController(StreamingA2aClient streamingA2aClient) {
        this.streamingA2aClient = streamingA2aClient;
    }

    /**
     * 流式问答：把 A2A SSE 事件实时转发给浏览器
     * GET /api/orchestrator/stream-ask?question=xxx&skillId=yyy
     */
    @Operation(summary = "流式询问编排器", description = "向编排器 Agent 发起请求，并通过 SSE 流式返回结果")
    @GetMapping(value = "/stream-ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAsk(@RequestParam String question, @RequestParam String skillId) {
        SseEmitter emitter = new SseEmitter(300_000L);

        streamingA2aClient.sendSubscribe(
                "http://localhost:8080",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                skillId,
                List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("type", "text", "text", question)))),
                event -> {
                    try {
                        emitter.send(event);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::complete
        );

        return emitter;
    }
}
