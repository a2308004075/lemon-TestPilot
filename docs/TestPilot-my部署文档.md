# TestPilot-my 部署文档

> **版本**：v1.0（2026-10-06）
> **对象**：`D:\develop\TestPilot-my`（已按批准计划完成全量开发，13 模块可运行）
> **基准**：以**实际实现代码**为准。实施阶段经确认的技术选型与《概要设计/详细设计》（编码前设计稿）存在差异，见 1.2。
> **关联**：《TestPilot项目说明文档.md》《TestPilot-my概要设计.md》《TestPilot-my详细设计.md》《DEVELOPMENT_REPORT.md》（根目录）

---

## 1. 系统组成

### 1.1 运行视图

```text
开发/本地使用态（默认）：
  [浏览器] --> [Vite dev 5173] --代理 /api--> [Spring Boot 3344] --> MySQL lemon_testpilot
                                                    └--> 外部 LLM API（可选，未启用时走内置规则引擎）

生产态（可选）：
  [浏览器] --> [Nginx 80（托管 frontend/dist + 反向代理 /api）] --> [java -jar 3344] --> MySQL + LLM
```

### 1.2 实际技术栈（与设计稿差异对照）

| 项 | 设计稿（编码前） | 实际实现 |
| --- | --- | --- |
| 后端框架 | Spring Boot 3.5 + MyBatis-Plus | **Spring Boot 2.7.18 + Spring Data JPA** |
| JDK | JDK 21 | **Java 8（1.8）** |
| 后端端口 | 8080 单端口托管 dist | **3344**，前端独立部署 |
| 数据库 | testpilot_my，19 张表 | **lemon_testpilot，16 张表** |
| 前端 | Vue 3 + TypeScript + Pinia + Vite 6 | **Vue 3.5 + JavaScript + Element Plus 2.8 + Vite 5.4**（无 Pinia） |
| AI | 自研客户端 + embedding 语义检索 | **OpenAI 兼容 /chat/completions + 内置规则引擎降级**（未配置 LLM 全功能可用） |

## 2. 环境要求

| 组件 | 版本要求 | 用途 | 说明 |
| --- | --- | --- | --- |
| JDK | 1.8（Java 8） | 后端运行 | `java -version` 可验证 |
| Maven | 3.6+ | 后端构建/启动 | 需可访问中央仓库（首次下载依赖较慢） |
| Node.js | 16+（含 npm） | 前端构建/启动 | 仅前端需要 |
| MySQL | 5.7 / 8.0 | 数据存储 | 本机 127.0.0.1:3306，账号建议 root（首次自动建库需建库权限） |
| 浏览器 | Chrome / Edge 现代版本 | 访问界面 | — |

## 3. 部署前配置（唯一必配项）

系统只有一个必须的本地配置：**MySQL 密码**。

1. 复制 `backend/src/main/resources/application-local.yml.example` 为同目录 `application-local.yml`：
   ```yaml
   spring:
     datasource:
       password: 你的MySQL密码
   ```
2. 该文件已列入 `.gitignore`，不会入库；`application.yml` 中 `spring.profiles.active: local` 自动加载它。
3. 数据库名、端口、账号在 `backend/src/main/resources/application.yml` 的 `spring.datasource.url/username` 中，默认 `jdbc:mysql://127.0.0.1:3306/lemon_testpilot?createDatabaseIfNotExist=true...`，一般无需修改。

> 使用 `scripts\start-backend.ps1` 启动时，若检测到 `application-local.yml` 不存在会自动从 example 复制并提示，改完密码重启即可。

## 4. 首次部署与启动

### 方式 A：一键脚本（推荐）

双击项目根目录：

```bat
scripts\start-all.bat
```

会打开两个窗口分别启动后端（3344）与前端（5173），前端窗口首次运行自动执行 `npm install`。

### 方式 B：分步手动启动

```bash
# 1) 后端（在 backend/ 目录下）
mvn spring-boot:run

# 2) 前端（在 frontend/ 目录下，另开终端）
npm install
npm run dev
```

### 首次启动自动完成的行为（无需手工干预）

| 步骤 | 行为 |
| --- | --- |
| 建库 | JDBC `createDatabaseIfNotExist=true` 自动创建 `lemon_testpilot` |
| 建表 | Flyway `V1__init_schema.sql` 创建 16 张表 |
| 种子 | `V2__seed_data.sql` 灌入演示数据（模块树/知识/用例/执行记录/工作台入口/验证案例等，仅 INSERT） |
| 补列 | `V3__add_missing_created_at.sql`（已执行过 V1 的旧库走此增量） |

**约定**：全部结构变更走 Flyway 迁移且不可变；系统无 DELETE/TRUNCATE/DROP 操作。

## 5. 部署验证

按顺序确认以下 4 项即部署成功：

| # | 验证项 | 方法 | 预期 |
| --- | --- | --- | --- |
| 1 | 后端存活 | 浏览器打开 `http://localhost:3344/api/dashboard/stats` | JSON `"code": 0` |
| 2 | 前端访问 | 浏览器打开 `http://localhost:5173` | 进入工作台首页，顶栏 DB 状态为绿色圆点 |
| 3 | 核心闭环 | AI 测试助手页 → 粘贴一段 Bug 现象材料 → 识别 → 执行 → 人工复核"确认通过" | 生成执行记录，执行记录页可查详情 |
| 4 | 案例验证 | 左侧"案例验证"页 → 对 `val_payment` 点击"运行验证" | 5 项检查全部通过，路由 `bug_analysis → log_triage → sql_analysis → regression_list` |

## 6. 生产部署（可选）

个人工作台默认以本地开发态使用即可；如需常驻服务，按以下方式部署。

### 6.1 后端：打包为 jar 常驻运行

```bash
cd backend
mvn clean package
# 产物：backend/target/testpilot-backend-1.0.0.jar
java -jar target/testpilot-backend-1.0.0.jar
```

数据库密码配置两种方式任选：

- **外置覆盖（推荐）**：将 `application-local.yml` 放到 jar 同目录的 `config/` 子目录下，Spring Boot 自动以外部文件优先；
- 内置：打包前确认 `src/main/resources/application-local.yml` 已填写（注意该文件含密码，勿将 jar 外发）。

注册为 Windows 服务可用 WinSW/NSSM 包裹 `java -jar` 命令，此处不展开。

### 6.2 前端：静态构建 + Nginx

```bash
cd frontend
npm run build
# 产物：frontend/dist/
```

前端为 history 路由，Nginx 参考配置：

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态资源
    location / {
        root  /path/to/frontend/dist;   # Windows 示例：D:/develop/TestPilot-my/frontend/dist
        index index.html;
        try_files $uri $uri/ /index.html;   # history 路由回退，必须
    }

    # API 反向代理到后端 3344
    location /api/ {
        proxy_pass http://127.0.0.1:3344;
        proxy_read_timeout 120s;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

此时直接访问 `http://localhost` 即可，无需 5173。

## 7. 日常运维

### 7.1 脚本清单（scripts/）

| 脚本 | 用途 |
| --- | --- |
| `start-all.bat` | 一键拉起前后端两个窗口 |
| `start-backend.ps1` | 后端启动（首次自动生成 application-local.yml） |
| `start-frontend.ps1` | 前端启动（node_modules 缺失时自动 npm install） |
| `stop-backend.ps1` | 停止本机 TestPilot 后端 Java 进程 |

前端手动停止：在前端终端窗口 `Ctrl+C`。

### 7.2 数据备份与恢复

```bash
# 备份
mysqldump -uroot -p lemon_testpilot > backup_lemon_testpilot.sql
# 恢复
mysql -uroot -p lemon_testpilot < backup_lemon_testpilot.sql
```

### 7.3 LLM 配置（可选）

不配置任何大模型时系统以内置规则引擎运行，全部功能可用（执行记录的引擎模式标记为 `rule`）。如需接入：配置中心 → 大模型 → 编辑厂商（Base URL / 模型名 / API Key，OpenAI 兼容接口）→ 测试连接 → 启用。调用失败或未启用均自动降级规则引擎，不阻断使用。

## 8. 常见问题（FAQ）

| 现象 | 原因与处理 |
| --- | --- |
| 后端启动报 `Access denied for user 'root'` | `application-local.yml` 密码不正确；修改后重启 |
| 后端启动报Communications link failure / Connection refused | MySQL 未启动或非 3306 端口；确认 MySQL 服务状态。**绿色版 MySQL 建议注册为 Windows 服务**（管理员 CMD 执行一次即可）：`D:\software\mysql-8.0\bin\mysqld.exe --install MySQL80 --defaults-file=D:\software\mysql-8.0\my.ini`，然后 `net start MySQL80`，并在服务管理器中将其设为"自动"启动 |
| 端口被占用（3344 / 5173） | 运行 `scripts\stop-backend.ps1` 清理后端；或修改 `application.yml` 的 `server.port`、`vite.config.js` 的端口 |
| Flyway 报 checksum 校验失败 | 迁移文件不可变：已执行的 `V1/V2/V3` SQL 不能修改，只能新增 `V4__xxx.sql` |
| `http://127.0.0.1:5173` 打不开 | Vite 默认监听 localhost（可能解析为 IPv6），改用 `http://localhost:5173` |
| 首次启动后端很久无响应 | Maven 首次下载依赖；等待或配置国内镜像（如阿里云 Maven 镜像） |
| 终端日志中文乱码 | PowerShell 5.1 编码显示问题，不影响功能与数据；`chcp 65001` 可缓解 |
| 页面接口报错 500 后无法恢复 | 查看后端窗口日志定位；数据问题可对照根目录 `DEVELOPMENT_REPORT.md` 第七节已知修复记录 |

## 9. 附录：目录结构

```text
TestPilot-my\
├── backend\                          # Spring Boot 2.7.18（Java 8，端口 3344）
│   ├── pom.xml                       # 产物 testpilot-backend-1.0.0.jar
│   └── src\main\
│       ├── java\com\testpilot\       # engine 引擎 / service / web / entity / repository / common / config
│       └── resources\
│           ├── application.yml       # 公共配置（端口 3344、激活 local profile）
│           ├── application-local.yml # 本地密码（.gitignore，需自建，见第 3 节）
│           ├── application-local.yml.example
│           └── db\migration\         # Flyway V1 建表 / V2 种子 / V3 补列
├── frontend\                         # Vue 3 + Vite（dev 5173，build 产物 dist/）
│   ├── vite.config.js                # /api 代理到 3344
│   └── src\                          # layout 布局 / router 13 路由 / api 封装 / views 13 页面 / styles 主题
├── scripts\                          # 启动/停止脚本（见 7.1）
├── docs\                             # 本文档与设计文档、手册截图
└── DEVELOPMENT_REPORT.md             # 开发变更报告（模块/API/验证结果/修复记录）
```
