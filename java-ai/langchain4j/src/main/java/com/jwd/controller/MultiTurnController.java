package com.jwd.controller;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MultiTurnController {

    private final ChatModel chatModel;


    @GetMapping("/chat/multi-turn")
    public String chat(String message,String userId){
        return null;
    }
}
