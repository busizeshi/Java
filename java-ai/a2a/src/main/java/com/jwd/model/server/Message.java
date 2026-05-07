package com.jwd.model.server;

import java.util.List;

// Message
public record Message(
    String role,   // "user" 或 "agent"
    List<Part> parts
) {}