-- Preview: generate new-format case_no (TC-yyyyMMddHHmmss) from updated_at
SELECT id, case_no, updated_at,
       DATE_FORMAT(updated_at, 'TC-%Y%m%d%H%i%s') AS new_no
FROM lemon_testpilot.test_case
ORDER BY id;
