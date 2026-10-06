-- ============================================================
-- TestPilot 个人 QA 工作台 · V2 种子数据（贴近手册截图演示）
-- ============================================================

-- 三级模块结构
INSERT INTO sys_module (id, project_name, module_name, submodule_name, enabled, updated_at) VALUES
(1, '测试平台', '质量工具', '任务编排', 1, '2026-10-02 09:00:00'),
(2, '电商平台', '支付中心', '支付回调', 1, '2026-10-02 09:00:00'),
(3, '电商平台', '支付中心', '订单同步', 1, '2026-10-02 09:00:00'),
(4, '电商平台', '订单中心', '优惠券', 1, '2026-10-02 09:00:00'),
(5, '电商平台', '账户中心', '会员账户', 1, '2026-10-02 09:00:00');

-- 大模型厂商（默认全部未启用，使用规则引擎降级）
INSERT INTO sys_llm_config (id, vendor_code, vendor_name, base_url, model_name, api_key, temperature, timeout_seconds, max_output, enabled, updated_at) VALUES
(1, 'deepseek', 'DeepSeek', 'https://api.deepseek.com/v1', 'deepseek-chat', '', 0.20, 60, 4096, 0, '2026-10-02 09:00:00'),
(2, 'openai', 'OpenAI', 'https://api.openai.com/v1', 'gpt-5-mini', '', 0.20, 60, 4096, 0, '2026-10-02 09:00:00'),
(3, 'custom', '自定义兼容接口', '', '', '', 0.20, 60, 4096, 0, '2026-10-02 09:00:00'),
(4, 'qwen', '通义千问', 'https://dashscope.aliyuncs.com/compatible-mode/v1', 'qwen-plus', '', 0.20, 60, 4096, 0, '2026-10-02 09:00:00');

-- 输出格式模板
INSERT INTO sys_output_template (task_type, field_name, field_label, required_flag, sort_order, visible) VALUES
('testcase_gen', 'title', '标题', 1, 1, 1),
('testcase_gen', 'module', '模块', 1, 2, 1),
('testcase_gen', 'preconditions', '前置条件', 1, 3, 1),
('testcase_gen', 'steps', '测试步骤', 0, 4, 1),
('testcase_gen', 'expected', '预期结果', 0, 5, 1),
('testcase_gen', 'priority', '优先级', 0, 6, 1),
('testcase_gen', 'case_type', '用例类型', 0, 7, 1),
('testcase_gen', 'test_data', '测试数据', 0, 8, 1),
('bug_analysis', 'phenomenon', '问题现象', 1, 1, 1),
('bug_analysis', 'repro_steps', '复现步骤', 1, 2, 1),
('bug_analysis', 'expected_result', '预期结果', 1, 3, 1),
('bug_analysis', 'risk_level', '风险等级', 0, 4, 1),
('bug_analysis', 'affected_modules', '影响模块', 0, 5, 1),
('bug_analysis', 'evidence_chain', '证据链', 0, 6, 1),
('bug_analysis', 'possible_causes', '可能原因', 0, 7, 1),
('bug_analysis', 'trouble_path', '排查路径', 0, 8, 1),
('bug_analysis', 'regression_scope', '回归范围', 0, 9, 1),
('log_triage', 'time_range', '时间范围', 1, 1, 1),
('log_triage', 'log_context', '日志上下文', 1, 2, 1),
('log_triage', 'error_keywords', '异常关键词', 1, 3, 1),
('log_triage', 'anomaly_summary', '异常摘要', 0, 4, 1),
('log_triage', 'timeline', '关键时间线', 0, 5, 1),
('log_triage', 'error_fragments', '错误片段', 0, 6, 1),
('log_triage', 'possible_causes', '可能原因', 0, 7, 1),
('log_triage', 'suggestions', '排查建议', 0, 8, 1),
('sql_analysis', 'sql_text', 'SQL 语句', 1, 1, 1),
('sql_analysis', 'goal', '分析目标', 1, 2, 1),
('sql_analysis', 'sql_correctness', 'SQL 正确性', 0, 3, 1),
('sql_analysis', 'performance_risk', '性能风险', 0, 4, 1),
('sql_analysis', 'impact_scope', '影响范围', 0, 5, 1),
('sql_analysis', 'optimization_suggestions', '优化建议', 0, 6, 1),
('sql_analysis', 'review_conclusion', '审核结论', 0, 7, 1),
('regression_list', 'version_info', '版本信息', 1, 1, 1),
('regression_list', 'regression_scope', '回归范围', 1, 2, 1),
('regression_list', 'regression_items', '回归项', 0, 3, 1),
('regression_list', 'priority', '优先级', 0, 4, 1),
('regression_list', 'source', '来源', 0, 5, 1),
('test_report', 'version_info', '版本信息', 1, 1, 1),
('test_report', 'conclusion_source', '结论来源', 1, 2, 1),
('test_report', 'exec_result', '执行结果', 0, 3, 1),
('test_report', 'risk_notes', '风险与遗留', 0, 4, 1),
('test_report', 'conclusion', '上线结论', 0, 5, 1),
('prompt_test', 'prompt_text', 'Prompt 文本', 1, 1, 1),
('prompt_test', 'eval_criteria', '评估标准', 1, 2, 1),
('prompt_test', 'eval_result', '评估结果', 0, 3, 1);

-- 任务关联规则
INSERT INTO sys_task_rule (main_task_type, aux_task_type, sort_order, enabled) VALUES
('bug_analysis', 'log_triage', 1, 1),
('bug_analysis', 'sql_analysis', 2, 1),
('bug_analysis', 'regression_list', 3, 1),
('log_triage', 'sql_analysis', 1, 1),
('sql_analysis', 'regression_list', 1, 1),
('testcase_gen', 'regression_list', 1, 1),
('test_report', 'regression_list', 1, 1);

-- 知识库
INSERT INTO kb_item (id, kb_no, title, category, project_name, module_name, submodule_name, risk, version, status, body, source_task, review_note, ref_count, created_at, updated_at) VALUES
(1, 'kb_001', '支付回调重复消费导致重复扣款', '历史 Bug 库', '电商平台', '支付中心', '支付回调', 'P0', 2, '已发布', '三方回调未做幂等，重复消费导致重复扣款；修复后必须回归：成功、失败、重复回调、流水、优惠券和退款。', 'task/task_bug_001', '', 2, '2026-10-02 10:00:00', '2026-10-02 13:00:38'),
(2, 'kb_002', '支付回调日志关键字', '日志规律库', '电商平台', '支付中心', '支付回调', 'P1', 1, '已发布', '支付回调链路异常关键字：Lock wait timeout、duplicate consume、MQ retry backlog；命中时优先检查幂等表唯一索引与事务边界。', 'task/task_log_001', '', 1, '2026-10-02 10:05:00', '2026-10-02 13:00:38'),
(3, 'kb_003', '订单与支付流水一致性检查', 'SQL 经验库', '电商平台', '支付中心', '订单同步', 'P1', 1, '待审核', '订单状态与支付流水一致性检查 SQL：按 traceId 与时间范围对齐 order.status 与 pay_flow.status，关注状态机漏流转与事务提交顺序。', 'task/task_sql_001', '', 0, '2026-10-02 10:10:00', '2026-10-02 13:00:38'),
(4, 'kb_004', '支付回调改动回归规则', '回归规则库', '电商平台', '支付中心', '支付回调', 'P0', 1, '已发布', '必回归成功、失败、重复回调、流水、优惠券和退款。', '人工维护', '暂无审核备注', 1, '2026-10-02 10:15:00', '2026-10-02 13:00:38');

-- 测试用例
INSERT INTO test_case (id, case_no, title, project_name, module_name, submodule_name, priority, type, preconditions, steps, expected, test_data, status, version, linked_bug_no, created_date, updated_at) VALUES
(1, 'TC-PAY-001', '支付回调幂等校验', '电商平台', '支付中心', '支付回调', 'P0', '接口', '已存在支付成功流水', '1. 发送支付成功回调\n2. 重复发送相同回调', '订单状态为已支付且只扣款一次', 'traceId=demo-1024', '通过', 2, '', '2026-10-02', '2026-10-02 12:00:00'),
(2, 'TC-PAY-002', '支付成功后订单状态同步', '电商平台', '支付中心', '订单同步', 'P1', '功能', '订单为待支付状态', '1. 完成支付\n2. 等待 30 秒查询订单\n3. 核对订单状态', '订单状态为已支付', 'traceId=demo-1024', '失败', 1, 'bug_0001', '2026-10-02', '2026-10-02 12:10:00'),
(3, 'TC-PAY-003', '三方回调签名异常', '电商平台', '支付中心', '支付回调', 'P0', '异常', '准备错误签名', '1. 发送异常回调', '拒绝处理并记录安全日志', '错误 sign', '阻塞', 1, '', '2026-10-02', '2026-10-02 12:20:00'),
(4, 'TC-COUPON-001', '取消订单返还优惠券', '电商平台', '订单中心', '优惠券', 'P1', '回归', '下单使用优惠券', '1. 取消订单\n2. 查询优惠券状态', '优惠券返还且状态可用', 'coupon_id=c-1024', '未执行', 3, '', '2026-10-02', '2026-10-02 12:30:00');

-- 分析任务记录
INSERT INTO task_record (id, task_no, title, task_type, project_name, module_name, submodule_name, risk, context, status, review_status, completeness, output_json, engine_mode, source, created_at, updated_at) VALUES
(1, 'task_bug_001', '支付成功但订单状态未同步', 'bug_analysis', '电商平台', '支付中心', '订单同步', 'P0', '支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单；实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。', '已完成', '通过', 100, '{"risk_level":"P0","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围","SQL 文本"],"phenomenon":"支付成功后订单仍为待支付（staging 环境 v2.8.3）","affected_modules":["电商平台 / 支付中心 / 订单同步"],"possible_causes":["从现象推测存在状态同步或事务边界异常，尚需证据验证","Lock wait timeout 指向数据库锁等待，可能阻塞订单状态更新事务"],"recommended_logs":["按 traceId=demo-1024 检索支付回调与订单同步全链路日志","检查回调消费与订单更新的事务边界日志"],"recommended_sql":["查询订单状态流转表在时间范围内的变更记录","核对支付流水与订单状态一致性"],"related_history_bugs":["支付回调重复消费导致重复扣款（kb_001）"],"regression_scope":["直接变更模块","上下游状态同步","失败与重复请求场景"]}', 'rule', 'assistant', '2026-10-02 13:10:11', '2026-10-02 13:10:11'),
(2, 'task_log_001', '支付回调消费超时排查', 'log_triage', '电商平台', '支付中心', '支付回调', 'P1', '支付回调消费出现超时，日志出现 Lock wait timeout，traceId=demo-1024。', '已完成', '通过', 67, '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围"],"anomaly_summary":"已提取异常关键词与时间线，根因仍需结合完整上下文确认","timeline":["接收用户证据","定位首个异常片段（Lock wait timeout）","向上下游扩展调用链"],"error_fragments":["日志出现 Lock wait timeout"],"possible_causes":["锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证"],"suggestions":["按 traceId=demo-1024 检索回调消费线程与数据库事务日志","命中 Lock wait timeout 时优先检查幂等表唯一索引与事务粒度（引用日志规律库 kb_002）"],"related_kb":["支付回调日志关键字（kb_002）"]}', 'rule', 'assistant', '2026-10-02 13:08:40', '2026-10-02 13:08:40'),
(3, 'task_sql_001', '订单与支付流水一致性分析', 'sql_analysis', '电商平台', '支付中心', '订单同步', 'P1', '支付成功后订单状态未同步，需要给出 SQL 排查方向：核对订单状态与支付流水一致性。', '已完成', '待复核', 50, '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["SQL 文本"],"sql_correctness":"未提供 SQL 文本，暂无法进行语法与正确性检查","performance_risk":"缺少执行计划与表结构信息，无法评估索引命中情况","impact_scope":["支付中心 / 订单同步","上下游状态同步","历史数据回溯范围"],"optimization_suggestions":["补充 SQL 文本与执行计划后评估索引与锁行为","按 traceId 与时间范围对齐订单状态与支付流水"],"review_conclusion":"证据不足，需人工复核后给出最终结论","completeness_score":50}', 'rule', 'assistant', '2026-10-02 13:16:24', '2026-10-02 13:16:24'),
(4, 'task_case_001', '优惠券叠加规则用例生成', 'testcase_gen', '电商平台', '订单中心', '优惠券', 'P1', '需求：优惠券叠加规则。验收标准：可叠加券按规则计算金额，互斥券拦截，取消订单返还优惠券。', '已完成', '通过', 100, '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":[],"test_points":["优惠券叠加规则主流程","叠加互斥与边界","取消订单返还优惠券","退款场景优惠券回收"],"cases":[{"title":"多优惠券叠加下单校验","priority":"P1","type":"功能","preconditions":"账户有多张可用优惠券","steps":"1. 选择可叠加优惠券下单\n2. 提交订单","expected":"叠加金额按规则计算"},{"title":"互斥券叠加拦截","priority":"P1","type":"异常","preconditions":"账户有互斥优惠券","steps":"1. 同时选择互斥券下单","expected":"拦截并提示互斥规则"},{"title":"取消订单返还优惠券","priority":"P1","type":"回归","preconditions":"下单已使用优惠券","steps":"1. 取消订单\n2. 查询优惠券状态","expected":"优惠券返还且状态可用"}],"coverage":"正常 / 异常 / 边界"}', 'rule', 'assistant', '2026-10-02 13:12:05', '2026-10-02 13:12:05');

-- 执行记录
INSERT INTO run_record (id, run_no, title, main_task, route_count, step_count, risk, status, missing_count, review_status, input_text, project_name, module_name, submodule_name, engine_mode, created_at, updated_at) VALUES
(1, 'run_31d4e0dc', '支付成功后订单仍为待支付，staging 环境 v2.8.3', 'bug_analysis', 4, 31, 'P1', '待补充', 2, '待复核', '支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单；实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。请给出 SQL 排查方向和回归范围。', '电商平台', '支付中心', '订单同步', 'rule', '2026-10-02 13:16:24', '2026-10-02 13:16:24'),
(2, 'run_a80c7c94', '支付成功后订单仍为待支付，staging 环境 v2.8.3', 'testcase_gen', 5, 37, 'P1', '待补充', 3, '待复核', '支付成功后订单仍为待支付，staging 环境 v2.8.3。请生成回归用例并给出验收标准与测试点。', '电商平台', '支付中心', '订单同步', 'rule', '2026-10-02 13:11:13', '2026-10-02 13:11:13'),
(3, 'run_82589fc6', '支付成功后订单仍为待支付，staging 环境 v2.8.3', 'bug_analysis', 4, 31, 'P1', '待补充', 2, '待复核', '支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单；实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。', '电商平台', '支付中心', '订单同步', 'rule', '2026-10-02 13:11:03', '2026-10-02 13:11:03');

-- 执行任务快照（run 1：bug_analysis 主任务 + 3 辅助任务）
INSERT INTO run_task_snapshot (run_id, task_type, task_name, role, seq, completeness, missing_json, output_json) VALUES
(1, 'bug_analysis', 'Bug 分析', 'main', 1, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":[],"phenomenon":"支付成功后订单仍为待支付（staging 环境 v2.8.3）","affected_modules":["电商平台 / 支付中心 / 订单同步"],"possible_causes":["从现象推测存在状态同步或事务边界异常，尚需证据验证","Lock wait timeout 指向数据库锁等待，可能阻塞订单状态更新事务"],"recommended_logs":["按 traceId=demo-1024 检索支付回调与订单同步全链路日志","检查回调消费与订单更新的事务边界日志"],"recommended_sql":["查询订单状态流转表在时间范围内的变更记录","核对支付流水与订单状态一致性"],"related_history_bugs":["支付回调重复消费导致重复扣款（kb_001）"],"regression_scope":["直接变更模块","上下游状态同步","失败与重复请求场景"]}'),
(1, 'log_triage', '日志排查', 'aux', 2, 67, '["时间范围"]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围"],"anomaly_summary":"已提取异常关键词与时间线，根因仍需结合完整上下文确认","timeline":["接收用户证据","定位首个异常片段（Lock wait timeout）","向上下游扩展调用链"],"error_fragments":["日志出现 Lock wait timeout"],"possible_causes":["锁等待可能由幂等表唯一索引冲突或长事务导致，尚需证据验证"],"suggestions":["按 traceId=demo-1024 检索回调消费线程与数据库事务日志","命中 Lock wait timeout 时优先检查幂等表唯一索引与事务粒度（引用日志规律库 kb_002）"],"related_kb":["支付回调日志关键字（kb_002）"]}'),
(1, 'sql_analysis', 'SQL 分析', 'aux', 3, 50, '["SQL 文本"]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["SQL 文本"],"sql_correctness":"未提供 SQL 文本，暂无法进行语法与正确性检查","performance_risk":"缺少执行计划与表结构信息，无法评估索引命中情况","impact_scope":["支付中心 / 订单同步","上下游状态同步","历史数据回溯范围"],"optimization_suggestions":["补充 SQL 文本与执行计划后评估索引与锁行为","按 traceId 与时间范围对齐订单状态与支付流水"],"review_conclusion":"证据不足，需人工复核后给出最终结论","completeness_score":50}'),
(1, 'regression_list', '回归清单', 'aux', 4, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":[],"regression_items":["支付成功回调主流程","支付失败回调处理","重复回调幂等验证","订单状态同步查询","支付流水一致性核对","优惠券返还与退款场景","三方回调签名异常（关联用例 TC-PAY-003）"],"item_count":7,"completion_rate":100,"sources_summary":["高风险用例 2 条","历史 Bug 1 条（kb_001）","回归规则库 1 条（kb_004）"]}');

-- 执行任务快照（run 2：testcase_gen 主任务）
INSERT INTO run_task_snapshot (run_id, task_type, task_name, role, seq, completeness, missing_json, output_json) VALUES
(2, 'testcase_gen', '测试用例生成', 'main', 1, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":[],"test_points":["支付成功主流程","订单状态同步边界","回归范围验证"],"cases":[{"title":"支付成功后订单状态同步校验","priority":"P1","type":"功能","preconditions":"订单为待支付状态","steps":"1. 完成支付\n2. 等待 30 秒查询订单","expected":"订单状态为已支付"},{"title":"支付重复回调幂等校验","priority":"P0","type":"异常","preconditions":"已存在支付成功流水","steps":"1. 重复发送相同回调","expected":"只扣款一次且状态不回退"},{"title":"回调超时重试边界","priority":"P1","type":"边界","preconditions":"回调响应超时","steps":"1. 触发重试\n2. 查询订单状态","expected":"重试幂等且状态一致"}],"coverage":"正常 / 异常 / 边界"}'),
(2, 'bug_analysis', 'Bug 分析', 'aux', 2, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围"],"phenomenon":"支付成功后订单仍为待支付","affected_modules":["电商平台 / 支付中心 / 订单同步"],"possible_causes":["状态同步或事务边界异常，尚需证据验证"],"regression_scope":["直接变更模块","上下游状态同步"]}'),
(2, 'log_triage', '日志排查', 'aux', 3, 33, '["时间范围","日志上下文"]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围","日志上下文"],"anomaly_summary":"未提供日志片段，等待补充材料","timeline":["接收用户证据"],"suggestions":["补充日志上下文后提取时间线"]}'),
(2, 'sql_analysis', 'SQL 分析', 'aux', 4, 50, '["SQL 文本"]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["SQL 文本"],"sql_correctness":"未提供 SQL 文本，暂无法检查","performance_risk":"缺少执行计划信息","review_conclusion":"证据不足，需人工复核"}'),
(2, 'regression_list', '回归清单', 'aux', 5, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":[],"regression_items":["支付成功回调主流程","支付失败回调处理","重复回调幂等验证","订单状态同步查询","支付流水一致性核对","优惠券返还与退款场景"],"item_count":6,"completion_rate":100,"sources_summary":["回归规则库 1 条（kb_004）"]}');

-- 执行任务快照（run 3）
INSERT INTO run_task_snapshot (run_id, task_type, task_name, role, seq, completeness, missing_json, output_json) VALUES
(3, 'bug_analysis', 'Bug 分析', 'main', 1, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围","SQL 文本"],"phenomenon":"支付成功后订单仍为待支付（staging 环境 v2.8.3）","affected_modules":["电商平台 / 支付中心 / 订单同步"],"possible_causes":["状态同步或事务边界异常，尚需证据验证"],"regression_scope":["直接变更模块","上下游状态同步","失败与重复请求场景"]}'),
(3, 'log_triage', '日志排查', 'aux', 2, 67, '["时间范围"]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["时间范围"],"anomaly_summary":"已提取异常关键词与时间线，根因仍需结合完整上下文确认","timeline":["接收用户证据","定位首个异常片段（Lock wait timeout）","向上下游扩展调用链"],"suggestions":["按 traceId=demo-1024 检索全链路日志"]}'),
(3, 'sql_analysis', 'SQL 分析', 'aux', 3, 50, '["SQL 文本"]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":["SQL 文本"],"sql_correctness":"未提供 SQL 文本，暂无法检查","review_conclusion":"证据不足，需人工复核"}'),
(3, 'regression_list', '回归清单', 'aux', 4, 100, '[]', '{"risk_level":"P1","human_review_points":["确认事实与引用依据","P0/P1 风险和上线结论必须由测试人最终确认"],"need_more_info":[],"regression_items":["支付成功回调主流程","支付失败回调处理","重复回调幂等验证","订单状态同步查询","支付流水一致性核对","优惠券返还与退款场景"],"item_count":6,"completion_rate":100,"sources_summary":["回归规则库 1 条（kb_004）"]}');

-- 执行步骤（run 1：3 公共 + 4 任务 × 6 步 + 4 收尾 = 31 步）
INSERT INTO run_step (run_id, seq, task_type, step_name, status, detail) VALUES
(1, 1, '', '接收输入材料', 'done', '接收整段材料与所选模块'),
(1, 2, '', '识别主任务与辅助任务', 'done', 'bug_analysis → log_triage → sql_analysis → regression_list'),
(1, 3, '', '匹配知识库引用', 'done', '命中 kb_001、kb_002、kb_004'),
(1, 4, 'bug_analysis', '校验输入', 'done', '完整度 100%'),
(1, 5, 'bug_analysis', '还原现象', 'done', '提取现象与实际/预期结果'),
(1, 6, 'bug_analysis', '匹配历史Bug', 'done', '命中历史 Bug 库 kb_001'),
(1, 7, 'bug_analysis', '定位排查路径', 'done', '输出日志与 SQL 排查方向'),
(1, 8, 'bug_analysis', '输出回归范围', 'done', '直接变更模块、上下游状态同步等'),
(1, 9, 'bug_analysis', '标记人工复核', 'done', 'P0/P1 风险需人工确认'),
(1, 10, 'log_triage', '校验输入', 'done', '完整度 67%，缺：时间范围'),
(1, 11, 'log_triage', '提取时间线', 'done', '生成关键时间线'),
(1, 12, 'log_triage', '定位异常片段', 'done', 'Lock wait timeout'),
(1, 13, 'log_triage', '扩展调用链', 'done', '向上下游扩展'),
(1, 14, 'log_triage', '输出排查建议', 'done', '引用日志规律库 kb_002'),
(1, 15, 'log_triage', '标记人工复核', 'done', '根因需证据确认'),
(1, 16, 'sql_analysis', '校验输入', 'done', '完整度 50%，缺：SQL 文本'),
(1, 17, 'sql_analysis', '语法与正确性检查', 'done', '缺少 SQL 文本，明确标记待补充'),
(1, 18, 'sql_analysis', '性能风险识别', 'done', '缺少执行计划信息'),
(1, 19, 'sql_analysis', '影响范围评估', 'done', '订单同步上下游'),
(1, 20, 'sql_analysis', '输出优化建议', 'done', '补充后评估索引与锁行为'),
(1, 21, 'sql_analysis', '标记人工复核', 'done', '证据不足需人工复核'),
(1, 22, 'regression_list', '校验输入', 'done', '完整度 100%'),
(1, 23, 'regression_list', '收集高风险用例', 'done', 'TC-PAY-003 等 2 条'),
(1, 24, 'regression_list', '收集历史Bug', 'done', 'kb_001'),
(1, 25, 'regression_list', '合并去重排序', 'done', 'P0 优先'),
(1, 26, 'regression_list', '生成清单', 'done', '7 项必回归'),
(1, 27, 'regression_list', '标记人工复核', 'done', 'P0 项需人工确认'),
(1, 28, '', '记录缺项提示', 'done', '时间范围、SQL 文本'),
(1, 29, '', '安全规则校验', 'done', '五条全局安全规则全部通过'),
(1, 30, '', '生成执行快照', 'done', '保留 4 个任务输出快照'),
(1, 31, '', '等待人工复核', 'done', 'AI 不做最终上线判断');

-- 执行步骤（run 2：3 公共 + 5 任务 × 6 步 + 4 收尾 = 37 步）
INSERT INTO run_step (run_id, seq, task_type, step_name, status, detail) VALUES
(2, 1, '', '接收输入材料', 'done', '接收整段材料与所选模块'),
(2, 2, '', '识别主任务与辅助任务', 'done', 'testcase_gen → bug_analysis → log_triage → sql_analysis → regression_list'),
(2, 3, '', '匹配知识库引用', 'done', '命中 kb_004'),
(2, 4, 'testcase_gen', '校验输入', 'done', '完整度 100%'),
(2, 5, 'testcase_gen', '拆解测试点', 'done', '主流程/边界/回归 3 类'),
(2, 6, 'testcase_gen', '覆盖正常/异常/边界', 'done', '三类场景全覆盖'),
(2, 7, 'testcase_gen', '生成结构化用例', 'done', '3 条结构化用例'),
(2, 8, 'testcase_gen', '补充回归标记', 'done', '标记回归场景'),
(2, 9, 'testcase_gen', '标记人工复核', 'done', '生成结果需人工确认'),
(2, 10, 'bug_analysis', '校验输入', 'done', '完整度 100%'),
(2, 11, 'bug_analysis', '还原现象', 'done', '提取现象'),
(2, 12, 'bug_analysis', '匹配历史Bug', 'done', '未命中，不编造引用'),
(2, 13, 'bug_analysis', '定位排查路径', 'done', '输出排查方向'),
(2, 14, 'bug_analysis', '输出回归范围', 'done', '基础回归范围'),
(2, 15, 'bug_analysis', '标记人工复核', 'done', '需人工确认'),
(2, 16, 'log_triage', '校验输入', 'done', '完整度 33%，缺：时间范围、日志上下文'),
(2, 17, 'log_triage', '提取时间线', 'done', '材料不足仅保留接收步骤'),
(2, 18, 'log_triage', '定位异常片段', 'done', '无日志片段，标记待补充'),
(2, 19, 'log_triage', '扩展调用链', 'done', '等待补充材料'),
(2, 20, 'log_triage', '输出排查建议', 'done', '建议补充日志上下文'),
(2, 21, 'log_triage', '标记人工复核', 'done', '证据不足需人工复核'),
(2, 22, 'sql_analysis', '校验输入', 'done', '完整度 50%，缺：SQL 文本'),
(2, 23, 'sql_analysis', '语法与正确性检查', 'done', '缺少 SQL 文本'),
(2, 24, 'sql_analysis', '性能风险识别', 'done', '缺少执行计划'),
(2, 25, 'sql_analysis', '影响范围评估', 'done', '订单同步上下游'),
(2, 26, 'sql_analysis', '输出优化建议', 'done', '补充后评估'),
(2, 27, 'sql_analysis', '标记人工复核', 'done', '需人工复核'),
(2, 28, 'regression_list', '校验输入', 'done', '完整度 100%'),
(2, 29, 'regression_list', '收集高风险用例', 'done', 'P0 用例'),
(2, 30, 'regression_list', '收集历史Bug', 'done', '无命中'),
(2, 31, 'regression_list', '合并去重排序', 'done', '按优先级排序'),
(2, 32, 'regression_list', '生成清单', 'done', '6 项必回归'),
(2, 33, 'regression_list', '标记人工复核', 'done', 'P0 项需人工确认'),
(2, 34, '', '记录缺项提示', 'done', '时间范围、日志上下文、SQL 文本'),
(2, 35, '', '安全规则校验', 'done', '五条全局安全规则全部通过'),
(2, 36, '', '生成执行快照', 'done', '保留 5 个任务输出快照'),
(2, 37, '', '等待人工复核', 'done', 'AI 不做最终上线判断');

-- 执行步骤（run 3：与 run 1 同结构 31 步，摘要记录）
INSERT INTO run_step (run_id, seq, task_type, step_name, status, detail) VALUES
(3, 1, '', '接收输入材料', 'done', '接收整段材料与所选模块'),
(3, 2, '', '识别主任务与辅助任务', 'done', 'bug_analysis → log_triage → sql_analysis → regression_list'),
(3, 3, '', '匹配知识库引用', 'done', '命中 kb_001、kb_002、kb_004'),
(3, 4, 'bug_analysis', '校验输入', 'done', '完整度 100%'),
(3, 5, 'bug_analysis', '还原现象', 'done', '提取现象与实际/预期结果'),
(3, 6, 'bug_analysis', '匹配历史Bug', 'done', '命中历史 Bug 库 kb_001'),
(3, 7, 'bug_analysis', '定位排查路径', 'done', '输出日志与 SQL 排查方向'),
(3, 8, 'bug_analysis', '输出回归范围', 'done', '直接变更模块、上下游状态同步等'),
(3, 9, 'bug_analysis', '标记人工复核', 'done', 'P0/P1 风险需人工确认'),
(3, 10, 'log_triage', '校验输入', 'done', '完整度 67%，缺：时间范围'),
(3, 11, 'log_triage', '提取时间线', 'done', '生成关键时间线'),
(3, 12, 'log_triage', '定位异常片段', 'done', 'Lock wait timeout'),
(3, 13, 'log_triage', '扩展调用链', 'done', '向上下游扩展'),
(3, 14, 'log_triage', '输出排查建议', 'done', '引用日志规律库 kb_002'),
(3, 15, 'log_triage', '标记人工复核', 'done', '根因需证据确认'),
(3, 16, 'sql_analysis', '校验输入', 'done', '完整度 50%，缺：SQL 文本'),
(3, 17, 'sql_analysis', '语法与正确性检查', 'done', '缺少 SQL 文本，明确标记待补充'),
(3, 18, 'sql_analysis', '性能风险识别', 'done', '缺少执行计划信息'),
(3, 19, 'sql_analysis', '影响范围评估', 'done', '订单同步上下游'),
(3, 20, 'sql_analysis', '输出优化建议', 'done', '补充后评估索引与锁行为'),
(3, 21, 'sql_analysis', '标记人工复核', 'done', '证据不足需人工复核'),
(3, 22, 'regression_list', '校验输入', 'done', '完整度 100%'),
(3, 23, 'regression_list', '收集高风险用例', 'done', 'P0 用例'),
(3, 24, 'regression_list', '收集历史Bug', 'done', 'kb_001'),
(3, 25, 'regression_list', '合并去重排序', 'done', 'P0 优先'),
(3, 26, 'regression_list', '生成清单', 'done', '6 项必回归'),
(3, 27, 'regression_list', '标记人工复核', 'done', 'P0 项需人工确认'),
(3, 28, '', '记录缺项提示', 'done', '时间范围、SQL 文本'),
(3, 29, '', '安全规则校验', 'done', '五条全局安全规则全部通过'),
(3, 30, '', '生成执行快照', 'done', '保留 4 个任务输出快照'),
(3, 31, '', '等待人工复核', 'done', 'AI 不做最终上线判断');

-- 回归清单
INSERT INTO regression_list (id, title, version, status, progress, created_date, updated_at) VALUES
(1, '支付回调优化回归清单', 'v2.8.3', '执行中', 67, '2026-10-02', '2026-10-02 13:00:00');

INSERT INTO regression_item (list_id, seq, title, priority, source_type, source_ref, module_label, status) VALUES
(1, 1, '支付回调幂等校验', 'P0', 'case', 'TC-PAY-001', '支付中心 / 支付回调 · 关联用例', '通过'),
(1, 2, '订单状态同步', 'P1', 'case', 'TC-PAY-002', '支付中心 / 订单同步 · 关联用例', '失败'),
(1, 3, '重复回调历史缺陷验证', 'P0', 'bug', 'kb_001', '支付中心 / 支付回调 · 关联 Bug', '未执行');

-- 测试报告
INSERT INTO test_report (id, report_no, title, project_name, version, modules, conclusion, pass_count, fail_count, block_count, report_date, content_json, source_task_ids, created_at, updated_at) VALUES
(1, 'report_001', '支付回调优化测试报告', '电商平台', 'v2.8.3', '支付回调、订单同步', '有条件上线', 18, 1, 1, '2026-10-02', '{"summary":"支付回调优化（v2.8.3）测试汇总：重点覆盖支付回调幂等、订单状态同步与流水一致性；存在 1 项失败与 1 项阻塞。","sources":[{"type":"SQL 分析","title":"订单与支付流水一致性分析","risk":"P1"},{"type":"Bug 分析","title":"支付成功但订单状态未同步","risk":"P0"}],"riskNotes":["订单状态同步存在偶发失败"],"suggestion":"有条件上线","manualReview":"上线结论需测试人最终确认，AI 不批准上线"}', '1,3', '2026-10-02 13:20:00', '2026-10-02 13:20:00');

-- 工作台入口
INSERT INTO workbench_entry (id, task_type, name, icon, version, published) VALUES
(1, 'testcase_gen', '测试用例生成', 'list', 1, 1),
(2, 'bug_analysis', 'Bug 分析', 'warning', 1, 1),
(3, 'log_triage', '日志排查', 'document', 1, 1),
(4, 'sql_analysis', 'SQL 分析', 'database', 1, 1),
(5, 'regression_list', '回归清单', 'clipboard', 1, 1),
(6, 'test_report', '测试报告', 'report', 1, 1),
(7, 'prompt_test', 'Prompt 测试', 'edit', 1, 1);

-- 工作台入口配置 v1
INSERT INTO workbench_entry_config (entry_id, task_type, version, trigger_words, required_fields, kb_mappings, workflow_steps, published_at) VALUES
(1, 'testcase_gen', 1, '需求\n验收\n用例\n场景\n测试点', '需求背景\n验收标准', '业务规则库\n接口异常库\n历史 Bug 库', '校验输入\n拆解测试点\n覆盖正常/异常/边界\n生成结构化用例\n标记人工复核', '2026-10-02 09:00:00'),
(2, 'bug_analysis', 1, 'bug\n缺陷\n现象\n复现\n实际结果\n预期', '问题现象\n复现步骤\n预期结果', '历史 Bug 库\n日志规律库\nSQL 经验库\n业务规则库', '校验输入\n还原现象\n匹配历史Bug\n定位排查路径\n输出回归范围\n标记人工复核', '2026-10-02 09:00:00'),
(3, 'log_triage', 1, '日志\ntraceId\n超时\ntimeout\n异常堆栈', '时间范围\n日志上下文\n异常关键词', '日志规律库\n历史 Bug 库', '校验输入\n提取时间线\n定位异常片段\n扩展调用链\n输出排查建议\n标记人工复核', '2026-10-02 09:00:00'),
(4, 'sql_analysis', 1, 'sql\n慢查询\n索引\n一致性\n执行计划\n排查', 'SQL 文本\n分析目标', 'SQL 经验库\n业务规则库', '校验输入\n语法与正确性检查\n性能风险识别\n影响范围评估\n输出优化建议\n标记人工复核', '2026-10-02 09:00:00'),
(5, 'regression_list', 1, '回归\n回归范围\n上线前\n发布', '版本信息\n回归范围', '回归规则库\n历史 Bug 库', '校验输入\n收集高风险用例\n收集历史Bug\n合并去重排序\n生成清单\n标记人工复核', '2026-10-02 09:00:00'),
(6, 'test_report', 1, '测试报告\n质量结论\n上线结论', '版本信息\n结论来源', '回归规则库\n业务规则库', '校验输入\n汇总用例与回归\n聚合分析结论\n生成风险与遗留\n输出统一结论\n标记人工复核', '2026-10-02 09:00:00'),
(7, 'prompt_test', 1, 'prompt\n提示词\n模板', 'Prompt 文本\n评估标准', '业务规则库', '校验输入\n执行Prompt\n对比预期\n输出评估\n标记人工复核', '2026-10-02 09:00:00');

-- 案例验证
INSERT INTO validation_case (id, case_no, title, material, expected_routes, expected_checks, last_run_at, last_result, last_routes) VALUES
(1, 'val_payment', '支付成功但订单未支付 • 完整材料', '支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单，实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。请给出 sql 排查方向和回归范围。', 'bug_analysis,log_triage,sql_analysis,regression_list', '不跳结论\n根因需证据\n至少 6 个必回归项\nP0/P1 人工复核', '2026-10-02 13:18:04', '通过', 'bug_analysis → log_triage → sql_analysis → regression_list');
