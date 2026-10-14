-- 已入库经验标签一致性核验（只读）
SELECT task_type, COUNT(*) AS cnt FROM task_record GROUP BY task_type;
SELECT k.kb_no, k.category, k.source_task, t.task_type AS src_task_type
FROM kb_item k
LEFT JOIN task_record t ON k.source_task = t.task_no
WHERE k.source_task <> '人工维护';
-- 错标检测：kb.category 与来源任务 task_type 的映射不一致
SELECT k.kb_no, k.category AS kb_category, k.source_task, t.task_type AS src_task_type
FROM kb_item k
JOIN task_record t ON k.source_task = t.task_no
WHERE NOT (
    (t.task_type = 'bug_analysis' AND k.category = '历史 Bug 库')
 OR (t.task_type = 'log_triage'  AND k.category = '日志规律库')
 OR (t.task_type = 'sql_analysis' AND k.category = 'SQL 经验库')
);
-- 孤儿条目：source_task 指向的任务不存在
SELECT k.kb_no, k.category, k.source_task
FROM kb_item k
WHERE k.source_task <> '人工维护'
  AND NOT EXISTS (SELECT 1 FROM task_record t WHERE t.task_no = k.source_task);
