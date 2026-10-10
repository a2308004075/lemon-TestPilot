-- 任务与知识关联（发布知识后自动进入对应任务的检索范围）
CREATE TABLE sys_task_kb_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    main_task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    category VARCHAR(50) NOT NULL COMMENT '知识分类',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '顺序',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    UNIQUE KEY uk_task_category (main_task_type, category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务与知识关联规则';

-- 种子数据与手册配置页一致
INSERT INTO sys_task_kb_rule (main_task_type, category, sort_order, enabled) VALUES
('testcase_gen', '业务规则库', 1, 1),
('testcase_gen', '回归规则库', 2, 1),
('testcase_gen', '历史 Bug 库', 3, 1),
('bug_analysis', '历史 Bug 库', 1, 1),
('bug_analysis', '日志规律库', 2, 1),
('bug_analysis', '业务规则库', 3, 1),
('log_triage', '日志规律库', 1, 1),
('log_triage', '接口异常库', 2, 1),
('log_triage', '历史 Bug 库', 3, 1),
('sql_analysis', 'SQL 经验库', 1, 1),
('sql_analysis', '业务规则库', 2, 1),
('regression_list', '业务规则库', 1, 1),
('regression_list', '回归规则库', 2, 1),
('regression_list', '历史 Bug 库', 3, 1),
('test_report', '业务规则库', 1, 1),
('test_report', '回归规则库', 2, 1),
('test_report', '历史 Bug 库', 3, 1);
