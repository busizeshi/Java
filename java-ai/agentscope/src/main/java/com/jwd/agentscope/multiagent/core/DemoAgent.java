package com.jwd.agentscope.multiagent.core;

import com.jwd.agentscope.multiagent.model.AgentContext;
import com.jwd.agentscope.multiagent.model.AgentResult;

public interface DemoAgent {

    String name();

    AgentResult execute(AgentContext context);
}
