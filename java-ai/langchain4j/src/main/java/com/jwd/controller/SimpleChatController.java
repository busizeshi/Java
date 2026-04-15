/**
 * 简单langchain4j示例
 */
package com.jwd.controller;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SimpleChatController {

    private final ChatModel chatModel;

    @GetMapping("/chat")
    public String chat(@RequestParam String message){
        return chatModel.chat(message);
    }
}
