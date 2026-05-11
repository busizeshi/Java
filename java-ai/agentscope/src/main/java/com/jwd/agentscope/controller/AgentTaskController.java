package com.jwd.agentscope.controller;

import com.jwd.agentscope.model.AgentTask;
import com.jwd.agentscope.service.AgentTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/agentscope/tasks")
@Tag(name = "AgentScope 任务", description = "AgentScope 任务提交与状态查询接口")
public class AgentTaskController {

    private final AgentTaskService taskService;

    public AgentTaskController(AgentTaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "提交 AgentScope 任务", description = "提交任务后立即返回 taskId 和 traceId，任务在后台异步执行")
    @GetMapping("/submit")
    public Map<String, Object> submit(@RequestParam("question") String question) {
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question cannot be blank");
        }
        AgentTask task = taskService.submit(question);
        return Map.of(
                "taskId", task.getTaskId(),
                "traceId", task.getTraceId(),
                "state", task.getState().name()
        );
    }

    @Operation(summary = "查询 AgentScope 任务状态", description = "根据 taskId 查询任务状态、执行结果和错误信息")
    @GetMapping("/status")
    public Map<String, Object> status(@RequestParam("taskId") String taskId) {
        try {
            AgentTask task = taskService.getTask(taskId);
            return Map.of(
                    "taskId", task.getTaskId(),
                    "traceId", task.getTraceId(),
                    "state", task.getState().name(),
                    "result", task.getResult() == null ? "" : task.getResult(),
                    "error", task.getError() == null ? "" : task.getError(),
                    "createdAt", task.getCreatedAt().toString(),
                    "updatedAt", task.getUpdatedAt().toString()
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
