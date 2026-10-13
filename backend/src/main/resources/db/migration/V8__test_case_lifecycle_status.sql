-- 用例状态由执行状态改为生命周期状态：使用 / 废弃
ALTER TABLE test_case MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT '使用' COMMENT '状态：使用/废弃';

-- 存量用例统一迁移为“使用”
UPDATE test_case SET status = '使用' WHERE status NOT IN ('使用', '废弃') OR status IS NULL;
