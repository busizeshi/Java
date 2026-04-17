package com.jwd.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, streamingChatModel = "qwenStreamingModel")
public interface StreamingAssistant {

    @SystemMessage("你是一个Java工程师。")
    TokenStream write(String topic);
}
