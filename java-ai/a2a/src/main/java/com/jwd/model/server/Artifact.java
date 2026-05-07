package com.jwd.model.server;


import java.util.List;

// Artifact（任务产出物）
public record Artifact(
    String name,
    String description,
    List<Part> parts
) {}