# TestPilot 个人 QA 工作台 — 开发变更报告

> 依据 docs 手册截图完成的 13 模块全量开发。后端 Spring Boot 2.7.18（Java 8，端口 3344），前端 Vue 3 + Vite + Element Plus（端口 5173），数据库 MySQL `lemon_testpilot`。

## 一、交付范围

| 层 | 内容 |
|---|---|
| 数据库 | 16 张表，Flyway V1 建表 + V2 种子数据 + V3 补列迁移，首次启动自动建库建表灌数据 |
| 后端 | engine 核心引擎（5 类）、service 业务层（11 个）、web REST 控制器（11 个）、entity/repository（16 表） |
| 前端 | 布局（深色侧边栏 13 菜单 + 顶栏 DB/LLM 状态）+ 13 个页面 + 共享分析页组件 |
| 脚本 | 一键启动（start-all.bat）、前后端单独启动、后端停止 |

## 二、13 个模块（与手册截图一一对应）

1. **工作台首页**（Dashboard）— 问候横幅、6 状态卡、快捷入口、数据资产、最近任务
2. **AI 测试助手**（Assistant）— 7 任务入口、智能输入识别、执行前完整度检查、执行结果 JSON、人工复核（驳回补充/确认通过）、5 条全局安全规则徽章
3. **执行记录**（Runs）— 列表搜索分页、详情抽屉（路由快照筛选 + 步骤明细 + 复核）
4. **用例库**（Cases）— 模块树统计、优先级/状态徽章、新增/编辑升版/复制、AI 智能生成草稿
5. **Bug 分析**（BugAnalysis）— 表单 + 实时完整度 + 输出结构说明 + 任务列表 + 已入库经验
6. **日志分析**（LogAnalysis）— 同上（共享 AnalysisPage 组件）
7. **SQL 分析**（SqlAnalysis）— 同上
8. **回归测试**（Regression）— 清单卡进度环、生成回归（高风险用例+历史 Bug）、状态流转、CSV 下载/复制
9. **测试报告**（Reports）— 勾选来源聚合生成、结论规则（全通过=可上线/有失败=有条件/有阻塞=不可上线）、Markdown 下载/复制/打印 PDF
10. **知识库**（Knowledge）— 统计卡、审核入库、撤销（被引用时阻止）、引用关系
11. **工作台编排**（Workbench）— 7 入口的触发词/必填信息/知识库映射/工作流步骤编辑，保存即发布新版本
12. **案例验证**（Validation）— 固定 walkthrough 材料验证路由识别与安全规则
13. **配置中心**（Config）— 项目模块树（启停）、大模型厂商（OpenAI 兼容 + 测试连接）、格式模板（全量同步）、关联规则

## 三、核心引擎（com.testpilot.engine）

- **TaskRouter**：输入文本触发词计分 → 主任务（最高分）+ 辅助任务（得分降序，同分按 TaskTypes.ORDER 决胜）
- **CompletenessChecker**：按入口 required_fields 计算完整度百分比与缺项列表
- **RuleBasedAnalyzer**：7 种任务类型的结构化 JSON 输出，仅引用真实已发布知识库条目
- **LlmClient**：OpenAI 兼容 /chat/completions；未启用或调用失败自动降级规则引擎（engineMode=rule/llm）
- **TaskExecutor**：按 workflow_steps 逐步执行生成 run_step，聚合快照落库；全局安全规则硬编码进输出（不跳步骤、证据不足明确标记、不编造引用、AI 不批准上线、P0/P1 与 Prompt 通过需人工复核）

## 四、数据库变更

- `V1__init_schema.sql`：16 张表（sys_module、sys_llm_config、sys_output_template、sys_task_rule、kb_item、test_case、task_record、run_record、run_task_snapshot、run_step、regression_list、regression_item、test_report、workbench_entry、workbench_entry_config、validation_case）
- `V2__seed_data.sql`：三级模块树、4 知识、4 用例、3 执行记录、4 分析任务、1 回归清单、1 测试报告、7 工作台入口 v1、验证案例 val_payment、4 家 LLM 厂商（未启用）
- `V3__add_missing_created_at.sql`：test_case / regression_list 补 `created_at` 列（仅 ADD COLUMN，不动存量数据）

约定已遵守：全部走 Flyway 迁移；无 DELETE/TRUNCATE/DROP；种子仅 INSERT；密码只存 application-local.yml（.gitignore，提供 example）。

## 五、REST API（/api 前缀，统一 ApiResponse{code,message,data}）

- Dashboard：`GET /dashboard/stats`
- Assistant：`GET/POST /assistant/entries | identify | execute`
- Runs：`GET /runs`、`GET /runs/{id}`、`POST /runs/{id}/review`
- Cases：`GET /cases`、`/tree`、`/modules`、`POST /generate-drafts`、CRUD、`POST /{id}/copy`
- Tasks（Bug/日志/SQL 共用）：`GET/POST /tasks`、`POST /completeness`、`GET /{id}`、`POST /{id}/review`
- Regression：`GET/POST /lists`、`GET /lists/{id}`、`POST /lists/generate`、`PUT /items/{id}/status`
- Reports：`GET /reports`、`POST /generate`、`GET /{id}`
- KB：列表/统计/分类/来源查询、CRUD、`POST /{id}/review`、`POST /{id}/revoke`
- Workbench：`GET /entries`、`PUT /entries/{taskType}`
- Validation：`GET /cases`、`POST /cases/{id}/run`
- Config：模块树/新增/启停、LLM 列表/更新/测试连接、格式模板读写、关联规则读写

## 六、验证结果（全部通过）

1. `mvn compile/package` 编译通过；后端启动自动建库 `lemon_testpilot`，Flyway V1→V3 迁移成功
2. identify：材料路由识别 `bug_analysis → log_triage → sql_analysis → regression_list`，完整度 79%，缺项（时间范围、SQL 文本）提示正确
3. execute：runId 落库、4 快照（完整度 100%/67%/50%/100%）、31 个执行步骤
4. review：驳回补充 / 确认通过 状态流转正常
5. val_payment 案例验证：5 项检查全 PASS（路由顺序、不跳结论、根因需证据、回归项数量、P0/P1 人工复核）
6. 前端 `npm run build` 构建通过（13 页面 chunk）
7. 前后端联调：5173 页面 200、/api 代理 code=0、val_payment 经代理运行 passed=True

## 七、开发中修复的关键问题

| 问题 | 根因 | 修复 |
|---|---|---|
| mysql 驱动缺版本号 | Spring Boot 2.7.18 BOM 管理新坐标 | pom 改 `com.mysql:mysql-connector-j` |
| `Cannot register alias 'taskExecutor'` | 引擎类名与 Spring 线程池 bean 别名冲突 | `@Service("taskExecuteEngine")` |
| `Unknown column 'created_at'` | 实体继承 TimestampedEntity 但 V1 DDL 缺列 | V3 增量迁移补列 |
| `engine_mode cannot be null` | Hibernate 首存显式插 NULL，MySQL DEFAULT 不生效 | 首存前设置 stepCount/missingCount/engineMode 默认值 |
| val_payment 路由顺序不符预期 | sql_analysis 触发词少 1 分 | 触发词补"排查"，同分由 ORDER 决胜 |
| 前端构建 `Could not resolve "../api"` | 子目录组件相对路径少一级 | 改 `../../api` |

## 八、如何启动

- 双击 `scripts\start-all.bat`（推荐），或分别运行 `scripts\start-backend.ps1` / `scripts\start-frontend.ps1`
- 首次运行：脚本自动从 example 生成 `application-local.yml`，需填入 MySQL root 密码后重启后端
- 访问：前端 `http://localhost:5173`（代理 /api → 3344）；后端 `http://localhost:3344`

## 九、已知限制与后续建议

- LLM 未配置 API Key 时全程规则引擎（功能完整，输出为模板化结构）；配置后 engineMode 标识 llm
- 测试报告 PDF 采用浏览器打印方案（详情页打印按钮），未引入服务端 PDF 依赖
- 回归清单导出为 CSV（Excel 可直接打开），XLSX 需引入 POI 后续增强
- 快速登录/多用户权限不在范围内（个人工作台定位）
