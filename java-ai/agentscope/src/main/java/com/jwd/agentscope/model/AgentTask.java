package com.jwd.agentscope.model;

import java.time.OffsetDateTime;

public class AgentTask {

    private final String taskId;
    private final String traceId;
    private final String question;
    private volatile AgentTaskState state;
    private volatile String result;
    private volatile String error;
    private final OffsetDateTime createdAt;
    private volatile OffsetDateTime updatedAt;

    public AgentTask(String taskId, String traceId, String question) {
        this.taskId = taskId;
        this.traceId = traceId;
        this.question = question;
        this.state = AgentTaskState.SUBMITTED;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getTaskId() { return taskId; }
    public String getTraceId() { return traceId; }
    public String getQuestion() { return question; }
    public AgentTaskState getState() { return state; }
    public String getResult() { return result; }
    public String getError() { return error; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void markRunning() {
        this.state = AgentTaskState.RUNNING;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markSucceeded(String result) {
        this.state = AgentTaskState.SUCCEEDED;
        this.result = result;
        this.updatedAt = OffsetDateTime.now();
    }

    public void markFailed(String error) {
        this.state = AgentTaskState.FAILED;
        this.error = error;
        this.updatedAt = OffsetDateTime.now();
    }
}
