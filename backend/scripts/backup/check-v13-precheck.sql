-- V13 执行前预检：确认目标对象现状（purpose/知识列/唯一键/新表）
SELECT 'COLUMN' AS kind, table_name, column_name AS detail
FROM information_schema.columns
WHERE table_schema = 'lemon_testpilot'
  AND ((table_name = 'sys_llm_config' AND column_name = 'purpose')
    OR (table_name = 'run_task_snapshot' AND column_name = 'knowledge_json'));

SELECT 'INDEX' AS kind, table_name, index_name AS detail, non_unique
FROM information_schema.statistics
WHERE table_schema = 'lemon_testpilot' AND table_name = 'sys_llm_config';

SELECT 'TABLE' AS kind, table_name, table_rows AS detail
FROM information_schema.tables
WHERE table_schema = 'lemon_testpilot' AND table_name IN ('kb_embedding', 'kb_retrieval_hit');
