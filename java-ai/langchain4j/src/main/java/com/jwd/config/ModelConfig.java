package com.jwd.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

    @Bean("qwenMaxModel")
    public ChatModel qwenMaxModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-51b422ad7151406b8c3ddb1ce0a424ba")
                .modelName("qwen-max")
                .build();

    }

    @Bean("doubaoModel")
    public ChatModel doubaoModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://ark.cn-beijing.volces.com/api/v3")
                .apiKey("8eb68d09-d0c8-4dc3-a947-1ce0c5ed390b") // 替换为你的豆包API Key
                .modelName("doubao-pro-32k") // 或其他豆包模型，如 doubao-lite-32k
                .build();
    }

    @Bean("qwenTurboModel")
    public ChatModel qwenTurboModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-51b422ad7151406b8c3ddb1ce0a424ba")
                .modelName("qwen-turbo")
                .build();
    }

    @Bean("qwenStreamingModel")
    public StreamingChatModel qwenStreamingModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-51b422ad7151406b8c3ddb1ce0a424ba")
                .modelName("qwen-max")
                .build();
    }

}
