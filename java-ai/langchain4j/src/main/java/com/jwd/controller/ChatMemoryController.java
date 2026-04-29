package com.jwd.controller;

import dev.langchain4j.data.message.ChatMessage;
import com.jwd.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/memory/chat")
public class ChatMemoryController {

    private final ChatService chatService;

    public ChatMemoryController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 多轮对话接口
     * X-Session-Id Header 用来标识会话，相同值共享对话历史
     */
    @PostMapping
    public Map<String, String> chat(
            @RequestBody ChatRequest req,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        String reply = chatService.chat(sessionId, req.message());
        return Map.of(
                "sessionId", sessionId,
                "reply", reply
        );
    }

    /**
     * 开始新会话：生成新 sessionId，等同于清空历史
     */
    @PostMapping("/new-session")
    public Map<String, String> newSession() {
        String newSessionId = UUID.randomUUID().toString();
        return Map.of("sessionId", newSessionId);
    }

    @GetMapping("/{sessionId}/messages")
    public List<ChatMessage> messages(@PathVariable String sessionId) {
        return chatService.getMessages(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    public Map<String, String> clearSession(@PathVariable String sessionId) {
        chatService.clearMemory(sessionId);
        return Map.of(
                "sessionId", sessionId,
                "message", "memory cleared"
        );
    }

    record ChatRequest(String message) {
    }
}
