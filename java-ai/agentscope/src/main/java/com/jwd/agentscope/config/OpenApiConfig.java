package com.jwd.agentscope.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI agentScopeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java-AI AgentScope 接口文档")
                        .version("v1")
                        .description("AgentScope 模块接口文档，用于导入 Apifox"))
                .tags(List.of(
                        new Tag().name("AgentScope 任务").description("AgentScope 任务提交与状态查询接口"),
                        new Tag().name("AgentScope 健康检查").description("AgentScope 模块健康检查接口"),
                        new Tag().name("AgentScope 多 Agent 编排").description("Sequential、Parallel、Loop 三种多 Agent 协作模式演示接口")
                ));
    }
}
