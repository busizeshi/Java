package com.jwd.agent.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private final Llm llm = new Llm();

    private final Memory memory = new Memory();

    private final React react = new React();

    @Data
    public static class Llm {

        @NotBlank
        private String baseUrl;

        @NotBlank
        private String apiKey;

        @NotBlank
        private String modelName;

        @Min(0)
        @Max(2)
        private Double temperature = 0.7;

        @Min(5)
        @Max(300)
        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class Memory {

        @Min(6)
        @Max(200)
        private Integer maxMessages = 24;
    }

    @Data
    public static class React {

        @Min(1)
        @Max(32)
        private Integer maxSteps = 8;
    }
}
