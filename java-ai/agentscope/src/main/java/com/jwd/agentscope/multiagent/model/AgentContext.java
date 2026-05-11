package com.jwd.agentscope.multiagent.model;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class AgentContext {

    private final String originalInput;
    private final Map<String, Object> variables = new LinkedHashMap<>();

    public AgentContext(String originalInput) {
        this.originalInput = originalInput;
    }

    public void put(String key, Object value) {
        variables.put(key, value);
    }

    public Object get(String key) {
        return variables.get(key);
    }

    public String getString(String key) {
        Object value = variables.get(key);
        return value == null ? "" : value.toString();
    }
}
