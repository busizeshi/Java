package com.jwd.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwd.handler.SkillHandler;
import com.jwd.model.server.*;
import com.jwd.repository.TaskRepository;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
public class A2aController {

    private final Map<String, SkillHandler> skillHandlers;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    public A2aController(
            List<SkillHandler> handlers,
            TaskRepository taskRepository,
            ObjectMapper objectMapper) {

        this.skillHandlers = new HashMap<>();
        handlers.forEach(h -> skillHandlers.put(h.skillId(), h));
        this.taskRepository = taskRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/", consumes = "application/json", produces = "application/json")
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request) {
        return switch (request.method()) {
            case "tasks/send"   -> handleTaskSend(request);
            case "tasks/get"    -> handleTaskGet(request);
            case "tasks/cancel" -> handleTaskCancel(request);
            default -> JsonRpcResponse.error(-32601, "Method not found: " + request.method(), request.id());
        };
    }

    private JsonRpcResponse handleTaskSend(JsonRpcRequest request) {
        try {
            // 解析参数
            Map<String, Object> params = request.params();
            String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());
            String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
            String skillId = (String) params.get("skillId");

            // 解析消息历史
            List<Map<String, Object>> rawHistory = (List<Map<String, Object>>) params.get("history");
            List<Message> history = parseHistory(rawHistory);

            // 先保存 submitted 状态
            Task submittedTask = new Task(
                    taskId, sessionId,
                    new TaskStatus(TaskState.SUBMITTED, null, Instant.now().toString()),
                    history, List.of(), Map.of()
            );
            taskRepository.save(submittedTask);

            // 找到对应的 Skill 处理器
            SkillHandler handler = skillHandlers.get(skillId);
            if (handler == null) {
                return JsonRpcResponse.error(-32602,
                        "Unknown skill: " + skillId, request.id());
            }

            // 执行 Skill
            Task completedTask = handler.handle(taskId, sessionId, history);
            taskRepository.save(completedTask);

            return JsonRpcResponse.success(completedTask, request.id());

        } catch (Exception e) {
            return JsonRpcResponse.error(-32603, "Internal error: " + e.getMessage(), request.id());
        }
    }

    private JsonRpcResponse handleTaskGet(JsonRpcRequest request) {
        String taskId = (String) request.params().get("id");
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return JsonRpcResponse.error(-32602, "Task not found: " + taskId, request.id());
        }

        return JsonRpcResponse.success(task, request.id());
    }

    private JsonRpcResponse handleTaskCancel(JsonRpcRequest request) {
        String taskId = (String) request.params().get("id");
        Task task = taskRepository.findById(taskId);

        if (task == null) {
            return JsonRpcResponse.error(-32602, "Task not found: " + taskId, request.id());
        }

        Task canceledTask = new Task(
                task.id(), task.sessionId(),
                new TaskStatus(TaskState.CANCELED, null, Instant.now().toString()),
                task.history(), task.artifacts(), task.metadata()
        );
        taskRepository.save(canceledTask);

        return JsonRpcResponse.success(canceledTask, request.id());
    }

    @SuppressWarnings("unchecked")
    private List<Message> parseHistory(List<Map<String, Object>> rawHistory) {
        if (rawHistory == null) return List.of();
        return rawHistory.stream().map(raw -> {
            String role = (String) raw.get("role");
            List<Map<String, Object>> rawParts = (List<Map<String, Object>>) raw.get("parts");
            List<Part> parts = rawParts.stream().map(p -> {
                String type = (String) p.get("type");
                return switch (type) {
                    case "text" -> Part.text((String) p.get("text"));
                    case "data" -> Part.data((Map<String, Object>) p.get("data"));
                    default -> Part.text("");
                };
            }).toList();
            return new Message(role, parts);
        }).toList();
    }
}