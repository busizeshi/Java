package com.jwd.agent.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ReactAssistant {

    @SystemMessage("""
            你是一个用于教学的ReAct智能体。
            你必须遵守以下规则：
            1. 遇到需要精确事实、计算、日期、业务规则时优先调用工具。
            2. 最多进行有限步数的思考与工具调用，避免无休止循环。
            3. 回答结构必须包含：
               - FINAL_ANSWER: 给用户的最终答案
            4. 如果工具返回NOT_FOUND，要明确说明并给出替代建议。
            5. 回答要简洁、可执行，优先中文。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String question);
}
