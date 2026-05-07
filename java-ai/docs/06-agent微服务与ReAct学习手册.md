# LangChain4j ReAct Agent 微服务学习手册

> 本文基于本项目新增的 `agent` 微服务，系统讲解如何在 Java + Spring Boot + LangChain4j 中实现一个可运行、可扩展、可教学的 ReAct Agent。

---

## 一、什么是 ReAct，为什么要学

ReAct = **Reasoning + Acting**，核心思想是让模型在回答问题时进行循环：

1. `Thought`：思考下一步要做什么
2. `Action`：调用工具获取外部信息或执行动作
3. `Observation`：观察工具返回结果
4. 继续下一轮，直到产出 `Final Answer`

和纯聊天模型相比，ReAct 的优势：

1. 能处理需要“外部事实”的问题（如价格、日期、规则）
2. 能做可解释推理（你可以看到它如何一步步得到结果）
3. 更容易落地为可控业务 Agent（工具权限、步骤上限、输出格式）

---

## 二、本项目 Agent 微服务结构

本次新增模块：`agent`

核心文件：

1. `agent/pom.xml`：模块依赖与构建
2. `AgentApplication.java`：微服务入口
3. `config/AgentProperties.java`：配置绑定
4. `config/AgentConfig.java`：LLM Bean 构建
5. `tools/ReactTools.java`：可被 Agent 调用的工具集合
6. `service/ReactAssistant.java`：AI Service 接口（系统提示词）
7. `service/ReactAgentService.java`：Agent 编排与响应组装
8. `controller/ReactAgentController.java`：REST API
9. `resources/application.yml`：服务配置

---

## 三、依赖与配置说明

### 3.1 `agent/pom.xml`

关键依赖：

1. `langchain4j-spring-boot-starter`
2. `langchain4j-open-ai-spring-boot-starter`
3. `spring-boot-starter-validation`

作用说明：

1. `langchain4j-spring-boot-starter` 提供 AI Service、工具调用等基础能力
2. `open-ai-spring-boot-starter` 让你可以用 OpenAI 兼容协议访问 Qwen/Doubao 等模型
3. `validation` 用于请求参数校验，避免接口脏数据

### 3.2 `application.yml`

```yaml
server:
  port: 8082

langchain4j:
  open-ai:
    chat-model:
      enabled: false

agent:
  llm:
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${QWEN_API_KEY:replace-me}
    model-name: qwen-turbo
    temperature: 0.2
    timeout-seconds: 60
  memory:
    max-messages: 24
  react:
    max-steps: 8
```

设计要点：

1. 把模型参数集中放在 `agent.llm`，方便切模型、切平台
2. `api-key` 用环境变量读取，不把真实密钥硬编码进代码
3. `max-messages` 控制短期记忆窗口大小
4. `max-steps` 预留给 ReAct 循环上限控制（避免无限推理）
5. 在代码中通过 `AiServices.builder(...).maxSequentialToolsInvocations(maxSteps)` 真正生效

---

## 四、核心代码逐层拆解

## 4.1 配置层：`AgentProperties` + `AgentConfig`

`AgentProperties` 负责强类型配置绑定，并用 `@Validated` + `@Min/@Max/@NotBlank` 做参数约束。

`AgentConfig` 中构建 `ChatModel`：

1. `baseUrl`：OpenAI 兼容网关地址
2. `apiKey`：鉴权
3. `modelName`：模型名称
4. `temperature`：控制随机性
5. `timeout`：请求超时

这一步解决的是：让模型接入标准化、可替换、可审计。

## 4.2 工具层：`ReactTools`

本 demo 提供了 5 类工具，覆盖 ReAct 的典型场景：

1. `queryPrice(productName)`：事实查询
2. `discountPrice(originPrice, discountPercent)`：数值计算
3. `applyFullReduction(originPrice, threshold, reduction)`：业务规则判断
4. `today()`：外部时间
5. `learningTips(topic)`：知识辅助

工具设计原则：

1. 工具输入参数要结构化（不要一个大字符串）
2. 工具返回要短小明确（避免模型二次误解）
3. 对不存在数据要有明确返回（如 `NOT_FOUND`）

## 4.3 AI 服务接口：`ReactAssistant`

通过 `@SystemMessage` 约束 Agent 行为：

1. 什么时候优先调用工具
2. 必须产出 `FINAL_ANSWER`
3. 工具失败时如何降级
4. 输出语言和风格

本质上这是一份“Agent 宪法”。提示词写得越清晰，Agent 越稳定。

## 4.4 编排层：`ReactAgentService`

关键流程：

1. 用 `AiServices.builder()` 构造 Assistant
2. 绑定模型、工具、会话记忆
3. 调用 `assistant.chat(sessionId, question)`
4. 解析最终答案并构造响应对象
5. 附加学习轨迹 `trajectory`
6. 通过 `maxSequentialToolsInvocations` 强制限制连续工具调用次数
7. 通过 `afterToolExecution` 统计本次真实工具调用次数（返回给 `stepsUsed`）

`trajectory` 字段是这个教学 demo 的重点：它把 ReAct 关键阶段可视化，便于你学习和调试。
`stepsUsed` 是本次请求里实际发生的工具调用次数，不是固定模板步数。

## 4.5 接口层：`ReactAgentController`

暴露接口：

- `POST /agent/react/chat`

请求体：

```json
{
  "sessionId": "s-1001",
  "question": "Java并发编程实战打8折后多少钱"
}
```

返回体包含：

1. 最终答案 `answer`
2. 步骤统计 `stepsUsed/maxSteps`
3. 学习轨迹 `trajectory`
4. 时间戳 `timestamp`

---

## 五、运行与验证

### 5.1 编译

在项目根目录执行：

```bash
mvn -pl agent -am -DskipTests compile
```

### 5.2 启动

```bash
mvn -pl agent spring-boot:run
```

启动前请先设置环境变量 `QWEN_API_KEY`。

### 5.3 调用示例

```bash
curl -X POST "http://localhost:8082/agent/react/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId":"demo-1",
    "question":"LangChain4j实战打9折再满100减20，最终多少钱？"
  }'
```

你会看到：

1. Agent 调用工具做价格与规则计算
2. 返回 `FINAL_ANSWER` 语义的最终结果
3. 附带可学习的 `trajectory`

---

## 六、把这个 Demo 升级成“生产可用 ReAct”的路径

## 6.1 记忆体系升级

当前是 `MessageWindowChatMemory`（短期窗口记忆），下一步可以：

1. 接入 Redis 持久化 memory store
2. 区分会话短期记忆与长期知识库
3. 做 memory 清理策略（TTL、大小上限）

## 6.2 工具治理升级

1. 工具分级：只读工具 / 写操作工具 / 高风险工具
2. 参数校验：对金额、日期、ID 做强校验
3. 审计日志：记录 `谁在什么时候调用了什么工具`
4. 超时熔断：工具慢或失败时快速降级

## 6.3 ReAct 控制面升级

1. 强制 `maxSteps` 终止条件
2. Tool 调用失败重试策略
3. Prompt 模板版本化
4. 注入可观测埋点（token、耗时、成功率）

## 6.4 安全与合规

1. API Key 全部走环境变量/密钥管理系统
2. 不在日志中打印敏感信息
3. 对用户输入做注入防护（Prompt Injection 基础防线）
4. 对高风险工具增加人工审批或双重确认

---

## 七、常见问题（FAQ）

### 7.1 为什么 Agent 不调用工具？

常见原因：

1. 系统提示词没有明确“优先调用工具”
2. 工具描述不清晰，模型不知道何时调用
3. 问题本身不需要外部事实

排查建议：

1. 强化 `@SystemMessage` 规则
2. 优化 `@Tool` 描述与参数命名
3. 观察返回结果是否出现 `FINAL_ANSWER` 前的工具痕迹

### 7.2 为什么回答不稳定？

1. 温度过高导致随机性大
2. 工具返回内容过长或歧义
3. 记忆窗口过小导致上下文丢失

建议：

1. 把 `temperature` 调低（如 0.1~0.3）
2. 工具返回固定格式
3. 适当增大 `max-messages`

### 7.3 如何支持多模型切换？

你可以在 `AgentConfig` 中定义多个 `ChatModel` Bean，再通过配置选择注入哪个模型。

### 7.4 为什么要返回 `trajectory`？

教学与调试价值很高：

1. 便于理解 ReAct 的每个阶段
2. 便于定位失败点（是思考错、选错工具，还是工具结果异常）
3. 便于后续做可观测和评估

---

## 八、学习路线建议（基于本 Demo）

建议按以下顺序实践：

1. 跑通接口，观察基础 ReAct 行为
2. 自己新增一个工具（如汇率换算）
3. 让 Agent 在一个问题中组合调用多个工具
4. 引入 Redis 持久化会话记忆
5. 增加步骤上限与失败重试
6. 做一次完整的“工具权限分层”

做到第 6 步后，你就具备了从 Demo 到业务 Agent 的核心能力。

---

## 九、总结速查

| 主题 | 本项目实现位置 | 关键点 |
|------|---------------|--------|
| 模型接入 | `AgentConfig` | OpenAI 兼容协议、可配置化 |
| ReAct规则 | `ReactAssistant` | SystemMessage 约束行为 |
| 工具系统 | `ReactTools` | `@Tool` 声明式调用 |
| 会话记忆 | `ReactAgentService` | `chatMemoryProvider` |
| API接口 | `ReactAgentController` | REST 化对外服务 |
| 教学可视化 | `ReactChatResponse.trajectory` | 可解释执行轨迹 |

如果你后续想继续深入，我建议下一步把“轨迹”从静态教学信息升级为真实的 Tool 执行日志（含每次 action 参数、返回值、耗时），再配合评测集做 Agent 回归测试。
