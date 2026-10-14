-- Preview: existing batch rows and candidate new batch_no (RT-yyyyMMddHHmmss from created_at)
SELECT id, batch_no, title, status, total, created_at,
       DATE_FORMAT(created_at, 'RT-%Y%m%d%H%i%s') AS new_no
FROM lemon_testpilot.case_execution_batch
ORDER BY id;
