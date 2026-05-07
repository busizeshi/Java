# 文档四：A2A 流式任务与 SSE 实时推送（含代码映射）

## 1. 为什么需要流式任务

同步调用 `tasks/send` 的特点是“请求发出后，等待任务完成再返回”。

当任务耗时上升（如报告生成、复杂分析）时，会出现：

1. 客户端连接长时间等待
2. 用户无法感知进度
3. 超时与重试策略复杂化

因此在 A2A 场景中，耗时任务建议切换到 `tasks/sendSubscribe`，通过 SSE 实时推送任务状态与中间产物。

## 2. send 与 sendSubscribe 的调用差异

1. `tasks/send`
- 适用：短任务
- 交互：一次请求一次响应
- 返回：最终 Task

2. `tasks/sendSubscribe`
- 适用：长任务或需要进度可见的任务
- 交互：建立 SSE 流并持续推送事件
- 返回：状态事件 + 产物事件 + 最终结束事件

## 3. 流式事件模型

当前实现采用两类事件：

1. `task_status_update`
- 用于状态迁移与进度文案
- 常见状态：`submitted -> working -> completed/failed`

2. `task_artifact_update`
- 用于中间或最终产物推送
- 例如“销售报告正文”先于最终完成态到达

事件数据结构保持 JSON-RPC 外壳：

1. `jsonrpc`
2. `id`（请求 id）
3. `result`（task id、status/artifact、final）

## 4. Server 端实现：A2aStreamController

来源文件：[A2aStreamController.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\controller\A2aStreamController.java)

### 4.1 流式入口

```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter sendSubscribe(@RequestBody Map<String, Object> rpcRequest)
```

说明：

1. 独立 `POST /stream` 作为 sendSubscribe 入口
2. 返回 `SseEmitter` 建立 `text/event-stream`
3. 超时设置为 5 分钟：`new SseEmitter(300_000L)`

### 4.2 异步执行模型

```java
private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
executor.submit(() -> runStreamingTask(...));
```

说明：

1. 请求线程快速返回，避免阻塞 servlet 工作线程
2. 每个流式任务在虚拟线程中执行，适合 I/O 或等待型步骤

### 4.3 runStreamingTask 状态推进

实现逻辑：

1. 先推送 `submitted`
2. 再推送 `working`
3. 根据 `skillId` 找到处理器
4. 若处理器实现 `StreamingSkillHandler`，交给它分步推送
5. 否则走普通 Skill 执行并在末尾补推 `artifact + completed`

这个分支保证了“同一个流式入口可兼容流式 Skill 与非流式 Skill”。

### 4.4 事件发送方法

1. `sendStatusEvent(...)`：封装 `task_status_update`
2. `sendArtifactEvent(...)`：封装 `task_artifact_update`

`isFinal=true` 时主动 `emitter.complete()`，关闭流。

## 5. 流式 Skill 抽象：StreamingSkillHandler

来源文件：[StreamingSkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\skill\StreamingSkillHandler.java)

```java
public interface StreamingSkillHandler extends SkillHandler {
    void handleStreaming(String taskId, String sessionId,
                         List<Message> history, SseEmitter emitter, String requestId);
}
```

说明：

1. 继承 `SkillHandler`，保留同步降级能力
2. 新增 `handleStreaming`，允许 Skill 主动分步推送中间状态
3. Controller 只做协议编排，领域进度由 Skill 自己定义

## 6. 流式 Skill 实现：销售报告生成

来源文件：[SalesReportSkillHandler.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\handler\SalesReportSkillHandler.java)

### 6.1 skillId 与能力定位

```java
@Override
public String skillId() {
    return "generate-sales-report";
}
```

说明：

1. 该 skill 对应“长耗时报告生成”场景
2. 适合作为 sendSubscribe 演示能力

### 6.2 同步降级实现

```java
@Override
public Task handle(String taskId, String sessionId, List<Message> history)
```

说明：

1. 当通过 `tasks/send` 调用时仍可执行
2. 保证能力在同步/流式两种调用方式下都可用

### 6.3 流式分步推进

`handleStreaming(...)` 的分步逻辑：

1. 推送“正在获取销售数据”
2. 推送“正在分析数据趋势”
3. 推送“正在生成报告”
4. 推送 `task_artifact_update`（报告正文）
5. 推送 `completed` 并结束流

说明：

1. 中间状态可显著改善用户感知
2. 报告先作为 artifact 推送，再给 completed 终态，符合流式语义

### 6.4 失败路径

出现异常时调用 `pushFailed(...)`：

1. 推送 `state=failed`
2. `final=true`
3. 结束流

## 7. 流式客户端：StreamingA2aClient

来源文件：[StreamingA2aClient.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\client\StreamingA2aClient.java)

### 7.1 使用 WebClient 订阅 SSE

```java
webClient.post()
    .uri(agentUrl + "/stream")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(rpcRequest)
    .retrieve()
    .bodyToFlux(String.class)
    .subscribe(...)
```

说明：

1. `WebClient` 对流式响应支持更自然
2. `bodyToFlux` 按数据流逐条消费
3. 通过回调将事件交给上层

### 7.2 当前解析策略

当前按 `line.startsWith("data:")` 提取原始数据字符串并回调。

优点：

1. 实现简单
2. 可快速演示端到端链路

限制：

1. 未解析 `event:` 字段
2. 未把 data JSON 反序列化为结构化对象
3. 心跳/空行处理较弱

## 8. 流式转发接口：StreamingOrchestratorController

来源文件：[StreamingOrchestratorController.java](D:\jwd-dev\study\Java\java-ai\a2a\src\main\java\com\jwd\controller\StreamingOrchestratorController.java)

### 8.1 对外接口

```java
@GetMapping(value = "/stream-ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamAsk(@RequestParam String question, @RequestParam String skillId)
```

说明：

1. 浏览器或前端直接订阅该接口
2. 内部调用 `StreamingA2aClient.sendSubscribe`
3. 将收到的事件继续 `emitter.send(event)` 转发给调用方

### 8.2 调用参数

1. `question`：用户问题
2. `skillId`：目标 skill（当前由调用方指定）

后续可与编排器结合，实现“先路由再流式调用”。

## 9. 测试方式（按当前代码）

当前服务端流式入口为 `POST /stream`，可直接用 curl 测试：

```bash
curl -N -X POST http://localhost:8080/stream \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0",
    "method":"tasks/sendSubscribe",
    "params":{
      "id":"task-stream-001",
      "sessionId":"session-001",
      "skillId":"generate-sales-report",
      "history":[{"role":"user","parts":[{"type":"text","text":"生成本月销售分析报告"}]}]
    },
    "id":"req-001"
  }'
```

预期可见多条 SSE 事件：

1. `task_status_update`（submitted）
2. `task_status_update`（working，步骤文案）
3. `task_artifact_update`（报告内容）
4. `task_status_update`（completed，final=true）

也可通过编排层流式转发接口：

```bash
curl -N "http://localhost:8080/api/orchestrator/stream-ask?question=生成本月销售分析报告&skillId=generate-sales-report"
```

## 10. 与 Agent Card 的一致性检查

当前 `AgentCardController` 中 `capabilities.streaming` 仍为 `false`，但流式能力实际上已经实现。

建议同步调整为：

1. `streaming: true`
2. 在 `skills` 中补充 `generate-sales-report`
3. 增加 examples，明确该技能适用于长任务报告生成

这样编排器才能通过 Card 自动发现并优先选择流式能力。

## 11. 代码实现的优点与可优化点

## 11.1 已实现的关键优点

1. 完整打通 sendSubscribe 的 server/client 端链路
2. 支持流式 Skill 的中间步骤推送
3. 同一 Skill 同时支持同步与流式两种调用
4. 使用虚拟线程减少阻塞压力

## 11.2 建议优先优化

1. 统一事件协议：`event` 与 `data` 结构做强类型封装
2. `StreamingA2aClient` 使用结构化 SSE 解码（而非仅字符串行）
3. 为流式任务落库状态迁移（submitted/working/completed/failed）
4. 增加断连处理与取消机制（结合 `tasks/cancel`）
5. 将目标 Agent URL 配置化，避免硬编码 `http://localhost:8080`

## 12. 本文档知识要点小结

1. `tasks/sendSubscribe` 解决长任务“不可见等待”的体验问题
2. SSE 推送分为状态事件与产物事件，二者缺一不可
3. `StreamingSkillHandler` 让领域能力拥有“可解释进度”
4. 流式能力上线后，需要同步更新 Agent Card 以保证可发现性
5. 当前实现已具备教学与 PoC 价值，进一步可向企业级稳定性演进
