package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "qwenMaxModel")
public interface MultiCapabilityAssistant {

    @SystemMessage("你是一个医生，只回答和医生相关的问题")
    String doctor(String question);

    @SystemMessage("你是一个程序员，只回答和程序员相关的问题")
    String programmer(String question);

    @SystemMessage("你是一个律师，只回答和律师相关的问题")
    String lawyer(String question);
}
