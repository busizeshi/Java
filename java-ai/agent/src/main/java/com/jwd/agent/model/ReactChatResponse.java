package com.jwd.agent.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReactChatResponse {

    private String sessionId;

    private String question;

    private String answer;

    private Integer stepsUsed;

    private Integer maxSteps;

    private LocalDateTime timestamp;

    private List<ReactStep> trajectory;
}
