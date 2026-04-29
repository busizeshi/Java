package com.jwd.service;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Objects;

public interface ChatMemoryStore {

    List<ChatMessage> getMessage(Object memoryId);

    void updateMessage(Object memoryId, List<ChatMessage> messages);

    void deleteMessage(Object memoryId);
}
