-- 为存量知识填充摘要（已备份 kb_item 表）
UPDATE kb_item k
LEFT JOIN task_record t ON REPLACE(k.source_task, 'task/', '') = t.task_no
SET k.summary = CASE
    WHEN k.kb_no = 'kb_002' THEN '重点检索 callback、idempotent、consume、order status 与 traceId。'
    WHEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.suggestions[0]')) IS NOT NULL
         AND JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.suggestions[0]')) != ''
         THEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.suggestions[0]'))
    WHEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.recommended_logs[0]')) IS NOT NULL
         AND JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.recommended_logs[0]')) != ''
         THEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.recommended_logs[0]'))
    WHEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.optimization_suggestions[0]')) IS NOT NULL
         AND JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.optimization_suggestions[0]')) != ''
         THEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.optimization_suggestions[0]'))
    WHEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.possible_causes[0]')) IS NOT NULL
         AND JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.possible_causes[0]')) != ''
         THEN JSON_UNQUOTE(JSON_EXTRACT(t.output_json, '$.possible_causes[0]'))
    ELSE LEFT(k.body, 120)
END
WHERE k.summary IS NULL OR k.summary = '';
