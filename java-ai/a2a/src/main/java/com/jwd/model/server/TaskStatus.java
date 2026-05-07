package com.jwd.model.server;

public record TaskStatus(
    TaskState state,
    Message message,
    String timestamp
) {}
