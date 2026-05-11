package com.jwd.client;

import com.jwd.repository.AgentRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrchestratorAgent {

    private final ChatClient chatClient;
    private final AgentRegistry agentRegistry;
    private final A2aClient a2aClient;

    public OrchestratorAgent(
            ChatClient.Builder builder,
            AgentRegistry agentRegistry,
            A2aClient a2aClient) {
        this.agentRegistry = agentRegistry;
        this.a2aClient = a2aClient;
        this.chatClient = builder.build();
    }

    public String handle(String userRequest) {
        String agentsSummary = agentRegistry.buildAgentsSummary();

        String routingDecision = chatClient.prompt()
                .system("""
                        你是任务路由器。根据用户请求和可用 Agent 列表，
                        决定应该调用哪个 Agent 的哪个 Skill。

                        只输出 JSON，格式：
                        {"agentName": "Agent名称", "skillId": "skill-id", "reason": "选择原因"}

                        如果没有合适的 Agent，输出：
                        {"agentName": null, "skillId": null, "reason": "原因"}
                        """)
                .user("可用 Agent：\n" + agentsSummary + "\n\n用户请求：" + userRequest)
                .call()
                .content();

        RoutingDecision decision = parseDecision(routingDecision);
        if (decision.agentName() == null) {
            return "抱歉，没有找到能处理这个请求的 Agent：" + decision.reason();
        }

        AgentRegistry.AgentInfo agentInfo = agentRegistry.getAgent(decision.agentName());
        if (agentInfo == null) {
            return "Agent 不存在：" + decision.agentName();
        }

        String taskId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        List<Map<String, Object>> history = new ArrayList<>();
        history.add(Map.of("role", "user",
                "parts", List.of(Map.of("type", "text", "text", userRequest))));

        Map<String, Object> taskResult = a2aClient.sendTask(
                agentInfo.url(), taskId, sessionId, decision.skillId(), history);

        return continueIfInputRequired(agentInfo.url(), taskId, sessionId, decision.skillId(), history, taskResult);
    }

    @SuppressWarnings("unchecked")
    private String continueIfInputRequired(String agentUrl,
                                           String taskId,
                                           String sessionId,
                                           String skillId,
                                           List<Map<String, Object>> history,
                                           Map<String, Object> taskResult) {
        Map<String, Object> current = taskResult;
        int guard = 0;

        while (guard++ < 3) {
            Map<String, Object> status = (Map<String, Object>) current.get("status");
            String state = status == null ? null : (String) status.get("state");
            if (!"input-required".equals(state)) {
                return extractResultText(current);
            }

            Map<String, Object> message = (Map<String, Object>) status.get("message");
            String question = extractTextFromMessage(message);
            if (question == null || question.isBlank()) {
                return "任务进入 input-required，但未返回可读提问。";
            }

            String answer = askUserForMissingInfo(question);
            history.add(Map.of("role", "agent",
                    "parts", List.of(Map.of("type", "text", "text", question))));
            history.add(Map.of("role", "user",
                    "parts", List.of(Map.of("type", "text", "text", answer))));

            current = a2aClient.sendTask(agentUrl, taskId, sessionId, skillId, history);
        }

        return "任务多轮补充超过 3 次，已停止。";
    }

    private String askUserForMissingInfo(String question) {
        return chatClient.prompt()
                .system("你要模拟最终用户，针对 Agent 的澄清问题给出简洁且完整的补充信息。"
                        + "如果问题要求时间范围和地区，默认给出：2025年1月1日到2025年1月31日，华东地区。")
                .user(question)
                .call()
                .content();
    }

    @SuppressWarnings("unchecked")
    private String extractResultText(Map<String, Object> task) {
        Map<String, Object> status = (Map<String, Object>) task.get("status");
        if (status == null) {
            return "任务无状态信息";
        }

        Map<String, Object> message = (Map<String, Object>) status.get("message");
        String text = extractTextFromMessage(message);
        if (text != null && !text.isBlank()) {
            return text;
        }

        List<Map<String, Object>> artifacts = (List<Map<String, Object>>) task.get("artifacts");
        if (artifacts != null && !artifacts.isEmpty()) {
            List<Map<String, Object>> parts = (List<Map<String, Object>>) artifacts.get(0).get("parts");
            if (parts != null) {
                return parts.stream()
                        .filter(p -> "text".equals(p.get("type")))
                        .map(p -> (String) p.get("text"))
                        .filter(t -> t != null && !t.isBlank())
                        .findFirst()
                        .orElse("任务完成，但无文本输出");
            }
        }

        return "任务完成，但无文本输出";
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromMessage(Map<String, Object> message) {
        if (message == null) {
            return null;
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) message.get("parts");
        if (parts == null) {
            return null;
        }
        return parts.stream()
                .filter(p -> "text".equals(p.get("type")))
                .map(p -> (String) p.get("text"))
                .findFirst()
                .orElse(null);
    }

    private RoutingDecision parseDecision(String json) {
        json = json.trim().replaceAll("```json|```", "").trim();
        try {
            boolean hasAgent = json.contains("\"agentName\": \"") && !json.contains("\"agentName\": null");
            if (!hasAgent) {
                return new RoutingDecision(null, null, "未找到合适 Agent");
            }

            String agentName = extractJsonField(json, "agentName");
            String skillId = extractJsonField(json, "skillId");
            String reason = extractJsonField(json, "reason");
            return new RoutingDecision(agentName, skillId, reason);
        } catch (Exception e) {
            return new RoutingDecision(null, null, "解析失败");
        }
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\": \"";
        int start = json.indexOf(key) + key.length();
        int end = json.indexOf("\"", start);
        return start > key.length() - 1 && end > start ? json.substring(start, end) : null;
    }

    record RoutingDecision(String agentName, String skillId, String reason) {
    }
}
