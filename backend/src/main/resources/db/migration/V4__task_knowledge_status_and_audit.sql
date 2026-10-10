-- ============================================================
-- TestPilot V4：任务知识入库状态 + API Key 密文加宽 + 操作审计表
-- ============================================================

-- 1. 分析任务增加知识入库状态（未入库/待审核/已入库）
ALTER TABLE task_record
    ADD COLUMN knowledge_status VARCHAR(20) NOT NULL DEFAULT '未入库'
        COMMENT '知识入库状态：未入库/待审核/已入库' AFTER review_status;

-- 2. API Key 改为 AES-GCM 密文存储（enc: 前缀），加宽列以容纳密文
ALTER TABLE sys_llm_config
    MODIFY COLUMN api_key VARCHAR(500) NOT NULL DEFAULT ''
        COMMENT 'API Key（AES-GCM 密文，enc: 前缀；兼容存量明文）';

-- 3. 操作审计（详情抽屉时间线数据源）
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(30) NOT NULL COMMENT '实体类型：task/case/kb/regression/report/module/llm',
    entity_no VARCHAR(50) NOT NULL COMMENT '业务编号（taskNo/caseNo/kbNo 等）',
    action VARCHAR(50) NOT NULL COMMENT '动作',
    detail VARCHAR(500) NOT NULL DEFAULT '' COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_entity (entity_type, entity_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作审计';
