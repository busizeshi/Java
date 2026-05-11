package com.jwd.agentscope.service;

import com.jwd.agentscope.model.AgentTask;
import com.jwd.agentscope.repository.AgentTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class AgentTaskService {

    private static final Logger log = LoggerFactory.getLogger(AgentTaskService.class);

    private final AgentTaskRepository repository;
    private final Executor taskExecutor;

    public AgentTaskService(AgentTaskRepository repository,
                            @Qualifier("agentScopeTaskExecutor") Executor taskExecutor) {
        this.repository = repository;
        this.taskExecutor = taskExecutor;
    }

    public AgentTask submit(String question) {
        String taskId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString().replace("-", "");

        AgentTask task = new AgentTask(taskId, traceId, question);
        repository.save(task);

        taskExecutor.execute(() -> runTask(task));
        return task;
    }

    public AgentTask getTask(String taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
    }

    private void runTask(AgentTask task) {
        MDC.put("traceId", task.getTraceId());
        try {
            task.markRunning();
            hook("observe", "Task accepted and execution started");

            hook("tool_call", "Calling simulated analysis tool");
            Thread.sleep(1200L);

            String result = "[AgentScope] 任务已完成: " + task.getQuestion();
            hook("reply", "Final response generated");
            task.markSucceeded(result);
        } catch (Exception e) {
            task.markFailed(e.getMessage());
            hook("error", "Task execution failed: " + e.getMessage());
            log.error("Task execution failed", e);
        } finally {
            MDC.remove("traceId");
        }
    }

    private void hook(String stage, String message) {
        log.info("hook_stage={} message={}", stage, message);
    }
}
