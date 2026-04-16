package com.jwd.controller;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamChatController {

    private final StreamingChatModel streamingChatModel;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam(defaultValue = "Java21的虚拟线程是什么？") String message) {
        SseEmitter emitter = new SseEmitter();

        streamingChatModel.chat(message, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse){
                try {
                    emitter.send(partialResponse);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                emitter.complete();
            }

            @Override
            public void onError(Throwable throwable) {
                emitter.completeWithError(throwable);
            }
        });

        return emitter;
    }
}
