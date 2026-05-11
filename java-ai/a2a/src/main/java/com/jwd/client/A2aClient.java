package com.jwd.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class A2aClient {

    private final RestTemplate restTemplate;

    public A2aClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getAgentCard(String agentUrl) {
        String cardUrl = agentUrl + "/.well-known/agent.json";
        ResponseEntity<Map> response = restTemplate.getForEntity(cardUrl, Map.class);
        return response.getBody();
    }

    public Map<String, Object> sendTask(String agentUrl, String taskId,
                                        String sessionId, String skillId,
                                        List<Map<String, Object>> history) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", taskId);
        params.put("sessionId", sessionId);
        params.put("skillId", skillId);
        params.put("history", history);

        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/send",
                "params", params,
                "id", UUID.randomUUID().toString()
        );

        return postJsonRpc(agentUrl, rpcRequest);
    }

    public Map<String, Object> setPushNotification(String agentUrl, String taskId, String webhookUrl) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/pushNotification/set",
                "params", Map.of(
                        "id", taskId,
                        "webhookUrl", webhookUrl
                ),
                "id", UUID.randomUUID().toString()
        );
        return postJsonRpc(agentUrl, rpcRequest);
    }

    public Map<String, Object> getTask(String agentUrl, String taskId) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/get",
                "params", Map.of("id", taskId),
                "id", UUID.randomUUID().toString()
        );
        return postJsonRpc(agentUrl, rpcRequest);
    }

    public Map<String, Object> cancelTask(String agentUrl, String taskId) {
        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/cancel",
                "params", Map.of("id", taskId),
                "id", UUID.randomUUID().toString()
        );
        return postJsonRpc(agentUrl, rpcRequest);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postJsonRpc(String agentUrl, Map<String, Object> rpcRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(rpcRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(agentUrl + "/", entity, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new RuntimeException("Empty response");
        }
        if (body.containsKey("error")) {
            Map<String, Object> error = (Map<String, Object>) body.get("error");
            throw new RuntimeException("A2A error: " + error.get("message"));
        }

        return (Map<String, Object>) body.get("result");
    }
}
