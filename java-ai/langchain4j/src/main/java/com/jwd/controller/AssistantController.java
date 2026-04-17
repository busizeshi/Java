package com.jwd.controller;

import com.jwd.model.CodeReviewRequest;
import com.jwd.model.ContractInfo;
import com.jwd.model.SentimentResult;
import com.jwd.model.TicketCategory;
import com.jwd.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class AssistantController {

    private final SimpleAssistant simpleAssistant;

    private final MultiCapabilityAssistant multiCapabilityAssistant;

    private final QwenAssistant qwenAssistant;

    private final TenantAssistant tenantAssistant;

    private final DoubaoAssistant doubaoAssistant;

    private final TranslateAssistant translateAssistant;

    private final CodeReviewerAssistant codeReviewerAssistant;

    private final SentimentAnalyser sentimentAnalyser;

    private final TicketClassifier ticketClassifier;

    private final ContractExtractor contractExtractor;

    private final StreamingAssistant streamingAssistant;

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

    @GetMapping("/ai-service/translate")
    public String translate(String text, String targetLanguage) {
        return translateAssistant.translate(text, targetLanguage);
    }

    @GetMapping("/ai-service/speech")
    public String speech(String message) {
        return translateAssistant.speech(message);
    }

    @PostMapping("/ai-service/code-review")
    public String codeReview(@RequestBody CodeReviewRequest request) {
        return codeReviewerAssistant.reviewCode(request.code(), request.language(), request.focusArea());
    }

//    @GetMapping("/ai-service/customer-service")
//    public String customerService(String company,
//                                  @RequestParam(defaultValue = "产品咨询、售后服务、投诉建议") String serviceScope,
//                                  String message) {
//        return fileBasedAssistant.chat(company, serviceScope, message);
//    }

    @GetMapping("/ai-service/sentiment-analysis")
    public SentimentResult sentimentAnalysis(String comment) {
        return sentimentAnalyser.analyzeSentiment(comment);
    }
    @GetMapping("/ai-service/ticket-classifier")
    public TicketCategory ticketClassifier(String ticket) {
        return ticketClassifier.classify(ticket);
    }
    @GetMapping("/ai-service/contract-extractor")
    public ContractInfo contractExtractor(String text) {
        return contractExtractor.extractContractInfo(text);
    }

    @GetMapping(value = "/ai-service/streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter writeStream(@RequestParam String topic) {
        SseEmitter emitter=new SseEmitter(60_000L);

        streamingAssistant.write(topic).onPartialResponse(
                token->{
                    try{
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }
        )
        .onCompleteResponse(
                (response)-> emitter.complete()
        ).onError(emitter::completeWithError).start();

        return emitter;
    }
}
