-- V13 手工登记（本环境无 FlywayRepairPostProcessor，迁移由手工执行后登记）
-- checksum 由 Flyway 8.5 ChecksumCalculator 等价实现计算：-1317901961
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
    (13, '13', 'semantic retrieval', 'SQL', 'V13__semantic_retrieval.sql', -1317901961, 'root', NOW(), 0, 1);
