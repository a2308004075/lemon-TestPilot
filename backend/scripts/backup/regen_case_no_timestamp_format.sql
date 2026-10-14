-- Regenerate case_no to new timestamp format (TC-yyyyMMddHHmmss).
-- Backup: test_case_before_case_no_timestamp.sql (mysqldump, full table)
-- id 1~3 share the same updated_at second, so +0/+1/+2 seconds keep them
-- unique (uk_case_no) and monotonic with id. updated_at is pinned to its
-- original value to avoid ON UPDATE CURRENT_TIMESTAMP overwriting history.
UPDATE lemon_testpilot.test_case SET case_no = 'TC-20261013170947', updated_at = '2026-10-13 17:09:47' WHERE id = 1;
UPDATE lemon_testpilot.test_case SET case_no = 'TC-20261013170948', updated_at = '2026-10-13 17:09:47' WHERE id = 2;
UPDATE lemon_testpilot.test_case SET case_no = 'TC-20261013170949', updated_at = '2026-10-13 17:09:47' WHERE id = 3;
UPDATE lemon_testpilot.test_case SET case_no = 'TC-20261013171907', updated_at = '2026-10-13 17:19:07' WHERE id = 4;

-- Verify
SELECT id, case_no, title, version, updated_at FROM lemon_testpilot.test_case ORDER BY id;
