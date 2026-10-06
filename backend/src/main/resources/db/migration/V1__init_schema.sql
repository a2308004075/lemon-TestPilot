-- ============================================================
-- TestPilot 个人 QA 工作台 · V1 初始化表结构
-- 数据库：lemon_testpilot（由 JDBC createDatabaseIfNotExist 自动创建）
-- ============================================================

-- 1. 三级模块结构（项目/模块/子模块）
CREATE TABLE sys_module (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(100) NOT NULL COMMENT '项目',
    module_name VARCHAR(100) NOT NULL COMMENT '模块',
    submodule_name VARCHAR(100) NOT NULL COMMENT '子模块',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_module (project_name, module_name, submodule_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '三级模块结构';

-- 2. 大模型厂商配置
CREATE TABLE sys_llm_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_code VARCHAR(50) NOT NULL COMMENT '厂商编码',
    vendor_name VARCHAR(100) NOT NULL COMMENT '厂商名称',
    base_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'API Base URL',
    model_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '模型名',
    api_key VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'API Key（仅本地保存）',
    temperature DECIMAL(3, 2) NOT NULL DEFAULT 0.20 COMMENT '温度',
    timeout_seconds INT NOT NULL DEFAULT 60 COMMENT '超时（秒）',
    max_output INT NOT NULL DEFAULT 4096 COMMENT '最大输出',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vendor (vendor_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '大模型厂商配置';

-- 3. 输出格式模板（按任务类型配置字段；删除=隐藏，不物理删除）
CREATE TABLE sys_output_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    field_name VARCHAR(100) NOT NULL COMMENT '字段名',
    field_label VARCHAR(100) NOT NULL COMMENT '字段显示名',
    required_flag TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必填',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    visible TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见（删除字段仅隐藏）'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '输出格式模板';

-- 4. 任务关联规则（主任务 → 辅助任务）
CREATE TABLE sys_task_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    main_task_type VARCHAR(50) NOT NULL COMMENT '主任务类型',
    aux_task_type VARCHAR(50) NOT NULL COMMENT '辅助任务类型',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '顺序',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '任务关联规则';

-- 5. 知识库条目
CREATE TABLE kb_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_no VARCHAR(50) NOT NULL COMMENT '知识编号',
    title VARCHAR(200) NOT NULL COMMENT '知识标题',
    category VARCHAR(50) NOT NULL COMMENT '知识分类',
    project_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '项目',
    module_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '模块',
    submodule_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '子模块',
    risk VARCHAR(10) NOT NULL DEFAULT 'P1' COMMENT '风险等级',
    version INT NOT NULL DEFAULT 1 COMMENT '版本',
    status VARCHAR(20) NOT NULL DEFAULT '待审核' COMMENT '状态：待审核/已发布/已驳回/已撤销',
    body TEXT NOT NULL COMMENT '知识正文',
    source_task VARCHAR(100) NOT NULL DEFAULT '人工维护' COMMENT '来源任务',
    review_note VARCHAR(500) NOT NULL DEFAULT '' COMMENT '审核备注',
    ref_count INT NOT NULL DEFAULT 0 COMMENT '被引用次数（撤销会被阻止）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_kb_no (kb_no),
    KEY idx_kb_category (category),
    KEY idx_kb_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库条目';

-- 6. 测试用例
CREATE TABLE test_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_no VARCHAR(50) NOT NULL COMMENT '用例编号',
    title VARCHAR(200) NOT NULL COMMENT '用例标题',
    project_name VARCHAR(100) NOT NULL DEFAULT '',
    module_name VARCHAR(100) NOT NULL DEFAULT '',
    submodule_name VARCHAR(100) NOT NULL DEFAULT '',
    priority VARCHAR(10) NOT NULL DEFAULT 'P1' COMMENT '优先级 P0/P1/P2',
    type VARCHAR(20) NOT NULL DEFAULT '功能' COMMENT '用例类型',
    preconditions TEXT COMMENT '前置条件',
    steps TEXT COMMENT '测试步骤（每行一步）',
    expected TEXT COMMENT '预期结果',
    test_data TEXT COMMENT '测试数据',
    status VARCHAR(20) NOT NULL DEFAULT '未执行' COMMENT '状态：未执行/通过/失败/阻塞',
    version INT NOT NULL DEFAULT 1 COMMENT '版本',
    linked_bug_no VARCHAR(50) NOT NULL DEFAULT '' COMMENT '关联 Bug',
    created_date DATE COMMENT '生成日期',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_case_no (case_no),
    KEY idx_case_module (module_name, submodule_name),
    KEY idx_case_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '测试用例';

-- 7. 分析任务记录（Bug/日志/SQL/用例生成等单页分析共用）
CREATE TABLE task_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL COMMENT '任务编号',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    project_name VARCHAR(100) NOT NULL DEFAULT '',
    module_name VARCHAR(100) NOT NULL DEFAULT '',
    submodule_name VARCHAR(100) NOT NULL DEFAULT '',
    risk VARCHAR(10) NOT NULL DEFAULT 'P1',
    context TEXT COMMENT '问题上下文',
    status VARCHAR(20) NOT NULL DEFAULT '已完成' COMMENT '状态：待补充/已完成',
    review_status VARCHAR(20) NOT NULL DEFAULT '待复核' COMMENT '复核状态：待复核/通过/驳回补充',
    completeness INT NOT NULL DEFAULT 0 COMMENT '输入完整度%',
    output_json LONGTEXT COMMENT '结构化输出 JSON',
    engine_mode VARCHAR(20) NOT NULL DEFAULT 'rule' COMMENT '引擎模式 rule/llm',
    source VARCHAR(20) NOT NULL DEFAULT 'manual' COMMENT '来源：manual/assistant',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_task_type (task_type),
    KEY idx_task_review (review_status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '分析任务记录';

-- 8. 执行记录（AI 助手一次完整流程）
CREATE TABLE run_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_no VARCHAR(50) NOT NULL COMMENT '执行编号',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    main_task VARCHAR(50) NOT NULL COMMENT '主任务类型',
    route_count INT NOT NULL DEFAULT 0 COMMENT '路由数',
    step_count INT NOT NULL DEFAULT 0 COMMENT '步骤数',
    risk VARCHAR(10) NOT NULL DEFAULT 'P1' COMMENT '风险等级',
    status VARCHAR(20) NOT NULL DEFAULT '已完成' COMMENT '状态：待补充/已完成',
    missing_count INT NOT NULL DEFAULT 0 COMMENT '缺项数',
    review_status VARCHAR(20) NOT NULL DEFAULT '待复核' COMMENT '复核状态：待复核/通过/驳回补充',
    input_text TEXT COMMENT '原始输入',
    project_name VARCHAR(100) NOT NULL DEFAULT '',
    module_name VARCHAR(100) NOT NULL DEFAULT '',
    submodule_name VARCHAR(100) NOT NULL DEFAULT '',
    engine_mode VARCHAR(20) NOT NULL DEFAULT 'rule',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_run_no (run_no),
    KEY idx_run_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '执行记录';

-- 9. 执行记录 · 路由任务快照
CREATE TABLE run_task_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL COMMENT '执行记录ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    role VARCHAR(20) NOT NULL COMMENT '角色：main/aux',
    seq INT NOT NULL COMMENT '顺序',
    completeness INT NOT NULL DEFAULT 0 COMMENT '完整度%',
    missing_json VARCHAR(1000) COMMENT '缺项 JSON',
    output_json LONGTEXT COMMENT '输出 JSON',
    KEY idx_snapshot_run (run_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '执行任务快照';

-- 10. 执行记录 · 步骤明细
CREATE TABLE run_step (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL COMMENT '执行记录ID',
    seq INT NOT NULL COMMENT '顺序',
    task_type VARCHAR(50) NOT NULL DEFAULT '' COMMENT '所属任务类型',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名',
    status VARCHAR(20) NOT NULL DEFAULT 'done' COMMENT '状态 done/skipped',
    detail VARCHAR(500) COMMENT '说明',
    KEY idx_step_run (run_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '执行步骤明细';

-- 11. 回归清单
CREATE TABLE regression_list (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '清单标题',
    version VARCHAR(50) NOT NULL DEFAULT '' COMMENT '版本',
    status VARCHAR(20) NOT NULL DEFAULT '执行中' COMMENT '状态：执行中/已完成',
    progress INT NOT NULL DEFAULT 0 COMMENT '进度%',
    created_date DATE COMMENT '创建日期',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '回归清单';

-- 12. 回归清单项
CREATE TABLE regression_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    list_id BIGINT NOT NULL COMMENT '清单ID',
    seq INT NOT NULL DEFAULT 0 COMMENT '顺序',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    priority VARCHAR(10) NOT NULL DEFAULT 'P1' COMMENT '优先级',
    source_type VARCHAR(20) NOT NULL DEFAULT 'case' COMMENT '来源类型 case/bug/rule',
    source_ref VARCHAR(100) NOT NULL DEFAULT '' COMMENT '来源引用编号',
    module_label VARCHAR(200) NOT NULL DEFAULT '' COMMENT '模块标签',
    status VARCHAR(20) NOT NULL DEFAULT '未执行' COMMENT '状态：未执行/通过/失败/阻塞',
    KEY idx_item_list (list_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '回归清单项';

-- 13. 测试报告
CREATE TABLE test_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_no VARCHAR(50) NOT NULL COMMENT '报告编号',
    title VARCHAR(200) NOT NULL COMMENT '报告标题',
    project_name VARCHAR(100) NOT NULL DEFAULT '电商平台' COMMENT '项目',
    version VARCHAR(50) NOT NULL DEFAULT '' COMMENT '发布版本',
    modules VARCHAR(200) NOT NULL DEFAULT '' COMMENT '覆盖模块',
    conclusion VARCHAR(20) NOT NULL DEFAULT '有条件上线' COMMENT '上线结论',
    pass_count INT NOT NULL DEFAULT 0 COMMENT '通过数',
    fail_count INT NOT NULL DEFAULT 0 COMMENT '失败数',
    block_count INT NOT NULL DEFAULT 0 COMMENT '阻塞数',
    report_date DATE COMMENT '报告日期',
    content_json LONGTEXT COMMENT '报告内容 JSON',
    source_task_ids VARCHAR(500) NOT NULL DEFAULT '' COMMENT '来源任务ID列表',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report_no (report_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '测试报告';

-- 14. 工作台入口
CREATE TABLE workbench_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型编码',
    name VARCHAR(100) NOT NULL COMMENT '入口名称',
    icon VARCHAR(20) NOT NULL DEFAULT '' COMMENT '图标',
    version INT NOT NULL DEFAULT 1 COMMENT '当前版本',
    published TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否已发布',
    UNIQUE KEY uk_entry_type (task_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工作台入口';

-- 15. 工作台入口配置（版本化；历史执行保留当时快照）
CREATE TABLE workbench_entry_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL COMMENT '入口ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型',
    version INT NOT NULL COMMENT '版本号',
    trigger_words VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '触发词（每行一项）',
    required_fields VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '必填信息（每行一项）',
    kb_mappings VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '知识库引用映射（每行一项）',
    workflow_steps VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '工作流步骤（严格顺序，每行一项）',
    published_at DATETIME COMMENT '发布时间',
    UNIQUE KEY uk_entry_version (task_type, version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工作台入口配置版本';

-- 16. 案例验证
CREATE TABLE validation_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_no VARCHAR(50) NOT NULL COMMENT '验证案例编号',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    material TEXT COMMENT '固定材料',
    expected_routes VARCHAR(500) NOT NULL DEFAULT '' COMMENT '预期路由（逗号分隔）',
    expected_checks VARCHAR(1000) NOT NULL DEFAULT '' COMMENT '预期检查（每行一项）',
    last_run_at DATETIME COMMENT '最近验证时间',
    last_result VARCHAR(20) NOT NULL DEFAULT '' COMMENT '最近验证结果：通过/未通过',
    last_routes VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最近实际路由',
    UNIQUE KEY uk_val_case (case_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '案例验证';
