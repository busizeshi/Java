package com.jwd.service;

import com.jwd.model.UserRole;
import com.jwd.tools.AdminTools;
import com.jwd.tools.ModifyTools;
import com.jwd.tools.QueryTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DynamicAgentService {

    private final ChatModel chatModel;

    private final QueryTools queryTools;

    private final ModifyTools modifyTools;

    private final AdminTools adminTools;


    public DynamicAgentService(@Qualifier("qwenTurboModel") ChatModel chatModel, QueryTools queryTools, ModifyTools modifyTools, AdminTools adminTools) {
        this.chatModel = chatModel;
        this.queryTools = queryTools;
        this.modifyTools = modifyTools;
        this.adminTools = adminTools;
    }

    public String chat(String sessionId, UserRole role, String message) {
        List<Object> tools = buildToolSet(role);

        // 每次请求现场 build，用完即丢，绝不缓存
        // 注意：接口方法有 @MemoryId，必须用 chatMemoryProvider 而不是 chatMemory；
        // 用 chatMemory 时 LangChain4j 会走 provider 路径并用 memoryId 查找，
        // 找不到对应实例就抛 NullPointerException。
        DynamicAssistant agent = AiServices.builder(DynamicAssistant.class)
                .chatModel(chatModel)
                .tools(tools.toArray())
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        return agent.chat(sessionId, message);
    }

    private List<Object> buildToolSet(UserRole role) {
        List<Object> tools = new ArrayList<>();
        tools.add(unwrap(queryTools));

        if (role == UserRole.MEMBER || role == UserRole.ADMIN) {
            tools.add(unwrap(modifyTools));
        }
        if (role == UserRole.ADMIN) {
            tools.add(unwrap(adminTools));
        }

        return tools;
    }

    private Object unwrap(Object bean) {
        try {
            return AopUtils.isAopProxy(bean)
                    ? ((Advised) bean).getTargetSource().getTarget()
                    : bean;
        } catch (Exception e) {
            return bean;
        }
    }

}
