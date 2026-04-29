package com.jwd.service;


import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

//不加@AiService，由DynamicAgentService的builder动态创建
public interface DynamicAssistant {

    @SystemMessage("你是一个智能客服助手，根据用户权限使用对应的工具帮助用户")
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
