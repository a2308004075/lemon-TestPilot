-- ============================================================
-- TestPilot · V12 用例版本改为自由文本
-- 版本由用户手动维护（如 v1 / v1.0.1），不再限定整数、不再自动升版；
-- 存量整数回填 vN 前缀，前端展示不再拼 v
-- ============================================================

ALTER TABLE test_case
    MODIFY COLUMN version VARCHAR(50) NOT NULL DEFAULT 'v1' COMMENT '版本（用户手动维护，如 v1.0.1）';

-- 存量整数版本回填为 vN 前缀，与展示格式一致（防御性条件，重复执行不产生重复前缀）
UPDATE test_case SET version = CONCAT('v', version) WHERE version NOT LIKE 'v%';
