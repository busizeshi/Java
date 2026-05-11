# 02-AgentScope工程落地学习笔记（基于 jichi.md 下一章节）

## 1. 本章结论
AgentScope 的学习重点，不是“会不会调用模型”，而是“能不能把 Agent 的执行过程做成可观测、可干预、可回放的工程链路”。

## 2. 承接上一章：从“知道是什么”到“知道怎么做”
上一章我们明确了 AgentScope 的定位：
- 补齐 Spring AI 在复杂多步骤任务中的透明性短板
- 适合复杂协作、任务规划、人工介入场景

这一章开始解决落地问题：
- 你的系统里，哪些点需要 Hook？
- PlanNotebook 在业务里怎么设计？
- Human-in-the-Loop 放在哪个节点最有价值？

## 3. 工程落地三层模型

### 3.1 执行层（Execution Layer）
职责：真正跑任务。
- 接收任务输入
- 调模型、调工具、更新状态
- 输出中间事件和最终结果

关键要求：
- 每个任务必须有 `taskId`
- 每次执行必须有 `traceId`
- 每一步行为必须有阶段标识（如 `observe/tool_call/reply`）

### 3.2 观测层（Observability Layer）
职责：让执行过程可见。
- Hook 采集关键事件
- 记录耗时、入参摘要、出参摘要、异常信息
- 支持按 `taskId/traceId` 检索完整链路

关键要求：
- 结构化日志优先于散落文本日志
- Prompt 输出必须做脱敏
- 错误信息要能直接定位到“哪一步失败”

### 3.3 干预层（Intervention Layer）
职责：在关键节点让人或策略接管。
- 高风险工具调用前人工确认
- 预算超限时自动拦截
- 输出质量不达标时回退重试

关键要求：
- 干预策略可配置
- 干预动作可审计
- 干预后任务可恢复执行

## 4. 面向你当前仓库的映射方式
你现在已有 `a2a` 模块，已经有任务状态流转与异步回调经验。
在 `agentscope` 模块建议直接复用这套思路：

1. 统一任务状态模型
- 至少包含：`SUBMITTED / RUNNING / SUCCEEDED / FAILED`

2. 统一接口风格
- `submit`：提交任务并返回 `taskId`
- `status`：按 `taskId` 查状态与结果

3. 统一异步机制
- 先内存实现（学习阶段）
- 再替换为 DB + MQ（工程阶段）

## 5. 本章最小实践目标（先跑通）

1. 在 `agentscope` 新增任务提交接口
2. 用线程池异步执行“模拟 Agent 任务”
3. 在执行过程打印 3 类 Hook 事件：
- `observe`：接收任务与计划生成
- `tool_call`：工具调用开始/结束
- `reply`：生成最终回复
4. 增加任务状态查询接口

完成标准：
- 能提交任务并马上返回
- 能轮询看到状态从 `SUBMITTED -> RUNNING -> SUCCEEDED`
- 失败时能看到 `FAILED` 和错误原因

## 6. 你要学什么代码（按顺序）
先按下面顺序看代码，每个文件只解决一个问题。

1. 启动与入口
- `src/main/java/com/jwd/agentscope/AgentScopeApplication.java`
- 关注点：模块是否能独立启动

2. 线程池配置
- `src/main/java/com/jwd/agentscope/config/AgentScopeAsyncConfig.java`
- 关注点：为什么任务要异步执行，线程池参数怎么影响吞吐

3. 任务状态模型
- `src/main/java/com/jwd/agentscope/model/AgentTaskState.java`
- `src/main/java/com/jwd/agentscope/model/AgentTask.java`
- 关注点：状态流转、时间字段、结果/错误字段

4. 仓储层
- `src/main/java/com/jwd/agentscope/repository/AgentTaskRepository.java`
- 关注点：为什么学习阶段先用内存 `ConcurrentHashMap`

5. 服务层（最关键）
- `src/main/java/com/jwd/agentscope/service/AgentTaskService.java`
- 关注点：
  - `submit` 如何生成 `taskId/traceId`
  - `runTask` 如何推进状态
  - `hook` 如何打点 `observe/tool_call/reply/error`

6. 控制层
- `src/main/java/com/jwd/agentscope/controller/AgentTaskController.java`
- 关注点：`submit/status` 的输入输出契约

7. 日志配置
- `src/main/resources/application.yml`
- 关注点：`[%X{traceId}]` 如何串联同一次任务日志

## 7. 本地启动步骤（一步一步）

### 7.1 编译
在项目根目录执行：
```bash
mvn -pl agentscope -am compile
```
预期：`BUILD SUCCESS`

### 7.2 启动 agentscope 模块
```bash
mvn -pl agentscope spring-boot:run
```
预期：服务监听 `8082` 端口。

### 7.3 健康检查
```bash
curl "http://localhost:8082/api/agentscope/ping"
```
预期返回：`module=agentscope`、`status=ok`。

## 8. 接口测试步骤（核心）

### 8.1 提交任务
```bash
curl "http://localhost:8082/api/agentscope/tasks/submit?question=请分析AgentScope在复杂任务中的价值"
```
预期返回示例：
```json
{
  "taskId":"...",
  "traceId":"...",
  "state":"SUBMITTED"
}
```
把 `taskId` 记下来。

### 8.2 查询状态
```bash
curl "http://localhost:8082/api/agentscope/tasks/status?taskId=<你的taskId>"
```
预期状态变化：
- 刚提交：`SUBMITTED`
- 执行中：`RUNNING`
- 完成后：`SUCCEEDED`

成功时 `result` 有值；失败时 `state=FAILED` 且 `error` 有值。

### 8.3 高频轮询测试（观察状态流转）
PowerShell 示例：
```powershell
$taskId = "替换成你的taskId"
1..6 | ForEach-Object {
  Invoke-RestMethod "http://localhost:8082/api/agentscope/tasks/status?taskId=$taskId"
  Start-Sleep -Milliseconds 400
}
```
目标：亲眼看到状态变化，而不是只看最终结果。

## 9. 如何观察 Hook 日志（重点）
在服务日志里搜索 `hook_stage=`，你会看到：
- `hook_stage=observe`：任务开始执行
- `hook_stage=tool_call`：模拟工具调用
- `hook_stage=reply`：生成最终回复

同时会看到日志里 `traceId` 字段（来自 MDC），用于串起同一次任务的所有事件。

## 10. 学习检查清单（完成一轮后自测）
你可以逐条自检：

1. 我能解释为什么 `submit` 要立即返回，而不是同步阻塞。
2. 我能描述 `SUBMITTED -> RUNNING -> SUCCEEDED/FAILED` 的状态机。
3. 我能根据 `traceId` 在日志里定位一次完整任务链路。
4. 我知道 Hook 的价值是“可观测/可干预”，不只是“多打印几行日志”。
5. 我知道当前实现是学习版（内存存储），生产要换成持久化。

## 11. 常见问题与排错

1. 端口占用
- 现象：启动失败提示端口被占用
- 处理：修改 `application.yml` 的 `server.port`

2. `taskId` 查不到
- 现象：`status` 返回 404
- 处理：确认使用的是 `submit` 返回的最新 `taskId`

3. 看不到 `traceId`
- 现象：日志中 `[]` 为空
- 处理：确认请求走的是异步任务接口，且日志 pattern 未被覆盖

4. 编译报编码问题（BOM）
- 现象：`非法字符: '\ufeff'`
- 处理：把 Java 源码转为 UTF-8 无 BOM

## 12. 下一步学习建议
下一章进入“从学习版到工程版”的第一步：
- 把 `AgentTaskRepository` 从内存改成数据库
- 把 `hook` 从硬编码改成可插拔拦截器
- 给 `tool_call` 增加耗时统计与失败重试
