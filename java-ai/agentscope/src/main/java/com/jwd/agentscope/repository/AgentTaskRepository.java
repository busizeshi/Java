package com.jwd.agentscope.repository;

import com.jwd.agentscope.model.AgentTask;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AgentTaskRepository {

    private final Map<String, AgentTask> store = new ConcurrentHashMap<>();

    public void save(AgentTask task) {
        store.put(task.getTaskId(), task);
    }

    public Optional<AgentTask> findById(String taskId) {
        return Optional.ofNullable(store.get(taskId));
    }
}
