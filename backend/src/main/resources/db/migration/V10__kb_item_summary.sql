-- 知识库条目新增摘要字段，用于已入库经验卡片展示
ALTER TABLE kb_item ADD COLUMN summary TEXT NULL AFTER body;
