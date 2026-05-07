package com.jwd.agent.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfig {

    @Bean
    public ChatModel reactChatModel(AgentProperties properties) {
        return OpenAiChatModel.builder()
                .baseUrl(properties.getLlm().getBaseUrl())
                .apiKey(properties.getLlm().getApiKey())
                .modelName(properties.getLlm().getModelName())
                .temperature(properties.getLlm().getTemperature())
                .timeout(Duration.ofSeconds(properties.getLlm().getTimeoutSeconds()))
                .build();
    }
}
