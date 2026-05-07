package com.jwd.agent.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReactStep {

    private int index;

    private String thought;

    private String action;

    private String actionInput;

    private String observation;
}
