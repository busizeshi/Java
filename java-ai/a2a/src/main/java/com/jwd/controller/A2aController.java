package com.jwd.controller;

import com.jwd.handler.SkillHandler;
import com.jwd.model.server.JsonRpcRequest;
import com.jwd.model.server.JsonRpcResponse;
import com.jwd.model.server.Message;
import com.jwd.model.server.Part;
import com.jwd.model.server.Task;
import com.jwd.model.server.TaskState;
import com.jwd.model.server.TaskStatus;
import com.jwd.repository.AsyncTaskExecutor;
import com.jwd.repository.TaskRepository;
import com.jwd.repository.WebhookRegistry;
import com.jwd.skill.ReportGenerationSkillHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "A2A 核心协议", description = "A2A JSON-RPC 核心接口")
public class A2aController {

    private final Map<String, SkillHandler> skillHandlers;
    private final TaskRepository taskRepository;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final WebhookRegistry webhookRegistry;

    public A2aController(
            List<SkillHandler> handlers,
            TaskRepository taskRepository,
            AsyncTaskExecutor asyncTaskExecutor,
            WebhookRegistry webhookRegistry) {

        this.skillHandlers = new HashMap<>();
        handlers.forEach(h -> skillHandlers.put(h.skillId(), h));
        this.taskRepository = taskRepository;
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.webhookRegistry = webhookRegistry;
    }

    @Operation(summary = "A2A JSON-RPC 统一入口", description = "支持 tasks/send、tasks/get、tasks/cancel、tasks/pushNotification/set")
    @PostMapping(value = "/", consumes = "application/json", produces = "application/json")
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request) {
        return switch (request.method()) {
            case "tasks/send" -> handleTaskSend(request);
            case "tasks/get" -> handleTaskGet(request);
            case "tasks/cancel" -> handleTaskCancel(request);
            case "tasks/pushNotification/set" -> handlePushNotificationSet(request);
            default -> JsonRpcResponse.error(-32601, "Method not found: " + request.method(), request.id());
        };
    }

    @SuppressWarnings("unchecked")
    private JsonRpcResponse handlePushNotificationSet(JsonRpcRequest request) {
        Map<String, Object> params = request.params();
        String taskId = (String) params.get("id");
        String webhookUrl = (String) params.get("webhookUrl");

        if (taskId == null || taskId.isBlank() || webhookUrl == null || webhookUrl.isBlank()) {
            return JsonRpcResponse.error(-32602, "id and webhookUrl are required", request.id());
        }

        webhookRegistry.register(taskId, webhookUrl);
        return JsonRpcResponse.success(Map.of("id", taskId, "webhookUrl", webhookUrl, "registered", true), request.id());
    }

    @SuppressWarnings("unchecked")
    private JsonRpcResponse handleTaskSend(JsonRpcRequest request) {
        try {
            Map<String, Object> params = request.params();
            String taskId = (String) params.getOrDefault("id", UUID.randomUUID().toString());
            String sessionId = (String) params.getOrDefault("sessionId", UUID.randomUUID().toString());
            String skillId = (String) params.get("skillId");

            List<Map<String, Object>> rawHistory = (List<Map<String, Object>>) params.get("history");
            List<Message> history = parseHistory(rawHistory);

            SkillHandler handler = skillHandlers.get(skillId);
            if (handler == null) {
                return JsonRpcResponse.error(-32602, "Unknown skill: " + skillId, request.id());
            }

            Task existingTask = taskRepository.findById(taskId);
            if (existingTask != null
                    && existingTask.status().state() == TaskState.INPUT_REQUIRED
                    && handler instanceof ReportGenerationSkillHandler reportHandler) {
                Task continuedTask = reportHandler.continueTask(taskId, sessionId, history);
                taskRepository.save(continuedTask);
                return JsonRpcResponse.success(continuedTask, request.id());
            }

            Task submittedTask = new Task(
                    taskId,
                    sessionId,
                    new TaskStatus(TaskState.SUBMITTED, null, Instant.now().toString()),
                    history,
                    List.of(),
                    Map.of()
            );
            taskRepository.save(submittedTask);

            asyncTaskExecutor.submitAsync(taskId, sessionId, handler, history);
            return JsonRpcResponse.success(submittedTask, request.id());
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
                task.id(),
                task.sessionId(),
                new TaskStatus(TaskState.CANCELED, null, Instant.now().toString()),
                task.history(),
                task.artifacts(),
                task.metadata()
        );
        taskRepository.save(canceledTask);

        return JsonRpcResponse.success(canceledTask, request.id());
    }

    @SuppressWarnings("unchecked")
    private List<Message> parseHistory(List<Map<String, Object>> rawHistory) {
        if (rawHistory == null) {
            return List.of();
        }
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
