package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "qwenMaxModel")
public interface SimpleAssistant {

    @SystemMessage("你是一个宠物博主，宠物医生，宠物健康管理学家，有10年经验，只回答宠物相关的问题")
    String answer(String userMessage);
}
