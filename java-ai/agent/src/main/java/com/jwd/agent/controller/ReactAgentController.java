package com.jwd.agent.controller;

import com.jwd.agent.model.ReactChatRequest;
import com.jwd.agent.model.ReactChatResponse;
import com.jwd.agent.service.ReactAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/react")
@RequiredArgsConstructor
public class ReactAgentController {

    private final ReactAgentService reactAgentService;

    @PostMapping("/chat")
    public ReactChatResponse chat(@Valid @RequestBody ReactChatRequest request) {
        return reactAgentService.run(request);
    }
}
