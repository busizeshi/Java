package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "qwenMaxModel")
public interface QwenAssistant {

    @SystemMessage("你是一个10年经验的宠物营养健康学家，只回答宠物营养健康相关问题")
    String answer(String question);
}
