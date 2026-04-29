package com.jwd.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {

    @SystemMessage("你是一个Java技术助手，记住用户在对话中提到的技术栈和问题背景")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
