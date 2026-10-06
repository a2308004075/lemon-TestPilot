-- ============================================================
-- V3：补齐 TimestampedEntity 基类所需的 created_at 列
-- test_case / regression_list 两表在建表时仅含 created_date + updated_at，
-- 实体继承基类后需要 created_at 列（增量补列，不影响存量数据）
-- ============================================================

ALTER TABLE test_case
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER linked_bug_no;

ALTER TABLE regression_list
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER progress;
