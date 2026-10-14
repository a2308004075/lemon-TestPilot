-- Check current updated_at values before regenerating batch_no
SELECT id, batch_no, title, created_at, updated_at
FROM lemon_testpilot.case_execution_batch
ORDER BY id;
