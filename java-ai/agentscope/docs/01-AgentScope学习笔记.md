# AgentScope 学习笔记（基于 jichi.md）

## 1. 核心结论
AgentScope 不是替代 Spring AI，而是补齐“复杂多 Agent 协作 + 全链路透明 + 可人工干预”场景的框架能力。

## 2. 为什么在 A2A 之后学习 AgentScope
你已经有 A2A 的能力：
- Agent 之间可互相委托任务
- 能做同步、SSE 流式、Webhook 异步回调

但在复杂任务里还会遇到一个痛点：
- 任务跑起来了，但推理过程不透明
- 工具调用出错时，需要大量手动日志定位

AgentScope 的价值正好在这里：让 Agent 的关键行为变成结构化、可拦截、可观测的事件。

## 3. AgentScope 的设计哲学
一句话：开发者应该能“看见并干预”Agent 的每一步。

落地点：
- Hook 系统：在 `reply / tool_call / observe` 等生命周期插逻辑
- PlanNotebook：公开任务计划，支持中途干预
- Human-in-the-Loop：关键节点暂停等待人工确认
- 透明 Prompt：调试时能看见完整输入上下文

## 4. 和 Spring AI / LangChain4j 的选型关系
- Spring AI：适合“业务 API + AI 能力快速集成”
- LangChain4j：适合“纯 Java 生态、框架中立”
- AgentScope：适合“多 Agent 编排、强可观测、可干预”

实践建议：
- 常规业务场景继续用 Spring AI
- 需要任务规划、行为审计、人工闸门的场景引入 AgentScope
- 不是三选一，而是按场景组合

## 5. 你这个学习模块的目标
1. 理解透明性设计：为什么 Hook 比“到处加日志”更可维护
2. 做出一个 ReActAgent + Tool 调用样例
3. 理解短期/长期记忆在业务中的边界
4. 用 PlanNotebook 表达可执行计划
5. 把 A2A 已有能力接入 AgentScope 流程

## 6. 结合当前仓库的学习路径
- 第一步：复用 `a2a/docs` 的任务流转思想（Task/State/Callback）
- 第二步：在 `agentscope` 模块先跑通基础接口（本模块已创建 `/api/agentscope/ping`）
- 第三步：逐步加 Hook、计划、记忆和人工确认
- 第四步：再做与 A2A 的桥接（A2A 负责协作，AgentScope 负责透明执行）

## 7. 风险与注意点
- AgentScope 社区体量小于 Spring AI，资料相对少
- 版本演进快，升级时注意 breaking changes
- 建议先在非核心链路灰度验证再进生产

## 8. 一句话复盘
A2A 解决“Agent 怎么协作”，AgentScope 解决“协作过程怎么被看见并可控”。
