-- ============================================================
-- TestPilot · V13 语义检索（第 2 步：BLOB 向量 + 应用层余弦 + 三级降级）
-- kb_embedding：知识向量缓存（float32 小端序列化，一行一知识覆盖式更新）
-- kb_retrieval_hit：检索命中软引用（可追溯，不参与撤销保护）
-- sys_llm_config：增加用途隔离（chat/embedding，同厂商可各配一行）
-- run_task_snapshot：记录本步骤检索方式与加载的知识快照
-- ============================================================

-- 1. 知识向量缓存（命名对齐当前实现：kb_id -> kb_item.id）
CREATE TABLE kb_embedding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL COMMENT '知识主键 kb_item.id',
    kb_version INT NOT NULL COMMENT '生成向量时的知识版本；与当前版本不符即视为过期（宁可检索不到不用旧向量）',
    vector LONGBLOB NOT NULL COMMENT 'float32 数组小端序列化',
    vector_model VARCHAR(100) NOT NULL DEFAULT '' COMMENT '嵌入模型名',
    dim INT NOT NULL DEFAULT 0 COMMENT '向量维度',
    status VARCHAR(10) NOT NULL DEFAULT 'ready' COMMENT 'ready|pending（重嵌失败置 pending，可查）',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kb (kb_id),
    KEY idx_model (vector_model)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识向量缓存（一行一知识，覆盖式更新）';

-- 2. 检索命中软引用
-- 注：score 由设计文档 DECIMAL(6,4) 放宽为 DECIMAL(10,4)，关键词累计得分可超过 99.9999
CREATE TABLE kb_retrieval_hit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL COMMENT '执行记录 run_record.id',
    route_id VARCHAR(50) NOT NULL DEFAULT '' COMMENT '路由任务类型（taskType，如 testcase_gen）',
    kb_id BIGINT NOT NULL COMMENT '知识主键 kb_item.id',
    title_snapshot VARCHAR(200) NOT NULL DEFAULT '' COMMENT '命中时标题快照',
    version_snapshot INT NOT NULL DEFAULT 0 COMMENT '命中时版本快照',
    score DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '相似度/关键词得分',
    method VARCHAR(10) NOT NULL DEFAULT '' COMMENT 'semantic|keyword',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_run (run_id, route_id),
    KEY idx_kb (kb_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 检索命中软引用（可追溯，不参与撤销保护）';

-- 3. 模型配置用途隔离：chat/embedding
-- 原 uk_vendor 唯一键使得同厂商只能配一行；DeepSeek 无 embeddings 端点，
-- 需要 chat 与 embedding 各配一行（不同 base_url/model_name）
ALTER TABLE sys_llm_config
    ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'chat'
        COMMENT '用途：chat/embedding' AFTER vendor_name;
ALTER TABLE sys_llm_config DROP INDEX uk_vendor;
ALTER TABLE sys_llm_config ADD KEY idx_vendor (vendor_code);

-- 4. 执行快照记录检索方式与加载的知识（供执行记录详情与助手页展示）
ALTER TABLE run_task_snapshot
    ADD COLUMN knowledge_json TEXT NULL
        COMMENT '本次加载的知识 [{id,title,version,score,method}]' AFTER missing_json;
