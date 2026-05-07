# 文档二：Agent Card 与 Skill 设计方法论（含代码映射）

## 1. 设计目标

在 A2A 体系中，`Agent Card + Skill` 决定了三件关键事情：

1. 能否被发现
2. 能否被正确路由
3. 能否被稳定调用

因此，设计重点不是“字段填满”，而是“让编排 Agent 能准确理解能力边界并做低歧义决策”。

## 2. Agent Card 设计原则

## 2.1 基础字段是身份信息，description 是路由信息

Agent Card 中的 `name/url/version` 解决“是谁、在哪、哪个版本”；
`description` 解决“什么场景该调用该 Agent”。

好的 `description` 应满足：

1. 明确业务域
2. 明确可完成动作
3. 明确不覆盖的边界（可选）

示例：

1. 好：专门处理销售数据分析任务，支持报表汇总与异常诊断
2. 差：处理各种企业问题

## 2.2 capabilities 直接影响客户端调用策略

常见能力声明：

1. `streaming`：是否支持 `tasks/sendSubscribe`
2. `pushNotifications`：是否支持 Webhook 回调
3. `stateTransitionHistory`：是否提供状态迁移历史（若实现）

设计建议：

1. 30 秒内任务，优先同步 + 可选流式
2. 分钟级任务，建议开启 push 或轮询兜底

## 2.3 input/output modes 约束交互契约

`defaultInputModes/defaultOutputModes` 与每个 Skill 的 `inputModes/outputModes` 一起构成“可接收什么、可产出什么”的契约。

建议：

1. 默认值保守（如 text）
2. Skill 级覆盖更精细（text/data/file）

## 3. Skill 设计原则

## 3.1 粒度：比 Tool 粗，但不能“万能”

Skill 粒度应满足“完成一个完整业务动作”，典型粒度：

1. 销售汇总查询
2. 销售异常诊断
3. 库存告警分析

不建议：

1. 过粗：`handle-all-business`
2. 过细：`analyze-sales-anomaly-yesterday-14-16`

## 3.2 description 必须包含“何时调用”

Skill description 建议模板：

1. 处理对象
2. 触发场景
3. 结果类型

模板示例：

`分析指定时间段销售波动，识别异常与原因，输出处置建议与结构化严重级别。`

## 3.3 examples 是消歧核心

当多个 Skill 都属于同一领域时，examples 用于定义自然语言触发器。

设计建议：

1. 句式覆盖口语化提问
2. 覆盖同义表达
3. 覆盖边界表达（如“先查汇总再分析异常”）

## 3.4 tags 用于候选预过滤

大型多 Agent 场景中，先按 tags 缩小候选，再交给 LLM 决策，可降低 token 成本与误路由概率。

## 4. 多 Skill Agent 的拆分方法

推荐按“业务上下文一致性”拆分：

1. 同数据源、同业务术语、同目标用户的能力可放同 Agent
2. 数据域差异大、执行链路差异大的能力应拆分为不同 Agent

判断标准：

1. 一个 Agent 能否用一句话清晰定义
2. Skill 之间是否共享大部分上下文
3. 编排器是否容易误判

## 5. 代码映射：Agent Card 实现

来源文件：[AgentCardController.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\controller\AgentCardController.java)

```java
@GetMapping("/.well-known/agent.json")
public Map<String, Object> agentCard() {
    return Map.of(
        "name", "销售数据分析 Agent",
        "description", "专门处理销售数据分析任务，能生成报表、识别异常、预测趋势",
        "url", "http://localhost:8080",
        "version", "1.0.0",
        "capabilities", Map.of(
            "streaming", false,
            "pushNotifications", false
        ),
        "defaultInputModes", List.of("text"),
        "defaultOutputModes", List.of("text", "data"),
        "skills", buildSkills()
    );
}
```

说明：

1. 固定路径正确，满足 A2A 发现入口
2. description 已明确为“销售分析”领域
3. capabilities 当前为同步模式，适合最小可用实现

## 6. 代码映射：Skill 声明结构

同文件 `buildSkills()` 中声明了两项技能：

1. `query-sales-summary`
2. `analyze-sales-anomaly`

说明：

1. 两个 Skill 属于同一领域，拆分粒度合理
2. 均包含 tags + examples，利于编排器消歧
3. 均声明 output 支持 `text/data`，与产物结构一致

## 7. 代码映射：Skill 执行抽象层

来源文件：[SkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\handler\SkillHandler.java)

```java
public interface SkillHandler {
    String skillId();
    Task handle(String taskId, String sessionId, List<Message> history);
}
```

说明：

1. `skillId()` 与 Agent Card 中 skill id 对齐，形成路由主键
2. `handle(...)` 统一输入输出契约，降低 Controller 耦合
3. 新增 Skill 时只需新增实现类并注册为 Bean

## 8. 代码映射：Skill 实现一（销售汇总）

来源文件：[SalesQuerySkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\skill\SalesQuerySkillHandler.java)

关键实现点：

1. 通过 `defaultSystem` 约束回答风格与指标重点
2. 从 history 中提取最后一条 user 文本作为查询意图
3. 拼装模拟销售数据并调用 ChatClient
4. 输出 `Task(COMPLETED)` + `Artifact(text)`

说明：

1. 该 Skill 语义是“汇总解释”，不承担异常诊断
2. 该边界与 `analyze-sales-anomaly` 形成互补而非重叠

## 9. 代码映射：Skill 实现二（异常诊断）

来源文件：[SalesAnomalySkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\skill\SalesAnomalySkillHandler.java)

关键实现点：

1. system prompt 明确输出结构：异常描述/原因/措施
2. 构造异常样本数据（销售额、转化率、支付失败率）
3. 返回 `Artifact(text + data)`，其中 data 含 `severity` 等字段

说明：

1. 该 Skill 输出同时服务“人读”和“程序消费”
2. 是更接近企业化交付的写法

## 10. 代码映射：注册表如何消费 Card 与 Skill

来源文件：[AgentRegistry.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\repository\AgentRegistry.java)

关键实现：

1. `register(agentUrl)` 拉取 Agent Card 并缓存
2. `buildAgentsSummary()` 组织 description + skills 文本给 LLM

说明：

1. Agent Card 设计质量会直接影响路由提示词质量
2. Skill 描述越清晰，路由越稳定

## 11. 设计反例与修正建议

## 11.1 反例：Skill 语义重叠

问题：两个 Skill 都写成“分析销售数据”，编排器难以区分。

修正：

1. 一个强调“汇总查询”
2. 一个强调“异常识别与处置”
3. examples 分别覆盖不同问句

## 11.2 反例：description 过宽

问题：description 写成“处理各种数据”。

修正：

1. 限定领域（销售）
2. 限定动作（汇总、异常）
3. 限定输出（文本+结构化）

## 11.3 反例：只返回 text

问题：后续系统无法自动判断严重等级或生成告警。

修正：

1. 在 Artifact 中增加 `Part.data`
2. 固定字段名（如 severity、period、reasonCodes）

## 12. 可直接复用的设计模板

## 12.1 Agent Card description 模板

`专注于【业务域】任务，支持【动作1】【动作2】，输出【输出形式】，适用于【典型场景】。`

## 12.2 Skill description 模板

`处理【输入对象】在【时间/范围】内的【分析动作】，输出【结果结构】与【建议动作】。`

## 12.3 examples 模板

1. `帮我看下本周【指标】变化`
2. `昨天【时段】为什么下降`
3. `最近三天是否有【异常类型】`

## 13. 本文档知识要点小结

1. Agent Card 是路由入口，Skill 是路由锚点
2. description/examples/tags 三者共同决定“可发现性与可路由性”
3. Skill 粒度要“完整业务动作级”，避免过粗或过细
4. `text + data` 双输出是更实用的工程实践
5. 设计质量最终体现在编排成功率、误路由率与维护成本上
