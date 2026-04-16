package com.jwd.controller;

import com.jwd.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AssistantController {

    private final SimpleAssistant simpleAssistant;

    private final MultiCapabilityAssistant multiCapabilityAssistant;

    private final QwenAssistant qwenAssistant;

    private final TenantAssistant tenantAssistant;


    private final DoubaoAssistant doubaoAssistant;

    @GetMapping("/ai-service/sample-test")
    public String sampleTest(String question) {
        return simpleAssistant.answer(question);
    }

    @GetMapping("/ai-service/doctor")
    public String doctor(String question) {
        return multiCapabilityAssistant.doctor(question);
    }

    @GetMapping("/ai-service/programmer")
    public String programmer(String question) {
        return multiCapabilityAssistant.programmer(question);
    }

    @GetMapping("/ai-service/lawyer")
    public String lawyer(String question) {
        return multiCapabilityAssistant.lawyer(question);
    }

    @GetMapping("/ai-service/qwen")
    public String qwen(String question) {
        return qwenAssistant.answer(question);
    }

    @GetMapping("/ai-service/doubao")
    public String doubao(String question) {
        return doubaoAssistant.answer(question);
    }

    @GetMapping("/ai-service/tenant")
    public String tenant(String domain,String message) {
        return tenantAssistant.chat(domain, message);
    }
}
