package com.jwd.service;

import com.jwd.model.SentimentResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "qwenTurboModel")
public interface SentimentAnalyser {

    @SystemMessage("""
            你是情感分析专家，分析用户评论的情感，给出情感类别，判断依据和评分
            """)
    SentimentResult analyzeSentiment(String comment);

}