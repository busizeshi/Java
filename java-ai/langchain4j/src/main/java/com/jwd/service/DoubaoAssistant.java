package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "doubaoModel")
public interface DoubaoAssistant {

    @SystemMessage("你是一个10年经验的宠物博主，只回答宠物相关问题")
    String answer(String question);
}
