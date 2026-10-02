# 附录 B：命令速查

> ⬆ [返回总目录](./README.md) · 上一附录：[附录 A 文件速查表](./附录A-文件速查表.md) · 下一附录：[附录 C 完整源码装配参考](./附录C-完整源码装配参考.md)

## 🎯 本附录是什么

全书命令的集中查阅版。日常开发、启动停止、故障排查、数据调试、接口调用——需要敲命令时来这里抄。七个小节：

| 小节 | 查什么 |
| --- | --- |
| B.1 | npm 脚本五件套（日常 90% 的操作） |
| B.2 | 启动与停止（终端 / 双击 / 换端口 / 局域网） |
| B.3 | 排查命令（端口、日志、环境——Windows / macOS 对照） |
| B.4 | SQLite 数据调试（查库、备份、恢复、重置） |
| B.5 | API 手工调用（curl 与 PowerShell 双版本） |
| B.6 | 环境变量一览 |
| B.7 | 常见错误 → 处理对照表（报错先查这里） |

## B.1 npm 脚本五件套

`package.json`（第 1 章）里只有 5 个脚本，先把它们背下来：

| 命令 | 展开是什么 | 干什么 | 预期看到 |
| --- | --- | --- | --- |
| `npm run dev` | `node server.mjs` | **开发模式**：Node 起服务并内嵌 Vite（改前端源码，保存后刷新可见） | `TestPilot 已启动：http://127.0.0.1:3344（局域网 http://<本机IP>:3344）` |
| `npm run check` | `tsc --noEmit` | 类型检查（不产出文件） | **无输出 = 通过**；有错会列出文件与行号 |
| `npm run build` | `tsc --noEmit && vite build` | 类型检查 + 生产构建 | `dist/` 目录生成（可能伴随 chunk 体积警告，见 B.7 第 11 条） |
| `npm start` | `node server.mjs --production` | **生产模式**启动（要求已有 `dist/`） | 与 dev 相同的启动行 |
| `npm run deploy` | `node scripts/deploy.mjs` | **一键部署**：六段流水线（检查 → 安装 → 类型 → 构建 → 启动 → 开浏览器） | 三段进度提示 + 启动行，全程日志写入 `logs/deploy.log` |

补充说明：

- `npm start` 与 `npm run start` 等价（`start` 是 npm 的保留快捷名）；
- `dev` 与 `start` 的本质差别：dev 走 Vite 中间件（源码实时编译）、start 走 `dist/` 静态文件（要求先构建）；
- 首次 `npm run deploy` 会执行 `npm ci`，需要联网下载约 58 个包；**已有 `dist/` 时可用 `npm start` 离线启动**。

## B.2 启动与停止

| 场景 | 命令 / 动作 |
| --- | --- |
| Windows 双击启动 | 双击 `start.bat`（启动失败会保留窗口显示原因） |
| macOS 双击启动 | 双击 `start.command` 或 `启动 TestPilot.command` |
| 终端启动（开发） | `npm run dev` |
| 终端启动（生产） | `npm run build` → `npm start` |
| 停止服务 | 终端内按 `Ctrl+C`；双击模式直接关闭窗口 |
| 换端口启动 | PowerShell：`$env:PORT=3345; npm run dev`（macOS：`PORT=3345 npm run dev`） |
| 局域网访问 | PowerShell：`$env:HOST='0.0.0.0'; npm run dev`，再用手机访问 `http://<电脑IP>:3344`（首次会弹 Windows 防火墙放行） |
| 部署时不自动开浏览器 | PowerShell：`$env:TESTPILOT_NO_BROWSER='1'; npm run deploy` |

## B.3 排查命令（Windows PowerShell / macOS 对照）

**查端口占用**（最常见的排查动作）：

```powershell
# Windows：查 3344 被谁占用（最右一列是 PID）
netstat -ano | findstr :3344
# 确认 PID 是本项目的 node 进程后，结束它
taskkill /PID <PID> /F
```

```bash
# macOS：查 / 结束
lsof -i :3344
kill <PID>
```

**看部署日志**：

```powershell
# Windows：看最后 50 行；加 -Wait 可实时跟踪
Get-Content logs\deploy.log -Tail 50
# macOS：tail -f logs/deploy.log
```

**其他日常检查**：

| 目的 | 命令 |
| --- | --- |
| 看 Node 版本（要求 v24+） | `node --version` |
| 修 PowerShell「禁止运行脚本」 | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`（改完重开终端） |
| 快速健康检查 | 浏览器打开 `http://127.0.0.1:3344/api/bootstrap`，能看到一大段 JSON 即后端正常 |

## B.4 数据调试（SQLite）

数据库文件是 `data/testpilot.sqlite`（WAL 模式下还有 `-wal` / `-shm` 伴生文件）。**不需要安装任何数据库工具**——用 Node 自带的 `node:sqlite` 直接查：

```powershell
node --input-type=module -e "import { DatabaseSync } from 'node:sqlite'; const db = new DatabaseSync('data/testpilot.sqlite'); console.log(db.prepare('SELECT COUNT(*) AS n FROM tasks').get());"
```

换掉里面的 SQL 即可做任意查询，例如：

- `SELECT * FROM providers`——看厂商配置（key 字段是掩码）；
- `SELECT id,title,status FROM knowledge LIMIT 5`——看知识库前 5 条；
- `SELECT COUNT(*) AS n FROM audit`——看审计流水条数。

三条纪律：

1. **备份**：停止服务后，复制整个 `data/` 目录即完成备份（含数据库、密钥与迁移备份三样东西）；
2. **恢复**：把备份的 `data/` 目录覆盖回去，重启服务即可；
3. **重置演示数据**：删除 `data/` 目录 → 重启 → `seed()` 重新灌入初始数据（⚠️ 密钥与审计记录也会一起清掉，别在正式使用后随手重置）。

## B.5 API 手工调用

**读接口（GET）**：

```powershell
Invoke-RestMethod http://127.0.0.1:3344/api/bootstrap | ConvertTo-Json -Depth 3
```

（`Invoke-RestMethod` 自动解析 JSON；后面的 `ConvertTo-Json` 只是把结果格式化好看）

**写接口（POST 创建任务）**：

```powershell
$body = '{"type":"bug","title":"命令行创建的任务","moduleId":"mod_pay_callback","risk":"P1"}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:3344/api/tasks -ContentType 'application/json' -Body $body
```

预期返回 201 与新建的任务对象（字段为驼峰式，如 `createdAt` / `reviewStatus`）。

**工作台预检（AI 引擎）**：

```powershell
$body = '{"text":"支付成功后订单仍为待支付，traceId=demo-1024","selectedRoutes":["bug_analysis"]}'
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:3344/api/workbench/preflight -ContentType 'application/json' -Body $body
```

**curl 版本**（cmd / macOS / Git Bash 通用；在 PowerShell 里请写 `curl.exe` 明确调用）：

```bash
curl http://127.0.0.1:3344/api/bootstrap
curl -X POST http://127.0.0.1:3344/api/tasks -H "Content-Type: application/json" -d "{\"type\":\"bug\",\"title\":\"命令行创建的任务\"}"
```

（全部 32 条接口的清单见 [附录 A.4](./附录A-文件速查表.md)）

## B.6 环境变量一览

| 变量 | 默认值 | 作用 | 谁读取 |
| --- | --- | --- | --- |
| `PORT` | `3344` | 服务端口 | `server.mjs` 第 11 行、`deploy.mjs` 第 11 行 |
| `HOST` | `127.0.0.1` | 监听地址；设为 `0.0.0.0` 允许局域网访问 | `server.mjs` 第 12 行、`deploy.mjs` 第 12 行 |
| `TESTPILOT_NO_BROWSER` | （未设置） | 设为 `1` 时 deploy 不自动打开浏览器 | `deploy.mjs` 第 45 行 |

命令行标志：`--production`（`server.mjs` 的生产模式开关，`npm start` 已内置）。

设置方式（只对当前终端会话生效）：

```powershell
# Windows PowerShell
$env:PORT=3345; npm run dev
```

```bash
# macOS / Linux
PORT=3345 npm run dev
```

## B.7 常见错误 → 处理对照表

报错先查这里；表里没有的，按「终端输出 → 浏览器控制台 → `logs/deploy.log`」的顺序排查。

| # | 现象 / 报错 | 原因 | 处理 |
| --- | --- | --- | --- |
| 1 | `'node' 不是内部或外部命令` / `无法将"node"项识别为...` | 未安装 Node 或未加入 PATH | 安装 Node 24 LTS（nodejs.org）后**重开终端** |
| 2 | `请安装 Node.js 24 LTS 或更高版本，然后重新启动。` | Node 版本低于 24 | 升级 Node 后重试 |
| 3 | `服务启动失败：EADDRINUSE，请检查端口 3344 是否被占用。` | 已有实例或其他程序占用端口 | 按 B.3 找 PID 处理，或换端口 `$env:PORT=3345` 启动 |
| 4 | `端口 3344 无法使用：EADDRINUSE。请关闭已有服务或设置其他 PORT。` | 同上（deploy 版提示） | 同上 |
| 5 | `缺少构建文件，请使用 start.command 或 start.bat 完成部署。` | 生产模式启动但还没有 `dist/` | 先 `npm run build`，或直接 `npm run deploy` |
| 6 | `无法加载文件 ...\npm.ps1，因为在此系统上禁止运行脚本` | PowerShell 执行策略过严 | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` 后重开终端 |
| 7 | `npm ci` 报 `ETIMEDOUT` / `ENOTFOUND` / `ECONNRESET` | 首次部署需联网下载依赖 | 检查网络后重试；网络受限可配置镜像源 |
| 8 | `npm ci` 报锁文件不一致（`EUSAGE` 等） | `package.json` 与 `package-lock.json` 不同步 | 删除 `node_modules` 后 `npm install` 重新生成锁文件 |
| 9 | 页面白屏 / 控制台 `Failed to fetch` | 后端没起来，或访问的端口与后端不一致 | 确认终端有「已启动」行；确认地址栏端口与后端一致（默认 3344） |
| 10 | 改了 `server.mjs` 却不生效 | `server.mjs` 没有热重载 | `Ctrl+C` 停止后重新 `npm run dev` |
| 11 | 构建时出现 `Some chunks are larger than 500 kB` | `jspdf` / `xlsx` 依赖体积较大 | **忽略即可**——不影响构建与访问（部署走查已确认） |
| 12 | macOS 双击 `.command` 没反应 / 提示无法打开 | 文件没有执行权限 | 终端执行 `chmod +x start.command "启动 TestPilot.command"`，或右键 → 打开 |
| 13 | 数据被误删 / 想回到初始状态 | 需要备份恢复或重置 | 停止服务 → 用备份的 `data/` 覆盖 → 重启；没有备份时删除 `data/` 目录重启即重置（密钥与审计会丢） |

## ⚠️ 使用要点

1. **三条「重启红线」**：改了 `server.mjs` / `workbench-api.mjs` / `deploy.mjs` 必须重启对应进程（Ctrl+C 后重新 `npm run dev`）；前端 `.tsx` / `.css` 保存后刷新页面即可；
2. **排错顺序**：终端输出 → 浏览器控制台 → `logs/deploy.log` → 本附录 B.7 对照表；
3. **「换端口」是最常用的一招**：本地有旧实例占着 3344 时，不必急着杀进程——`$env:PORT=3345` 先跑起来。

## ✍️ 练习

1. 用 3345 端口把开发服务器跑起来，并访问确认（不修改任何文件）；
2. 用 PowerShell 调用 `bootstrap` 接口，数出 `counts` 里各资源的数量；
3. 先启动一个 dev 实例，再开第二个终端启动第二个同端口实例，观察两边报错的不同（第二个应立即报 EADDRINUSE），然后按 B.7 第 3 条处理其中一个。

---

> ⬆ [返回总目录](./README.md) · 上一附录：[附录 A 文件速查表](./附录A-文件速查表.md) · 下一附录：[附录 C 完整源码装配参考](./附录C-完整源码装配参考.md)
