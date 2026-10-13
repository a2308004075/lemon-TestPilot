-- 回归清单：记录生成输入材料（变更内容与影响模块），便于追溯每次生成的分析依据
ALTER TABLE regression_list
    ADD COLUMN change_content TEXT COMMENT '变更内容与影响模块（生成输入材料）' AFTER version;
