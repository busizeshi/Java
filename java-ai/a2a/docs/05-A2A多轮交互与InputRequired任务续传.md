# 文档五：A2A 多轮交互（input-required）与任务续传实战

## 1. 本节目标

本节把 `generate-sales-report` 从“一次请求一次完成”升级为“信息不全先追问、补齐后继续执行”的多轮任务。

核心点：

1. Agent 在信息不足时返回 `input-required`
2. Client 使用**同一个 `taskId`** 追加历史消息再次调用 `tasks/send`
3. Server 识别到该 `taskId` 处于 `input-required` 后走续传分支，最终完成任务

## 2. 任务状态机变化

新增的关键流转：

1. `submitted -> input-required`
2. `input-required -> completed`
3. 若补充信息仍不完整，可继续 `input-required -> input-required`

说明：`input-required` 状态下，`Task.status.message` 会带上 Agent 的追问文本。

## 3. Server 端改造

## 3.1 ReportGenerationSkillHandler：支持追问与续传

来源文件：[ReportGenerationSkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\skill\ReportGenerationSkillHandler.java)

实现要点：

1. `handle(...)` 中先抽取参数（开始日期、结束日期、地区）
2. 参数不全时返回 `TaskState.INPUT_REQUIRED`，并附追问消息
3. 参数齐全时返回 `TaskState.COMPLETED` + 报告 Artifact
4. 新增 `continueTask(...)`：用于同 `taskId` 的续传执行

当前解析支持示例表达：

1. `2025年1月1日到2025年1月31日`
2. `2025年1月1日到1月31日`

## 3.2 A2aController：升级 tasks/send 续传分支

来源文件：[A2aController.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\controller\A2aController.java)

`handleTaskSend(...)` 新逻辑：

1. 先查 `taskRepository.findById(taskId)`
2. 若已有任务且状态是 `input-required`，调用 `ReportGenerationSkillHandler.continueTask(...)`
3. 否则按首次调用流程：先存 `submitted`，再执行 `handle(...)`

这样可以保证：同 `taskId` 会被识别为同一任务链路，而不是新任务。

## 3.3 冲突处理：避免 skillId 重复注册覆盖

来源文件：[SalesReportSkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\handler\SalesReportSkillHandler.java)

工程中已有另一个 `generate-sales-report` 的 handler。为避免 Map 注册覆盖，本次移除了它的 `@Component`，只保留新的 `ReportGenerationSkillHandler` 生效。

## 4. Client 端改造

来源文件：[OrchestratorAgent.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\client\OrchestratorAgent.java)

新增处理策略：

1. 第一次 `tasks/send`
2. 若返回 `input-required`，提取 Agent 追问
3. 将 `agent` 追问与 `user` 补充信息都追加到 `history`
4. 用同一 `taskId/sessionId` 再次调用 `tasks/send`
5. 直到返回非 `input-required`（本实现有轮次保护）

说明：当前示例里“用户补充信息”由 LLM 模拟生成，用于演示自动续传链路；真实产品里应替换为前端用户输入。

## 5. Agent Card

当前 `AgentCardController` 中已声明：

1. `skillId = generate-sales-report`
2. 描述中包含“信息不全会主动追问”

这有助于编排器正确理解该 Skill 的多轮行为。

## 6. curl 两轮调用示例

## 6.1 第一轮：发起任务（信息不完整）

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tasks/send",
    "params": {
      "id": "task-multi-001",
      "sessionId": "session-001",
      "skillId": "generate-sales-report",
      "history": [
        {"role": "user", "parts": [{"type": "text", "text": "帮我生成一份销售报告"}]}
      ]
    },
    "id": "req-001"
  }'
```

预期：`status.state = input-required`，并返回追问消息。

## 6.2 第二轮：补充信息并续传（taskId 不变）

```bash
curl -X POST http://localhost:8080/ \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tasks/send",
    "params": {
      "id": "task-multi-001",
      "sessionId": "session-001",
      "skillId": "generate-sales-report",
      "history": [
        {"role": "user",  "parts": [{"type": "text", "text": "帮我生成一份销售报告"}]},
        {"role": "agent", "parts": [{"type": "text", "text": "需要补充以下信息才能生成报告：开始日期、结束日期、地区（可选）"}]},
        {"role": "user",  "parts": [{"type": "text", "text": "2025年1月1日到1月31日，华东地区"}]}
      ]
    },
    "id": "req-002"
  }'
```

预期：`status.state = completed`，并在 `artifacts` 中返回“销售报告”。

## 7. 关键坑位

1. 第二次请求必须复用同一个 `taskId`
2. 第二次请求 `history` 必须带完整上下文，不是只传新增回答
3. 同 `skillId` 的多个 `@Component` 会互相覆盖
4. 源文件编码建议统一 UTF-8（无 BOM），避免编译器识别异常字符

## 8. 本地验证结果

已执行：

```bash
mvn -DskipTests compile
```

结果：`BUILD SUCCESS`
