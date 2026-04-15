package com.jwd.controller;

import com.jwd.redis.RListService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class MultiTurnController {

    private final ChatModel chatModel;

    private final RListService rListService;


    @GetMapping("/chat/multi-turn")
    public String chat(String message,String userId){
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是一个有10年经验的养宠人士，并从事了10年的宠物护理以及宠物医生工作，你需要帮助用户回答宠物相关问题。" +
                "记住，只回答宠物相关的问题"));

        List<HistoryMessage> historyMessages = rListService.getAll(userId);

        if(!historyMessages.isEmpty()){
            for (HistoryMessage historyMessage : historyMessages) {
                messages.add(new UserMessage(historyMessage.user));
                messages.add(new AiMessage(historyMessage.assistant));
            }
        }

        messages.add(new UserMessage(message));

        AiMessage response = chatModel.chat(messages).aiMessage();

        HistoryMessage newMessage = new HistoryMessage(message, response.text());
        rListService.add(userId, newMessage);
        rListService.expire(userId, 7, TimeUnit.DAYS);
        return response.text();
    }

    record HistoryMessage(String user,String assistant){}
}
