# 06 A2A Push Notification 与 Webhook 异步回调

## 1. 为什么要用 Push Notification
- 同步 `tasks/send` 适合短任务，客户端要一直等待。
- SSE `tasks/sendSubscribe` 适合几秒到几分钟任务，客户端保持长连接。
- 长任务（十几分钟到几小时）更适合 Webhook：客户端先注册回调地址，服务端完成后主动通知。

## 2. 本章目标
- 服务端把 `tasks/send` 改造成异步提交：先返回 `submitted`。
- 新增 `tasks/pushNotification/set`：为 `taskId` 注册 Webhook URL。
- 任务完成后服务端主动 `POST` 到 Webhook。
- 客户端提供回调接口并等待结果。

## 3. 关键代码改造
### 3.1 Webhook 注册表
文件：`com.jwd.repository.WebhookRegistry`
- 用 `ConcurrentHashMap<taskId, webhookUrl>` 保存注册信息。
- 提供 `register` 与 `find` 两个方法。

### 3.2 异步执行器
文件：`com.jwd.repository.AsyncTaskExecutor`
- 新建虚拟线程执行 `handler.handle(...)`。
- 执行后把最终 `Task` 写回 `TaskRepository`。
- 如果有 webhook，构造 JSON-RPC 回调请求推送到客户端：
  - `method = tasks/pushNotification`
  - `params.id = taskId`
  - `params.task = finalTask`

### 3.3 A2aController 接入
文件：`com.jwd.controller.A2aController`
- 注入 `AsyncTaskExecutor` 与 `WebhookRegistry`。
- `tasks/send`：保存 `SUBMITTED` 后立即返回；后台异步执行。
- 新增 `tasks/pushNotification/set` 分支，注册回调地址。
- `input-required` 续传逻辑保留（`ReportGenerationSkillHandler.continueTask`）。

### 3.4 客户端侧新增能力
- `A2aClient#setPushNotification(...)`：调用 `tasks/pushNotification/set`。
- `WebhookCallbackController`：接收 `/webhook/a2a-callback`。
- `WebhookResultStore`：按 taskId 保存等待中的 `CompletableFuture`。
- `LongRunningTaskService`：提交任务 + 注册 webhook + 等待回调。
- `LongRunningTaskController`：`/api/long-task/submit` 对外演示。

## 4. 端到端时序
1. Client 调 `tasks/send`，拿到 `submitted`。
2. Client 调 `tasks/pushNotification/set` 注册回调 URL。
3. Server 异步执行 Skill。
4. Server 完成后向 `webhookUrl` 发送回调。
5. Client 回调接口收到结果，唤醒等待中的 Future。
6. 业务接口返回最终报告。

## 5. 调用示例
### 5.1 直接调用业务演示接口
```bash
curl "http://localhost:8080/api/long-task/submit?question=2025年1月1日到2025年1月31日，华东地区销售报告"
```

### 5.2 原生 JSON-RPC 两步法
第一步：提交任务
```bash
curl -X POST http://localhost:8080/ -H "Content-Type: application/json" -d '{
  "jsonrpc":"2.0",
  "method":"tasks/send",
  "params":{
    "id":"task-push-001",
    "sessionId":"session-001",
    "skillId":"generate-sales-report",
    "history":[{"role":"user","parts":[{"type":"text","text":"生成2025年1月华东销售报告"}]}]
  },
  "id":"req-001"
}'
```

第二步：注册回调
```bash
curl -X POST http://localhost:8080/ -H "Content-Type: application/json" -d '{
  "jsonrpc":"2.0",
  "method":"tasks/pushNotification/set",
  "params":{
    "id":"task-push-001",
    "webhookUrl":"http://localhost:8080/webhook/a2a-callback"
  },
  "id":"req-002"
}'
```

## 6. 设计注意点
- 生产环境里，`send` 与 `set` 之间可能有竞态，建议在服务端做“先完成后补推”兜底。
- 当前示例把回调地址写死为本机地址，生产应改为配置项。
- Webhook 最好加签名、鉴权和重试机制，避免伪造请求与回调丢失。
- 失败任务也应推送，便于客户端及时结束等待并显示错误。
