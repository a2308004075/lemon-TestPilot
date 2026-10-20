# TestPilot 语义检索第 3 步：专用向量库升级预留方案

> 状态：**预留方案（暂不开发）**。第 2 步（MySQL BLOB 向量 + 应用层余弦 + 三级降级）已落地并通过运行验证。
> 本文档说明第 3 步的触发条件、候选选型与平滑升级路径，供后续按需开发。

---

## 1. 背景与演进阶梯

知识库检索按"先能用、再专业"的阶梯演进：

| 阶段 | 方案 | 状态 |
| --- | --- | --- |
| 第 1 步 | 模块匹配引用（按分类+模块人工圈定） | 已被第 2 步取代 |
| 第 2 步 | MySQL 存向量（LONGBLOB）+ 应用层余弦 + semantic→keyword→none 三级降级 + 软引用可追溯 | **已落地** |
| 第 3 步 | 专用向量库（ANN 索引、过滤、命名空间），本文档描述 | 预留 |

第 2 步在知识量较小的阶段完全够用（全量内存余弦 O(N)，无新增组件、无运维负担）。第 3 步的价值在于**规模**与**检索质量**：当 N 或延迟超过阈值时，用专业向量库替换应用层计算。

---

## 2. 第 2 步现状（升级起点）

### 2.1 能力清单

- 向量生成：`EmbeddingService.embed()`，OpenAI 兼容 `POST /embeddings`，API Key 走加密库（CryptoService）
- 向量存储：`kb_embedding` 表，一行一知识（`uk_kb` 唯一），float32 小端序列化 LONGBLOB
- 过期判定：`kb_version` 与当前版本一致 **且** `vector_model` 与当前模型一致，才可用（宁可检索不到，不用旧向量）
- 懒补偿：检索时对候选集中失效/缺失向量批量补嵌，单次上限 16 条
- 三级降级：semantic（余弦 ≥ 0.30，Top5）→ keyword（中文 2~3 字 n-gram + 英文词元，标题权重 3 / 正文 1）→ none（显式"未检索到相关知识（未虚构）"）
- 软引用落库：命中写 `kb_retrieval_hit`（可追溯，不阻塞撤销）；执行快照记 `knowledge_json`
- 配置隔离：`sys_llm_config.purpose`（chat / embedding 同厂商可各配一行）

### 2.2 关键参数（当前值）

| 参数 | 值 | 位置 |
| --- | --- | --- |
| 语义命中阈值 | 0.30 | `RetrievalService.SEMANTIC_THRESHOLD` |
| 返回条数 | Top5（score 降序、id 升序） | `RetrievalService.TOP_K` |
| 懒补偿上限 | 16 条/次 | `EmbeddingService.lazyCompensate` |
| 分词上限 | 800 词元 | `RetrievalService.tokenize` |
| 向量编码 | float32 小端 | `EmbeddingService.serialize` |
| 得分离散度 | DECIMAL(10,4) | `kb_retrieval_hit.score` |

### 2.3 当前瓶颈预判

- 单次检索候选 = 分类内全部已发布知识，全量反序列化 + 余弦，O(N) 且吃内存
- 无法做 ANN 近似、无原生元数据过滤索引、无命名空间隔离
- 高频增量写入依赖懒补偿 + 审核/编辑触发，非实时

---

## 3. 升级触发条件（量化）

满足任一条件即建议启动第 3 步：

1. **规模**：已发布知识 ≥ 5000 条，或 `kb_embedding` 向量总量 > 200 MB
2. **延迟**：单次检索（不含嵌入 API 耗时）P95 > 300 ms
3. **过滤/隔离**：需要项目/模块/时间等组合过滤或跨项目命名空间隔离
4. **实时性**：要求秒级增量可见（当前为懒补偿 + 事件驱动）
5. **多模态/多模型**：需要同时维护多套向量模型并路由

未达条件时继续使用第 2 步，避免过早引入运维复杂度。

---

## 4. 候选向量库对比

| 方案 | 部署成本 | Java 生态 | 过滤能力 | 适用规模 | 结论 |
| --- | --- | --- | --- | --- | --- |
| **Qdrant** | 单二进制 / Docker，零依赖 | 官方 Java client 成熟 | payload filter 强（条件组合/嵌套） | 百万级 | **首选**：低运维、REST/gRPC 双协议 |
| Milvus | 依赖 etcd/MinIO，组件多 | Java SDK 可用 | 强 | 亿级集群 | 中长期大规模再评估 |
| pgvector | 需已有 PostgreSQL | JDBC 直连 | SQL 联合过滤自然 | 百万级 | 若未来引入 PG 可作低运维替代 |
| Elasticsearch kNN | 需已有 ES 集群 | 成熟 | 强 | 千万级 | 仅当已存在 ES 时复用 |
| MySQL 原生 | — | — | — | — | MySQL 8.0 无原生向量类型与 ANN 索引，不能作为第 3 步载体 |

**推荐决策**：独立部署 **Qdrant**；在项目引入 PostgreSQL 的前提下，可用 **pgvector** 作为零新增组件的备选。

---

## 5. 平滑升级设计（代码预留点）

### 5.1 抽象边界（现有代码已天然隔离，无需重构）

- 语义检索唯一入口：`RetrievalService.semanticHits(...)`——升级只改这一个方法内部
- 向量生成与存储解耦：`EmbeddingService.embed(cfg, text)` 保持不变（换库不换嵌）
- 升级动作 = 新增 `VectorStoreService`：
  - `upsert(kbId, version, model, float[] vector, payload)`
  - `delete(kbId)`
  - `search(float[] query, int topK, Map<String,Object> filters)`
- `semanticHits` 改为：生成查询向量 → `vectorStore.search(...)` → 映射回 KbItem → 落软引用
- 三级降级链结构不变：向量库异常（连接失败/超时）捕获后走 keyword

### 5.2 接口契约不变清单（前端零改动目标）

以下结构/字段在升级前后必须保持一致：

- `RetrievalService.Result / Hit`、`toHitMap` 的 `{id, kbNo, title, category, version, risk, score, method}`
- `recordHits` 落库行为、`run_task_snapshot.knowledge_json` 结构
- `GET /api/kb/search`、`GET /api/kb/{id}/impact`、`GET /api/kb/embeddings/status`、`POST /api/kb/embeddings/reembed` 的响应字段
- `embeddingStatus` 中 `ready/pending/staleVersion/staleModels` 的语义（数据源改为向量库计数）

### 5.3 数据迁移步骤（一次性）

1. 部署 Qdrant 实例，建 collection：维度 = 当前嵌入模型维度、距离 = Cosine、payload = `{kb_id, category, project, module, version, model}`
2. **全量重嵌优于搬运**：旧向量可能混合多模型，直接按当前模型全量重嵌入库（复用 `reembedAll` 的遍历逻辑）
3. 双写过渡期：`embedKnowledge` 同时写 `kb_embedding`（MySQL）与 Qdrant，配置开关控制
4. 校验一致性：抽样 50~100 条，对比 MySQL 余弦 Top5 与 Qdrant Top5 的重合率（重合率 ≥ 90% 视为通过）
5. 切换读路径：配置 `vector.store=qdrant|mysql`，观测降级率与延迟
6. `kb_embedding` 表保留至少一个版本周期用于回退，随后按需清理

### 5.4 新增配置项（预留）

```properties
# 向量库类型：mysql（第 2 步现状，默认）/ qdrant（第 3 步）
vector.store=mysql
vector.qdrant.host=127.0.0.1
vector.qdrant.port=6333
vector.qdrant.collection=testpilot_kb
vector.qdrant.api-key=
```

---

## 6. 风险与回退

| 风险 | 缓解 |
| --- | --- |
| 向量库故障导致检索不可用 | 已有降级链：捕获异常 → keyword → none，业务不中断 |
| 双写期数据不一致 | 迁移窗口内以 MySQL 为权威源；校验脚本兜底 |
| 换库后召回质量下降 | 上线前跑影子对比（同一 query 双库结果重合率） |
| 回退成本 | `vector.store=mysql` 一键切回；`kb_embedding` 保留完整 |

---

## 7. 附：与第 2 步设计文档的关系

- 第 2 步的详细设计见 `docs/TestPilot-my详细设计.md` 第 4.6 节（知识库语义检索）
- 本文档为第 3 步的**升级预留**，不改变第 2 步的任何已上线契约
- 启动第 3 步开发时：先跑第 3 节触发条件评估，再按第 5.3 节步骤执行
