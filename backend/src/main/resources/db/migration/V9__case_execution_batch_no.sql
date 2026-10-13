-- ============================================================
-- TestPilot · V9 用例执行批次增加批次号
-- 格式：YYYYMMDDNNN（如 20261013001），按创建日期每天从 001 递增
-- ============================================================

ALTER TABLE case_execution_batch
    ADD COLUMN batch_no VARCHAR(20) COMMENT '批次号：YYYYMMDDNNN' AFTER id;

-- 存量批次按创建日期顺序回填批次号
WITH numbered AS (
    SELECT id,
           created_at,
           ROW_NUMBER() OVER (PARTITION BY DATE(created_at) ORDER BY id) AS rn
    FROM case_execution_batch
)
UPDATE case_execution_batch b
    JOIN numbered n ON b.id = n.id
SET b.batch_no = CONCAT(DATE_FORMAT(n.created_at, '%Y%m%d'), LPAD(n.rn, 3, '0'));

ALTER TABLE case_execution_batch
    MODIFY COLUMN batch_no VARCHAR(20) NOT NULL COMMENT '批次号：YYYYMMDDNNN';

ALTER TABLE case_execution_batch ADD UNIQUE KEY uk_batch_no (batch_no);
