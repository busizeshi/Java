package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,chatModel = "qwenTurboModel")
public interface FileBasedAssistant {

    @SystemMessage(fromResource = "prompts/customer-service.txt")
    String chat(@V("companyName") String company,
                @V("serviceScope") String serviceScope,
                @UserMessage String message);
}
