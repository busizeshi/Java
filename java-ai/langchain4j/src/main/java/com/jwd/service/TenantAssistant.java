package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "qwenTurboModel")
public interface TenantAssistant {

    @SystemMessage("你是{{domain}}领域的专家，只需要回答{{domain}}领域的问题，其他问题一概不回答")
    String chat(@V("domain") String domain, @UserMessage String userMessage);
}
