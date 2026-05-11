# 03-多 Agent 编排协作学习笔记

## 1. 本章结论
多 Agent 编排的核心不是“多创建几个 Agent”，而是把复杂任务拆成可控的协作结构：顺序依赖用 Sequential，并行视角用 Parallel，质量迭代用 Loop。

本章代码没有强依赖具体 AgentScope SDK，而是先用工程化 demo 把编排模型跑通。这样学习重点会落在“协作结构”上，而不是被版本和依赖细节打断。

## 2. 为什么需要多 Agent
单 Agent 在复杂任务里有两个常见瓶颈：

- System Prompt 太长，角色边界容易混乱。
- 一个 Agent 同时负责调研、生成、审查，质量和效率很难兼顾。

多 Agent 的思路是：一个 Agent 只做好一个专业角色，多个 Agent 通过 Pipeline 协作完成目标。

## 3. 三种编排模式

### 3.1 Sequential：顺序编排
适合前后有依赖的任务。

本章 demo：
- 意图分析 Agent
- 计划拆解 Agent
- 总结 Agent

执行特点：
- 第一步先理解用户输入。
- 第二步基于理解生成计划。
- 第三步基于前面上下文输出总结。

典型场景：
- 需求分析 -> 方案设计 -> 输出报告
- 自然语言 -> SQL 生成 -> SQL 审查
- 文档生成 -> 风格检查 -> 最终润色

### 3.2 Parallel：并行编排
适合多个视角互相独立的任务。

本章 demo：
- 技术研究 Agent
- 商业分析 Agent
- 风险审查 Agent

执行特点：
- 多个 Agent 同时处理同一个输入。
- 每个 Agent 输出自己的专业视角。
- 最后由服务层合并结果。

典型场景：
- 行业调研
- 多数据源检索
- 多专家评审

### 3.3 Loop：循环编排
适合需要反复改进直到达标的任务。

本章 demo：
- SQL 生成 Agent
- SQL 评分 Agent

执行特点：
- 先生成草稿。
- 再评分。
- 分数低于阈值就继续生成。
- 达标或达到最大迭代次数后退出。

典型场景：
- 代码生成 + 测试
- SQL 生成 + 质量评分
- 文案生成 + 审核改写

## 4. 代码阅读路径

先看模型：
- `multiagent/model/AgentContext.java`
- `multiagent/model/AgentStep.java`
- `multiagent/model/PipelineResponse.java`

再看 Agent 抽象：
- `multiagent/core/DemoAgent.java`

再看三种编排器：
- `multiagent/core/SequentialPipeline.java`
- `multiagent/core/ParallelPipeline.java`
- `multiagent/core/LoopPipeline.java`

再看专业 Agent：
- `multiagent/agent/IntentAnalysisAgent.java`
- `multiagent/agent/TechnologyResearchAgent.java`
- `multiagent/agent/DraftSqlAgent.java`
- `multiagent/agent/SqlReviewAgent.java`

最后看对外接口：
- `multiagent/service/MultiAgentPipelineService.java`
- `multiagent/controller/MultiAgentPipelineController.java`

## 5. 本地测试

启动模块：

```bash
mvn -pl agentscope spring-boot:run
```

### 5.1 测试顺序编排

```bash
curl -X POST "http://localhost:8082/api/agentscope/multi-agent/sequential" ^
  -H "Content-Type: application/json" ^
  -d "{\"input\":\"帮我设计一个多 Agent 销售分析系统\"}"
```

重点观察：
- `mode` 是否为 `SEQUENTIAL`
- `steps` 是否按意图分析、计划拆解、总结顺序执行
- 后续 Agent 是否利用了前面 Agent 的上下文

### 5.2 测试并行编排

```bash
curl -X POST "http://localhost:8082/api/agentscope/multi-agent/parallel" ^
  -H "Content-Type: application/json" ^
  -d "{\"input\":\"调研多 Agent 在企业知识库中的应用价值\"}"
```

重点观察：
- `mode` 是否为 `PARALLEL`
- `steps` 是否包含技术、商业、风险三个视角
- 每个视角是否相互独立

### 5.3 测试循环编排

```bash
curl -X POST "http://localhost:8082/api/agentscope/multi-agent/loop" ^
  -H "Content-Type: application/json" ^
  -d "{\"input\":\"生成华东地区销售额汇总 SQL\",\"maxIterations\":3}"
```

重点观察：
- 第一次 SQL 评分较低。
- 第二次生成会补上聚合和分组。
- 评分达标后循环停止。

## 6. Apifox 导入

启动 `agentscope` 后导入：

```text
http://localhost:8082/v3/api-docs
```

导入后会看到目录：

- AgentScope 任务
- AgentScope 健康检查
- AgentScope 多 Agent 编排

本章新增接口都在 `AgentScope 多 Agent 编排` 下。

## 7. 工程化思考

这个 demo 已经具备工程结构，但还不是生产版。下一步可以继续增强：

- 把 `AgentContext` 持久化，支持任务恢复。
- 给每个 `AgentStep` 增加 traceId、状态、异常信息。
- 把 Agent 注册成 Spring Bean，而不是在 Service 里手动 new。
- 增加真正的大模型调用，让 DemoAgent 变成 LLM Agent。
- 增加 Hook 机制，记录每个 Agent 的输入、输出、耗时和评分。

## 8. 选型口诀

- 步骤有依赖：用 Sequential。
- 多视角分析：用 Parallel。
- 质量要达标：用 Loop。
- 复杂业务：三者可以嵌套组合。
