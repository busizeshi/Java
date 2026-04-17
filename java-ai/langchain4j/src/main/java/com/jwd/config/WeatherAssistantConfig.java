package com.jwd.config;

import com.jwd.service.WeatherAssistant;
import com.jwd.tools.WeatherTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherAssistantConfig {

    @Bean
    public WeatherAssistant weatherAssistant(@Qualifier("qwenTurboModel") ChatModel model, WeatherTools weatherTools) {
        return AiServices.builder(WeatherAssistant.class)
                .chatModel(model)
                .tools(weatherTools)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}
