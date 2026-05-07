package com.jwd.agent.service;

import com.jwd.agent.config.AgentProperties;
import com.jwd.agent.model.ReactChatRequest;
import com.jwd.agent.model.ReactChatResponse;
import com.jwd.agent.model.ReactStep;
import com.jwd.agent.tools.ReactTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ReactAgentService {

    private final ChatModel reactChatModel;

    private final ReactTools reactTools;

    private final AgentProperties properties;

    public ReactChatResponse run(ReactChatRequest request) {
        AtomicInteger toolInvocationCount = new AtomicInteger(0);

        ReactAssistant assistant = AiServices.builder(ReactAssistant.class)
                .chatModel(reactChatModel)
                .tools(reactTools)
                .chatMemoryProvider(this::buildOrGetMemory)
                .maxSequentialToolsInvocations(properties.getReact().getMaxSteps())
                .afterToolExecution(toolExecution -> toolInvocationCount.incrementAndGet())
                .build();

        String answer = assistant.chat(request.getSessionId(), request.getQuestion());
        List<ReactStep> trajectory = buildLearningTrajectory(request.getQuestion(), answer);

        return ReactChatResponse.builder()
                .sessionId(request.getSessionId())
                .question(request.getQuestion())
                .answer(extractFinalAnswer(answer))
                .stepsUsed(toolInvocationCount.get())
                .maxSteps(properties.getReact().getMaxSteps())
                .timestamp(LocalDateTime.now())
                .trajectory(trajectory)
                .build();
    }

    private MessageWindowChatMemory buildOrGetMemory(Object memoryId) {
        return MessageWindowChatMemory.withMaxMessages(properties.getMemory().getMaxMessages());
    }

    private String extractFinalAnswer(String rawAnswer) {
        String marker = "FINAL_ANSWER:";
        int idx = rawAnswer.indexOf(marker);
        if (idx < 0) {
            return rawAnswer;
        }
        return rawAnswer.substring(idx + marker.length()).trim();
    }

    private List<ReactStep> buildLearningTrajectory(String question, String answer) {
        List<ReactStep> steps = new ArrayList<>();
        steps.add(ReactStep.builder()
                .index(1)
                .thought("理解问题并识别是否需要工具来获得精确结果")
                .action("analyze_question")
                .actionInput(question)
                .observation("检测到这是一个教学型ReAct请求，优先采用工具辅助回答")
                .build());
        steps.add(ReactStep.builder()
                .index(2)
                .thought("结合上下文决定是否调用价格、折扣、日期或学习建议工具")
                .action("tool_selection")
                .actionInput("queryPrice/discountPrice/applyFullReduction/today/learningTips")
                .observation("由模型在调用时自动选择最合适工具")
                .build());
        steps.add(ReactStep.builder()
                .index(3)
                .thought("整合工具观察结果并生成最终回答")
                .action("final_synthesis")
                .actionInput("将Observation转成面向用户的可执行结论")
                .observation(extractFinalAnswer(answer))
                .build());
        return steps;
    }
}
