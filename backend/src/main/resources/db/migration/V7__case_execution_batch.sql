-- ============================================================
-- TestPilot · V7 用例执行批次子系统（6.11 W3）
-- 勾选用例创建执行批次，逐条登记执行结果并与用例状态联动
-- ============================================================

-- 1. 用例执行批次（计数冗余列，列表进度直读）
CREATE TABLE case_execution_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '批次标题（自动：用例执行批次 yyyy-MM-dd HH:mm）',
    status VARCHAR(10) NOT NULL DEFAULT '执行中' COMMENT '状态：执行中/已完成',
    total INT NOT NULL DEFAULT 0 COMMENT '用例总数',
    passed INT NOT NULL DEFAULT 0 COMMENT '通过数',
    failed INT NOT NULL DEFAULT 0 COMMENT '失败数',
    blocked INT NOT NULL DEFAULT 0 COMMENT '阻塞数',
    skipped INT NOT NULL DEFAULT 0 COMMENT '跳过数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_batch_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用例执行批次';

-- 2. 用例执行条目（创建时快照用例编号/标题/优先级，执行时逐条登记）
CREATE TABLE case_execution_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    seq_order INT NOT NULL COMMENT '顺序（按勾选顺序）',
    case_id BIGINT NOT NULL COMMENT '用例ID（联动用例状态）',
    case_no_snapshot VARCHAR(50) NOT NULL COMMENT '用例编号快照',
    title_snapshot VARCHAR(200) NOT NULL COMMENT '用例标题快照',
    priority_snapshot VARCHAR(10) NOT NULL COMMENT '优先级快照',
    status VARCHAR(10) NOT NULL DEFAULT '待执行' COMMENT '状态：待执行/通过/失败/阻塞/跳过',
    note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '执行备注',
    executed_at DATETIME COMMENT '执行时间（待执行为空）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_item_batch (batch_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用例执行条目';
