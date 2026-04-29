package com.jwd.store;

import com.jwd.service.ChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class RedisChatMemoryStore implements ChatMemoryStore, dev.langchain4j.store.memory.chat.ChatMemoryStore {

    private static final String KEY_PREFIX = "chat:memory:";
    private static final Duration EXPIRE_DURATION = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private String buildKey(Object memoryId) {
        return KEY_PREFIX + Objects.requireNonNull(memoryId, "memoryId must not be null");
    }

    @Override
    public List<ChatMessage> getMessage(Object memoryId) {
        return getMessages(memoryId);
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = stringRedisTemplate.opsForValue().get(buildKey(memoryId));
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        return ChatMessageDeserializer.messagesFromJson(json);
    }

    @Override
    public void updateMessage(Object memoryId, List<ChatMessage> messages) {
        updateMessages(memoryId, messages);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        String json = ChatMessageSerializer.messagesToJson(messages == null ? Collections.emptyList() : messages);
        stringRedisTemplate.opsForValue().set(key, json, EXPIRE_DURATION);
    }

    @Override
    public void deleteMessage(Object memoryId) {
        deleteMessages(memoryId);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        stringRedisTemplate.delete(buildKey(memoryId));
    }
}
