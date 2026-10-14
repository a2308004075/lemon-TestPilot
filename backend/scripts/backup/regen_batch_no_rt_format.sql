-- Regenerate batch_no to new timestamp format (RT-yyyyMMddHHmmss).
-- Backup: case_execution_batch_before_batch_no_rt.sql (mysqldump, full table)
-- New no derives from each row's created_at (unique seconds, no collision).
-- updated_at is pinned to its original value to avoid ON UPDATE overwriting.
UPDATE lemon_testpilot.case_execution_batch
SET batch_no = 'RT-20261013161908', updated_at = '2026-10-13 17:33:31'
WHERE id = 2;
UPDATE lemon_testpilot.case_execution_batch
SET batch_no = 'RT-20261013174935', updated_at = '2026-10-13 17:49:54'
WHERE id = 3;

-- Verify
SELECT id, batch_no, title, status, total, created_at, updated_at
FROM lemon_testpilot.case_execution_batch
ORDER BY id;
