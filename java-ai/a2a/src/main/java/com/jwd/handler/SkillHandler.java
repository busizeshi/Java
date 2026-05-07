package com.jwd.handler;

import com.jwd.model.server.Message;
import com.jwd.model.server.Task;

import java.util.List;

public interface SkillHandler {
    String skillId();

    Task handle(String taskId, String sessionId, List<Message> history);
}