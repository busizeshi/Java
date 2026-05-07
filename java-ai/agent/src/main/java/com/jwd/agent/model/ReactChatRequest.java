package com.jwd.agent.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReactChatRequest {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String question;
}
