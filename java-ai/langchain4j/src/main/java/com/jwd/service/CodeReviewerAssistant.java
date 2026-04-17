package com.jwd.service;

import dev.langchain4j.service.V;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenTurboModel")
public interface CodeReviewerAssistant {

    @SystemMessage("你是一个有10年经验的软件开发工程师，你的任务是审查代码并提供反馈。")
    @UserMessage("请审查以下代码并提供反馈：\n{{code}}\n语言：{{language}}\n关注点：{{focusArea}}")
    String reviewCode(@V("code") String code, @V("language") String language, @V("focusArea") String focusArea);
}
