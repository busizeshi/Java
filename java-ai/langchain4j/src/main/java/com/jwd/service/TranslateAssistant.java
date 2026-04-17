package com.jwd.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(wiringMode = EXPLICIT, chatModel = "qwenTurboModel")
public interface TranslateAssistant {

    @SystemMessage("你是一个专业的翻译，只能翻译，不能做其他事情")
    @UserMessage("请将文本{{text}}翻译成{{targetLanguage}}")
    String translate(@V("text") String text, @V("targetLanguage") String targetLanguage);

    String speech(@UserMessage String message);
}
