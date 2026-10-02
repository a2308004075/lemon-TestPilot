# 附录 A：文件速查表

> ⬆ [返回总目录](./README.md) · 上一册：[第 12 章 学习路线与通关建议](./12-学习路线与通关建议.md) · 下一附录：[附录 B 命令速查](./附录B-命令速查.md)

## 🎯 本附录是什么

一套**反向索引**。写代码写到一半，想不起「这个函数在哪个文件第几行」「这张表是谁建的」「这条接口分发给谁」时，来这里查。五张表：

| 表 | 回答的问题 |
| --- | --- |
| A.1 文件速查表 | 22 个文件分别在哪、多少行、干什么、哪一章深讲 |
| A.2 函数与组件索引 | 7 个核心源码文件里，每个函数 / 组件的定义行号 |
| A.3 数据表索引 | 16 张表谁建的、装什么、哪一章深讲 |
| A.4 API 路由索引 | 32 条路由的方法 / 路径 / 实现行号 |
| A.5 功能模块对照 | 11 个功能模块各自动到的表 / 路由 / 组件 / 章节 |

> ⚠️ **口径说明（读表前必看）**
>
> 1. **行数口径**：按「编辑器打开文件看到的最大内容行号」统计，全书统一。源码迭代后行号会漂移——本表行号用于**锚定**，跨版本精确定位请以**函数 / 组件名**为准（这也是表中「定义」列存在的意义）；
> 2. **与 00 章的两种口径**：00 章说「8 个核心文件、约 600 行」，指**参与运行的核心链路**（那 8 个文件合计 590 行）；本附录是**全仓库快照**：22 个文件、1522 行，连双击脚本、历史资产与仓库文档都算上；
> 3. **与 00 章 0.5 节的接口数**：「约 30 条路由」是**概数**口径（把同族的 POST / PATCH 合并计数）；精确值以 A.4 为准：32 条。

## A.1 文件速查表（22 个文件 · 1522 行）

### 前端应用（7 个 · 288 行）

| 文件 | 行数 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `index.html` | 13 | 唯一入口 HTML：`<div id="app">` 挂载点 + 模块脚本引入 | 2.4 |
| `vite.config.ts` | 7 | 全部构建配置：React 插件 + 端口 3344 | 2.5 |
| `tsconfig.json` | 18 | TypeScript 严格模式配置 | 2.6 |
| `src/vite-env.d.ts` | 1 | 引入 Vite 客户端类型的唯一一行 | 2.6 / 2.7 |
| `src/main.tsx` | 200 | 主应用：NAV + API 封装 + 15 个页面组件 + 6 个抽屉弹窗 + 组件与函数工具族 | 2.7 / 6 / 7 |
| `src/workbench.tsx` | 35 | 工作台四页：AI 助手 / 执行记录 / 工作台编排 / 案例验证 | 6.4 / 8 |
| `src/styles.css` | 14 | 第二代全站样式：设计令牌 + 布局 + `wb-*` 工作台家族 | 6.5 / 8 |

### 后端服务（4 个 · 251 行）

| 文件 | 行数 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `server.mjs` | 127 | 全后端：建表 + seed + 通用资源引擎 + 22 条路由 + 静态服务 | 3 / 4 / 5 / 8 / 9 / 11 |
| `server-runtime.mjs` | 2 | 兼容入口：`import './server.mjs'` 的薄壳 | 3 |
| `workbench-api.mjs` | 69 | AI 工作流引擎：7 入口定义 + 触发词路由 + 10 条接口 | 8 |
| `scripts/provider-keys.mjs` | 53 | API Key 的加密存储（DPAPI / 钥匙串） | 9 |

### 工程化与交付（5 个 · 122 行）

| 文件 | 行数 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `package.json` | 30 | 依赖清单 + 5 个脚本（dev / build / start / check / deploy） | 1.2 / 2.3 |
| `scripts/deploy.mjs` | 62 | 六段部署流水线（检查 → 安装 → 类型 → 构建 → 启动 → 开浏览器） | 10 |
| `start.bat` | 14 | Windows 双击启动薄壳（转发给 deploy.mjs） | 10 |
| `start.command` | 14 | macOS 双击启动薄壳 | 10 |
| `启动 TestPilot.command` | 2 | macOS 中文文件名启动壳 | 10 |

### 历史资产（2 个 · 745 行）

| 文件 | 行数 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `app.js` | 498 | 第一代纯 JS 原型（localStorage 版）——第 11 章迁移功能的「数据来源」 | 0 / 11 |
| `styles.css`（根级） | 247 | 第一代原型样式（与 `app.js` 配套，现役界面已不引用） | 0 / 11 |

### 仓库与文档（4 个 · 116 行）

| 文件 | 行数 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `.gitignore` | 15 | 忽略 node_modules / data / dist / logs / backups | 2 / 10 |
| `pnpm-workspace.yaml` | 2 | 工作区声明（历史遗留，当前 npm 流程不依赖） | 10 |
| `README.md`（根级） | 70 | 项目说明与快速开始 | — |
| `docs/deployment-review.md` | 29 | 一次真实部署走查的记录 | 10 |

## A.2 函数与组件索引

「行号」列是定义所在行（区间）；「定义」列就是你在编辑器里全局搜索的名字。

### A.2.1 `server.mjs`（127 行）

| 行号 | 定义 | 职责 |
| --- | --- | --- |
| 1–8 | `import` 区 | node:http / fs / path / sqlite + provider-keys + workbench-api |
| 10–17 | 配置与数据库 | root / port / host / production / dataDir / backups / `db`（WAL + 外键） |
| 19–32 | `db.exec` 建表 | 12 张表一次建齐（见 A.3） |
| 34–37 | `now` / `makeId` / `json` / `parse` | 时间戳、ID、JSON 序列化与解码 |
| 39–77 | `seed()` | 首次运行的演示数据（5 模块 / 4 用例 / 4 知识 / 4 任务 / 1 回归 / 1 报告 / 1 工作流 / 4 厂商） |
| 78 | `seed()` 调用 | 幂等种子入口 |
| 79 | `const workbench` | 挂载工作台引擎（来自 workbench-api.mjs） |
| 80–82 | relations 守卫 | 补一条「任务 ↔ 知识」关联演示 |
| 84 | `moduleLabel()` | 模块 id →「项目 / 模块」标签 |
| 85 | `mapRow()` | 蛇形→驼峰 + JSON 列解码 + 附加字段（命名转换层） |
| 86 | `resources` | 8 个通用资源的声明表（表名 / 搜索列 / 排序） |
| 87 | `listResource()` | 分页 / LIKE 搜索 / 排序的通用查询引擎 |
| 88 | `send()` | 统一 JSON 响应（含 no-store） |
| 89 | `readBody()` | 请求体读取与解析 |
| 90 | `audit()` | 写一条操作审计 |
| 91 | `providerKeys` 解构 | 取 `saveProviderKey` / `readProviderKey` |
| 93–120 | `handleApi()` | 22 条路由分发（见 A.4.1） |
| 122 | `types` | 5 种静态文件的 MIME 表 |
| 123–124 | 双模式准备 | 生产缺 dist 立即报错；dev 内嵌 Vite 中间件 |
| 125 | `createServer` | 请求总入口（`/api/` 分流 + 静态回退） |
| 126 | error 处理 | 端口占用友好提示 |
| 127 | `listen` | 启动并打印访问地址 |

### A.2.2 `workbench-api.mjs`（69 行）

| 行号 | 定义 | 职责 |
| --- | --- | --- |
| 1–9 | `ENTRY_DEFS` | 7 个入口的完整定义（触发词 / 必需字段 / 知识规则 / 工作流） |
| 10 | `DEFAULT_ORDER` | 顺序路由的默认优先序 |
| 11–15 | `now` / `parse` / `json` / `makeId` / `camel` | 五个小工具 |
| 17 | `normalizeAsset()` | 入口资产行 → 前端结构 |
| 18 | `normalizeRun()` | 运行记录行 → 前端结构（含 steps 组装） |
| 19–25 | `fieldPresent()` | 字段有无的判定规则（正则表） |
| 26 | `routeText()` | 文本 → 命中入口（触发词匹配 + 显式选择） |
| 27–36 | `buildOutput()` | 按入口生成结构化输出（缺项 / 风险 / 建议） |
| 38 | `export function createWorkbench(db)` | 唯一导出：建表 + 种子 + 返回 `{handle}` |
| 40–43 | 建 4 表 | workbench_assets / workflow_runs / workflow_run_steps / validation_cases |
| 45–48 | 种子 | 7 入口资产 + 验证案例样本 |
| 50–53 | `send` / `body` / `assets` / `getRun` | 内部闭包工具 |
| 54–67 | `handle(req,res,url,parts)` | 10 条接口分发（见 A.4.2） |
| 68 | 返回 | `return {handle}` |

### A.2.3 `src/main.tsx`（200 行）

**类型与常量**

| 行号 | 定义 | 职责 |
| --- | --- | --- |
| 14–16 | `Row` / `PageKey` / `Drawer` | 行记录 / 13 个页面 id 联合类型 / 抽屉状态 |
| 18–25 | `NAV` | 13 项侧栏导航（id / 标签 / 图标） |
| 26–33 | `TASK_META` | 六类任务的元数据（标题 / 说明 / 必需字段） |
| 34 | `KB_CATEGORIES` | 知识库 7 个分类 |
| 35–41 | `API` | 前端请求封装（request / list / get / post / patch） |

**外壳与页面组件（L43–143）**

| 行号 | 组件 | 职责 |
| --- | --- | --- |
| 43–74 | `App`（外壳） | 状态 / bootstrap 加载 / 13 页面分发 / 抽屉与弹窗挂载 |
| 76 | `Dashboard` | 首页：六指标 + 快捷入口 + 资产四连 + 最近任务 |
| 88 | `QuickTasks` | 组合任务速建【遗留·未挂载】 |
| 99 | `CasesPage` | 用例库：列表页模式的样板 |
| 114 | `CaseEditor` | 用例编辑器（子组件） |
| 116 | `AnalysisPage` | Bug / 日志 / SQL 三页共用的工作区 |
| 123 | `RegressionPage` | 回归清单（进度与即改即存） |
| 125 | `GenerateRegression` | 回归生成表单（子组件） |
| 127 | `ReportsPage` | 报告列表与生成 |
| 131 | `KnowledgePage` | 知识库列表 + 弹窗 |
| 133 | `WorkflowPage` | 工作流模板页【遗留·未挂载】 |
| 135 | `SettingsPage` | 配置中心外壳（四个面板） |
| 137 | `ModuleSettings` | 面板：模块树管理 |
| 139 | `ModelSettings` | 面板：厂商配置 |
| 141 | `TemplateSettings` | 面板：输出模板 |
| 143 | `RelationSettings` | 面板：任务↔知识映射（只读） |

**抽屉、弹窗与共享组件（L145–162）**

| 行号 | 组件 | 职责 |
| --- | --- | --- |
| 145 | `DetailDrawer` | 全应用统一详情抽屉（承载 6 类记录） |
| 154 | `ReportDetail` | 报告详情块 |
| 155 | `WorkflowDetail` | 工作流详情块 |
| 156 | `RegressionItemDetail` | 回归项详情块 |
| 158 | `KnowledgeModal` | 知识入库弹窗 |
| 159 | `MigrationModal` | 旧数据迁移弹窗 |
| 161 | `TaskTable` | 任务表格（多处复用） |
| 162 | `KnowledgeExperience` | 知识卡片流（分析页侧栏） |

**工具组件族（L164–179，16 个小部件）**

`PanelTitle` · `PageIntro` · `Tabs` · `Metric` · `Asset` · `Field` · `SearchBox` · `ModuleSelect` · `Badge` · `TaskIcon` · `Empty` · `DetailGrid` · `Pagination` · `TreeNode` · `Completeness` · `TemplateNotice`——从面板标题到分页的公共积木，7.16 节有逐个档案。

**工具函数族（L181–200）**

| 行号 | 函数 | 职责 |
| --- | --- | --- |
| 181 | `localAnalysis` | 本地规则分析（三层风险判定） |
| 182 | `outputSections` | 输出段模板（按任务类型） |
| 183 | `guessTitle` | 从文本猜标题 |
| 184 | `badgeClass` | 值 → 徽章颜色类 |
| 185 | `shortModule` | 模块标签缩写 |
| 186 | `moduleFromId` | 模块 id → 名称（迁移用映射） |
| 187–188 | `date` / `dateTime` | 日期与时间格式化 |
| 189 | `pretty` | 任意值 → 展示字符串 |
| 190 | `groupBy` | 数组按字段分组 |
| 191 | `groupModules` | 模块树聚合用例 |
| 192 | `countTree` | 嵌套对象计数 |
| 193 | `downloadRows` | CSV / Excel 导出 |
| 194 | `downloadText` | 文本文件下载 |
| 195 | `regressionText` | 回归清单 → Markdown 文本 |
| 196 | `exportRegression` | 回归导出（四格式分发） |
| 197 | `reportMarkdown` | 报告 → Markdown |
| 198 | `downloadPdf`（async） | 报告 → PDF（jsPDF） |
| 200 | 挂载调用 | `createRoot(...).render(<App/>)` |

### A.2.4 `src/workbench.tsx`（35 行）

| 行号 | 定义 | 职责 |
| --- | --- | --- |
| 5 | `api()` | 工作台请求封装（自动加 `/api/workbench` 前缀） |
| 6–7 | `post()` / `patch()` | 带方法体的请求快捷封装 |
| 8 | `routeIcon()` | 入口 id → 图标组件 |
| 9 | `dateTime()` | 时间格式化 |
| 11 | `AssistantPage`（导出） | AI 测试助手（全应用「新建任务」落地页） |
| 27 | `RunResult` | 运行结果卡（助手页与记录页两处复用） |
| 29 | `RunsPage`（导出） | 执行记录列表 |
| 31 | `OrchestrationPage`（导出） | 工作台编排（多路由时间线） |
| 33 | `SandboxPage`（导出） | 案例验证（批量比对） |
| 35 | `WBadge` | 工作台徽章 |

### A.2.5 `scripts/deploy.mjs`（62 行）

| 行号 | 定义 | 职责 |
| --- | --- | --- |
| 7–12 | 配置 | root / 部署日志流 / port / host |
| 13 | `say()` | 双通道输出（终端 + logs/deploy.log） |
| 14–23 | `run()` | 子进程封装（Promise + 失败即退出） |
| 24 | `try` 开始 | 六段流水线入口 |
| 25–32 | ① 环境检查 | Node ≥ 24 / 端口合法 / `node:sqlite` 可用 / 端口占用探测 |
| 33–36 | ② 依赖安装 | `npm ci`（锁文件精确安装） |
| 37–38 | ③ 类型检查 | `tsc --noEmit` |
| 39 | ④ 构建 | `vite build` → dist/ |
| 40–41 | ⑤ 启动 | 以 `--production` 拉起 server.mjs |
| 42–52 | ⑥ 开浏览器 | 探测「已启动」输出后调系统默认浏览器 |
| 53–54 | 流与信号 | stderr 转发；SIGINT / SIGTERM 透传给子进程 |
| 55–58 | 谢幕判定 | 监听子进程退出（非零则报错） |
| 59–62 | `catch` / `finally` | 失败提示 + 日志收尾 |

### A.2.6 `scripts/provider-keys.mjs`（53 行）

| 行号 | 定义 | 职责 |
| --- | --- | --- |
| 1–5 | `import` 区 | child_process / util / path |
| 6 | `exec` | `promisify(execFile)` |
| 7–19 | `dpapi(mode, value)` | 加密后端：Windows DPAPI（PowerShell EncodedCommand 承载） |
| 20–53 | `providerKeys(dataDir)`（导出） | 工厂：`save`（加密落盘）/ `read`（解密读出）——server.mjs 第 91 行解构使用 |

### A.2.7 `src/styles.css`（14 行）

| 行号 | 段 | 内容 |
| --- | --- | --- |
| 1 | 字体导入 | Google Fonts：Manrope + Noto Sans SC |
| 2 | 设计令牌 | `:root` 变量 + 全局重置 + loading 态 |
| 3–11 | 布局与组件 | shell / sidebar / topbar / hero / metric / tabs / filterbar / 回归卡 / 工作流构建器 / overlay 与抽屉 / 媒体查询 |
| 12–13 | 分隔 | 空行 + `/* AI 测试助手与工作台编排 */` 注释 |
| 14 | 工作台家族 | `wb-*` 类名（第 8 章追加） |

## A.3 数据表索引（16 张）

16 张表由两处 SQL 建出：`server.mjs` 第 19–32 行建 12 张，`workbench-api.mjs` 第 40–43 行建 4 张。按第 4 章的五组口径排列。

### 组织与系统（4 张）

| 表名 | 建表位置 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `modules` | server.mjs 第 20 行 | 项目 / 模块 / 子模块三级树 | 4.2 / 7.11 |
| `settings` | server.mjs 第 29 行 | 键值设置（value_json 存任意结构） | 4.2 |
| `audit` | server.mjs 第 30 行 | 写操作审计流水（详情抽屉时间线的数据源） | 4.2 / 5.2 |
| `migrations` | server.mjs 第 31 行 | 迁移记账（幂等守卫） | 4.2 / 11 |

### 业务资产（5 张）

| 表名 | 建表位置 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `cases` | server.mjs 第 22 行 | 测试用例（执行状态 / 关联） | 4.2 / 7.3 |
| `knowledge` | server.mjs 第 23 行 | 知识库（7 分类，可被任务引用） | 4.2 / 7.9 |
| `regressions` | server.mjs 第 24 行 | 回归清单（data_json 条目 + progress） | 4.2 / 7.6 |
| `reports` | server.mjs 第 25 行 | 测试报告（sources_json 引用链） | 4.2 / 7.8 |
| `workflows` | server.mjs 第 26 行 | 工作流模板（steps_json） | 4.2 / 7.10 |

### 任务与流转（2 张）

| 表名 | 建表位置 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `tasks` | server.mjs 第 21 行 | 六类任务统一表（type 区分 Bug / 日志 / SQL…） | 4.2 / 5.3 |
| `relations` | server.mjs 第 27 行 | 多态关联边（任务 ↔ 知识 ↔ 用例…） | 4.5 / 7.12 |

### 模型配置（1 张）

| 表名 | 建表位置 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `providers` | server.mjs 第 28 行 | 大模型厂商（key 只存掩码，密文另行加密存放） | 4.2 / 9 |

### 工作流引擎（4 张）

| 表名 | 建表位置 | 职责 | 深讲章节 |
| --- | --- | --- | --- |
| `workbench_assets` | workbench-api.mjs 第 40 行 | 7 个入口资产（触发词 / 必需项 / 版本） | 8 |
| `workflow_runs` | workbench-api.mjs 第 41 行 | 运行记录（状态 + 复核状态机） | 8 |
| `workflow_run_steps` | workbench-api.mjs 第 42 行 | 运行分步明细（时间线） | 8 |
| `validation_cases` | workbench-api.mjs 第 43 行 | 案例验证样本（期望路由 / 期望字段） | 8 |

## A.4 API 路由索引（32 条）

真实项目共 **32 条分发**：`server.mjs` 的 `handleApi` 22 条 + `workbench-api.mjs` 的 `handle` 10 条。全部挂在 `http://127.0.0.1:3344/api/` 之下。

### A.4.1 基础路由（server.mjs `handleApi`，22 条）

| 方法 | 路径 | 行号 | 功能 | 章节 |
| --- | --- | --- | --- | --- |
| ALL | `/api/workbench/*` | 96 | 转发给工作台引擎（见 A.4.2） | 8 |
| GET | `/api/bootstrap` | 97 | 首页聚合：8 资源计数 + 模块 + 厂商 + 任务 | 5.3 |
| GET | `/api/:resource` | 98 | 8 个资源的通用列表（分页 / 搜索 / 排序） | 5.3 |
| GET | `/api/:resource/:id` | 99 | 通用详情 | 5.3 |
| POST | `/api/tasks` | 100 | 创建任务（支持父子拆分） | 5.3 |
| PATCH | `/api/tasks/:id` | 101 | 更新任务（状态 / 复核） | 5.3 |
| POST | `/api/cases` | 102 | 创建用例 | 5.3 / 7.3 |
| PATCH | `/api/cases/:id` | 103 | 更新用例（执行状态等） | 5.3 / 7.3 |
| POST | `/api/knowledge` | 104 | 发布知识 | 5.3 / 7.9 |
| PATCH | `/api/knowledge/:id` | 105 | 更新知识（被引用时 409 保护） | 5.3 / 7.9 |
| PATCH | `/api/regressions/:id` | 106 | 更新回归（进度自动计算） | 5.3 / 7.6 |
| POST | `/api/reports` | 107 | 生成报告 | 5.3 / 7.8 |
| POST | `/api/workflows` | 108 | 创建 / 保存工作流模板 | 5.3 / 7.10 |
| POST | `/api/modules` | 109 | 新建模块节点 | 5.3 / 7.11 |
| PATCH | `/api/modules/:id` | 110 | 更新模块 | 5.3 / 7.11 |
| PATCH | `/api/providers/:id` | 111 | 更新厂商（Key 走加密通道） | 9 |
| POST | `/api/providers/:id/test` | 112 | 厂商探活 | 9 |
| POST | `/api/ai/analyze` | 113 | AI 分析（失败降级到本地规则） | 7.5 / 9 |
| POST | `/api/migrate` | 114 | 第一代数据迁移（幂等 + 备份先行） | 11 |
| GET | `/api/settings/:key` | 115 | 读设置 | 5.3 |
| PATCH | `/api/settings/:key` | 116 | 写设置 | 5.3 |
| GET | `/api/audit` | 117 | 实体操作审计列表 | 5.3 |

### A.4.2 工作台路由（workbench-api.mjs `handle`，10 条）

| 方法 | 路径 | 行号 | 功能 | 章节 |
| --- | --- | --- | --- | --- |
| GET | `/api/workbench/config` | 56 | 入口资产 + 分发策略配置 | 8 |
| POST | `/api/workbench/preflight` | 57 | 提交前预检（命中路由 + 缺项提示） | 8 |
| POST | `/api/workbench/runs` | 58 | 新建运行（执行管道全流程） | 8 |
| GET | `/api/workbench/runs` | 59 | 运行记录列表（分页 / 筛选） | 8 |
| GET | `/api/workbench/runs/:id` | 60 | 运行详情（含 steps 与复核结果） | 8 |
| PATCH | `/api/workbench/runs/:id` | 61 | 复核（状态机流转） | 8 |
| GET | `/api/workbench/assets` | 62 | 入口资产列表 | 8 |
| PATCH | `/api/workbench/assets/:id` | 63 | 更新入口资产（版本自增） | 8 |
| GET | `/api/workbench/validations` | 64 | 验证案例列表 | 8 |
| POST | `/api/workbench/validations/:id` | 65 | 执行单个案例验证 | 8 |

## A.5 功能模块全链路对照（11 个模块）

前面三节按「文件 / 表 / 路由」切分，本节按**功能模块**切分——学完一个功能，回来清点它动过的全部落点：

| 功能模块 | 数据表 | API 路由（行号） | 前端落点 | 深讲章节 |
| --- | --- | --- | --- | --- |
| 工作台引擎：AI 助手 · 执行记录 · 编排 · 案例验证 | `workbench_assets` `workflow_runs` `workflow_run_steps` `validation_cases` | `/api/workbench/*` × 10 | `workbench.tsx` 四页（助手 / 记录 / 编排 / 验证）+ `RunResult` | 8（核心） |
| 任务分析 | `tasks` | `POST /api/tasks`（100）· `PATCH /api/tasks/:id`（101） | `Dashboard` `QuickTasks` ⚠️ `AnalysisPage` `TaskTable` | 7.1 / 7.2 / 7.5 |
| 用例库 | `cases` | `POST /api/cases`（102）· `PATCH /api/cases/:id`（103） | `CasesPage` `CaseEditor` | 7.3 / 7.4 |
| 回归清单 | `regressions` | `PATCH /api/regressions/:id`（106） | `RegressionPage` `GenerateRegression` | 7.6 / 7.7 |
| 测试报告 | `reports` | `POST /api/reports`（107） | `ReportsPage` `ReportDetail` | 7.8 / 7.13 |
| 知识库 | `knowledge` | `POST /api/knowledge`（104）· `PATCH /api/knowledge/:id`（105） | `KnowledgePage` `KnowledgeModal` `KnowledgeExperience` | 7.9 / 7.14 / 7.15 |
| 工作流模板 | `workflows` | `POST /api/workflows`（108） | `WorkflowPage` ⚠️ `WorkflowDetail` | 7.10 / 7.13 |
| 模块树 | `modules` | `POST /api/modules`（109）· `PATCH /api/modules/:id`（110） | `ModuleSettings` `ModuleSelect` | 7.11 |
| 配置与审计（系统支撑） | `settings` `relations` `audit` | `GET/PATCH /api/settings/:key`（115–116）· `GET /api/audit`（117） | `TemplateSettings` `RelationSettings` `DetailDrawer` 时间线 | 4.5 / 5.2 / 7.11–7.12 |
| 厂商与模型接入 | `providers` | `PATCH /api/providers/:id`（111）· `POST /api/providers/:id/test`（112）· `POST /api/ai/analyze`（113） | `ModelSettings` | 9 |
| 数据迁移 | `migrations` | `POST /api/migrate`（114） | `MigrationModal` `moduleFromId` | 11 |

> ⚠️ **读表说明**
>
> 1. **章节列只列差异化部分**：12 张基础表的建表 SQL 统一在 4.2 完整展开、基础写路由统一在 5.3 逐条深挖，本列不重复；工作台引擎的 4 张表与 10 条接口集中在第 8 章；
> 2. **8 个通用资源**：任务 / 用例 / 知识 / 回归 / 报告 / 工作流模板 / 模块树 / 厂商这 8 行的列表与详情都走同一套引擎（`GET /api/:resource`，A.4.1 第 98–99 行）——这就是第 5 章「配置驱动」的含义：读共用一套引擎，写路由才各有专属行号；
> 3. **⚠️ = 【遗留·未挂载】**：`QuickTasks` 与 `WorkflowPage` 代码保留但未进 `NAV`（见 7.2 / 7.10）；
> 4. **「配置与审计」不是资源，是系统表**：`settings` 服务模板配置、`relations` 支撑引用保护与关联规则、`audit` 喂详情抽屉时间线。

**怎么用**：学完一章后找到对应行，把「表 → API → 组件 → 章节」默想一遍，就是该模块的完整链路；排查问题时反向走——从界面症状定位组件，顺 API 找到表。

## ⚠️ 使用要点

1. **本附录是「反查表」，各章的是「正查表」**：正向学习请回各章的「🔍 对照真实项目」小节；本表供你定位与跳转；
2. **改了代码行号漂移怎么办**：用「定义 / 组件 / 函数」列的名字全局搜索（如搜 `listResource`、搜 `QuickTasks`），即可重新锚定——名字比行号稳定；
3. **三组数字别混**：16 张表（A.3）≠ 8 个通用资源（`resources` 声明的 tasks / cases / knowledge / regressions / reports / workflows / modules / providers，走通用读引擎）≠ 32 条路由（A.4，含全部专用接口）——三者的按模块对账见 A.5；
4. **两条「引擎入口」**：全后端只有两处分发——`server.mjs` 的 `handleApi`（第 93 行）与 `workbench-api.mjs` 的 `handle`（第 54 行）；所有接口请求都在它们内部被判定。找不到接口实现时，先确认它属于哪个引擎。

## ✍️ 练习

1. **闭卷定位**（不许打开文件）：`PATCH /api/knowledge/:id` 的 409 引用保护写在哪个文件第几行？`validation_cases` 表在哪里建？`badgeClass` 函数在哪个文件？——做完用本附录自查；
2. **顺藤摸瓜**：从 `NAV` 数组出发，说出「案例验证」页面在 `main.tsx` 里的分发行、对应组件所在文件与行号、它调用的第一个接口；
3. **统计验证**：数一数 `server.mjs` 的 22 条路由里各方法各占几条（提示：PATCH 7 条 / POST 9 条 / GET 5 条 / 转发 1 条——自己先数一遍再对照）；
4. **模块听写**：盖住 A.5 的右三列，任选一个模块（如「用例库」），闭卷说出它的表、写路由、前端组件与深讲章节——再用 A.3 / A.4 逐项核对。

---

> ⬆ [返回总目录](./README.md) · 上一册：[第 12 章 学习路线与通关建议](./12-学习路线与通关建议.md) · 下一附录：[附录 B 命令速查](./附录B-命令速查.md)
