package com.jwd.service;

import com.jwd.store.RedisChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChatService {
    private final ChatAssistant assistant;
    private final RedisChatMemoryStore redisChatMemoryStore;

    public ChatService(@Qualifier("qwenTurboModel") ChatModel model, RedisChatMemoryStore redisChatMemoryStore) {
        this.redisChatMemoryStore = redisChatMemoryStore;
        this.assistant = AiServices.builder(ChatAssistant.class)
                .chatModel(model)
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.builder()
                                .id(memoryId)
                                .maxMessages(20)
                                .chatMemoryStore(redisChatMemoryStore)
                                .build())
                .build();
    }

    public String chat(String sessionId, String message) {
        log.info("sessionId: {}, message: {}", sessionId, message);
        String response = assistant.chat(sessionId, message);
        log.info("sessionId: {}, response: {}", sessionId, response);
        return response;
    }

    public List<ChatMessage> getMessages(String sessionId) {
        return redisChatMemoryStore.getMessages(sessionId);
    }

    public void clearMemory(String sessionId) {
        redisChatMemoryStore.deleteMessages(sessionId);
    }
}
