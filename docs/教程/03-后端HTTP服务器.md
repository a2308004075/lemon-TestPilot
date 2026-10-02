# 第 3 章 · 后端 HTTP 服务器

> ⬆ [返回总目录](./README.md) · 上一章：[第 2 章 · 环境准备与项目骨架](./02-环境准备与项目骨架.md) · 下一章：[第 4 章 · 数据层设计（SQLite）](./04-数据层设计.md) →

---

### 🎯 本章目标

第 2 章结尾留了一个悬念：**为什么 `npm run dev` 不是 `vite` 命令？** 本章亲手写出 `server.mjs`，揭开「一个 3344 端口同时服务前端页面与 `/api/*` 接口」的全部秘密。学完本章你将得到：

1. 一个约 80 行、可运行的**完整 HTTP 服务器**（本章成稿，逐段手写出来）；
2. 覆盖它的全部关键能力：路由分发骨架、JSON 响应规范、静态文件服务四件套（MIME / 防目录穿越 / SPA 回退 / 禁缓存）、dev/prod 双模式、端口占用保护；
3. 一套随时可复跑的验证清单：开发模式 2 项 + 生产模式 4 项检查。

> 本章结束时：`npm run dev` 能在 3344 端口打开界面并热更新；`npm start` 能在同一端口以纯静态方式服务（完全不加载 Vite）。第 4、5 章会往本章留好的「扩展点」里装入数据库与全部业务接口。

### 🚧 前置条件

- 第 2 章已完成：`npm run check` 通过，`npx vite --port 5173` 能看到「TestPilot 启动成功」；
- `server.mjs` 目前**不存在**——本章从零创建它；
- 命令行工具：Windows 用 `curl.exe`（Win10+ 自带）；macOS 用系统自带 `curl`。

---

### 🧠 设计思路与原理

动手之前，先回答三个「为什么」。

**① 为什么前后端要挤在一个端口？**

常见的前后端分离项目里，前端 5173、后端 3000，浏览器跨端口请求会遇到 CORS（跨源限制），开发时要配 `proxy`、上线要配 Nginx 反向代理。本项目的做法是**干脆合成一个进程**：浏览器只认识 `127.0.0.1:3344` 一个地址，页面请求和接口请求天然的「同源」，CORS、代理、部署时的路由分流全部消失。代价是后端要顺带干「前端服务器」的活——正是本章 3.4、3.5 要写的部分。

**② 为什么开发和生产必须用两种模式？**

| | 开发模式（`npm run dev`） | 生产模式（`npm start`） |
| --- | --- | --- |
| 前端文件从哪来 | Vite 即时编译（改完保存即生效） | `dist/` 里的构建产物（改动需重新构建） |
| 谁处理页面请求 | 内嵌的 Vite 中间件 | 我们手写的静态文件服务 |
| 加载 Vite 吗 | 加载 | 不加载（启动更快、依赖更少） |
| 适合 | 写代码时 | 日常使用、部署 |

两种模式共用同一个 `server.mjs`、同一个端口、同一份 API 逻辑——用一个命令行参数（`--production`）切换。这就是「一份代码，两种形态」。

**③ 为什么不用 Express？** 第 1 章从「依赖最小化」讲过，这里补充工程视角：

| 能力 | Express 提供 | 本项目自写 | 结论 |
| --- | --- | --- | --- |
| 路由 | `app.get('/api/x', h)` | `parts` 数组 + 前缀判断（3.3） | 本项目路由高度规则，框架语法用不上 |
| body 解析 | 中间件 | `readBody()` 8 行（第 5 章） | 收益太小 |
| 静态服务 | `express.static()` 一行 | 本章 3.4 共 15 行 | 反而获得了防穿越、SPA 回退的完全控制 |
| 依赖 | 数十个包 | 0 个（`node:http` 内置） | 教学链路全透明，部署零供应链风险 |

结论：不是 Express 不好，而是本项目的接口形态（几乎所有路由都是「资源名 + id」的同一套处理逻辑）让它没有发挥空间。

本章要写的服务器，结构如下：

```
浏览器 ──HTTP:3344──▶ http.createServer 回调
                         │
              ┌──────────┴──────────┐
              │ 路径以 /api/ 开头？    │
              ├─ 是 → handleApi()    │ ← 第 4 章装数据库、第 5 章装资源路由
              └─ 否 → 页面/资源请求
                    ├ dev  → Vite 中间件（即时编译 TSX + 热更新）
                    └ prod → dist/ 静态文件（3.4）
```

---

## 3.1 起手：15 行的最小 HTTP 服务器

在项目根目录新建 `server.mjs`，写入：

```js
import http from 'node:http';

const port = Number(process.env.PORT || 3344);
const host = process.env.HOST || '127.0.0.1';

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/plain; charset=utf-8' });
  res.end('Hello TestPilot');
});

server.listen(port, host, () => console.log(`TestPilot 已启动：http://127.0.0.1:${port}`));
```

运行：

```powershell
node server.mjs
```

预期终端输出一行：

```text
TestPilot 已启动：http://127.0.0.1:3344
```

浏览器打开 `http://127.0.0.1:3344`，显示 **Hello TestPilot**。此刻不管访问什么路径（包括 `/api/anything`）都返回同一行字——它还没有任何「路由」概念，这正是下一步要解决的。

逐行讲解：

1. `import http from 'node:http'`——`node:` 前缀显式声明「这是 Node 内置模块」，与 npm 安装的包区分开（第 1 章说过本项目后端零第三方依赖）；
2. `http.createServer(listener)`——创建服务器对象但**还没有监听**；`listener` 在**每个**请求到达时被调用一次，参数是 `req`（请求）与 `res`（响应）；
3. `res.writeHead(200, headers)`——写状态行与响应头。200 表示成功；`Content-Type: text/plain; charset=utf-8` 告诉浏览器「这是纯文本，用 UTF-8 解码」；
4. `res.end(body)`——发送响应体并结束这次响应。HTTP 处理的最小闭环就是 `writeHead + end` 这两步；
5. `server.listen(port, host, callback)`——真正开始监听。`host` 为 `127.0.0.1` 表示**只接受本机回环连接**，同一局域网的其他电脑访问不到（这是有意的：工具默认只给自己用）；
6. `process.env.PORT || 3344`——环境变量优先，没设置时用 3344。留这个「后门」是为了后期能灵活换端口（练习 4 会用到）。

**理解 `req` 与 `res` 的本质**（这决定了后面所有代码的写法）：

- `req`（IncomingMessage）是一个**可读流**：包含 `method`、`url`、`headers`，请求体（body）则需要自己一段段读取——第 5 章的 `readBody()` 就是干这个的；
- `res`（ServerResponse）是一个**可写流**：`writeHead` 只能调用一次、`end` 收尾；
- 回调是**异步**触发的：Node 单线程事件循环，不需要为某个请求「等在那里」，能同时接待成百上千个连接。

### 换命令行视角看结果

浏览器看的是「人眼视角」，调试接口更常用命令行。Windows PowerShell 里注意用 `curl.exe`（`curl` 是 `Invoke-WebRequest` 的别名，参数不通用）：

```powershell
curl.exe -i http://127.0.0.1:3344/api/anything
```

预期输出（节选）：

```text
HTTP/1.1 200 OK
Content-Type: text/plain; charset=utf-8
...

Hello TestPilot
```

`-i` 参数会同时打印响应头和响应体——排查接口问题时非常常用，请记住它。

---

## 3.2 解析 URL，学会发 JSON

目标：让服务器**认识路径**。把 `server.mjs` 改成：

```js
import http from 'node:http';

const port = Number(process.env.PORT || 3344);
const host = process.env.HOST || '127.0.0.1';

function send(res, status, payload) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(payload));
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  if (url.pathname === '/api/ping') {
    return send(res, 200, { pong: true, q: url.searchParams.get('q') || '', method: req.method });
  }
  send(res, 404, { error: '接口不存在' });
});

server.listen(port, host, () => console.log(`TestPilot 已启动：http://127.0.0.1:${port}`));
```

改完**必须重启**：在 3.1 的终端按 `Ctrl+C` 停掉，再 `node server.mjs`。（Node 不会自动重载后端文件——这是本章最容易踩的坑之一，见「要点与坑」第 7 条。）

验证：

```powershell
curl.exe -s "http://127.0.0.1:3344/api/ping?q=hello"
```

预期输出：

```json
{"pong":true,"q":"hello","method":"GET"}
```

逐点讲解：

1. **`new URL(req.url, base)` 的第二个参数不能省**。`req.url` 是相对路径（如 `/api/ping?q=hello`），URL 构造器必须有一个「基准地址」才能解析出主机名、路径、查询串；写 `new URL(req.url)` 会直接抛 `TypeError: Invalid URL`。基准用 `http://${req.headers.host}`——就是浏览器请求头里带的 Host 字段；
2. `url.pathname` 是路径部分（不含查询串）；`url.searchParams.get('q')` 是查询参数（返回字符串或 `null`）。后面「分页、搜索、过滤」全靠它；
3. **`send()` 把响应收敛成一个函数**：写头 + 序列化 JSON + 结束，三件事一次做完。整个项目所有 JSON 响应都走它，响应头里有两个固定约定：
   - `Content-Type: application/json; charset=utf-8`——明确告知客户端「这是 JSON、UTF-8 解码」，否则中文会乱码；
   - `Cache-Control: no-store`——**禁止任何缓存**。本地工具的数据「永远应该是最新的」，宁可每次重新请求，也不要被浏览器缓存骗了（改完代码看到旧数据是新手最大的困惑源）；
4. `return send(...)` 里的 `return` 起「提前结束、不再往下执行」的作用（send 本身没有返回值）；
5. `req.method` 是请求方法。本章它只用来演示，第 5 章起，`GET` = 读、`POST` = 建、`PATCH` = 改，全部靠它分流；
6. 没命中的路径统一 `404 + JSON`。**API 的错误响应也是 JSON**——前端拿到 `{error: '...'}` 可以直接展示；如果这里返回 HTML 错误页，前端还得先解析 HTML，纯属添乱。这是 API 设计的一致性原则。

---

## 3.3 API 路由分发骨架（handleApi）

现在做本次改造里最重要的结构决策：**把「HTTP 层」和「业务逻辑」分开**——HTTP 层（`createServer` 回调）只负责「解析 URL、判断是不是 API 请求」；业务逻辑全部收进一个函数 `handleApi`。

把 `server.mjs` 改成：

```js
import http from 'node:http';

const port = Number(process.env.PORT || 3344);
const host = process.env.HOST || '127.0.0.1';

function send(res, status, payload) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(payload));
}

// 所有 /api/* 请求都进这里；本章只有骨架，第 4、5 章逐段装满
async function handleApi(req, res, url) {
  const parts = url.pathname.split('/').filter(Boolean);
  if (parts[0] !== 'api') return false;
  try {
    // ↓↓↓ 第 4 章接数据库、第 5 章挂 8 个资源的通用路由 ↓↓↓
    return send(res, 404, { error: '接口不存在' });
  } catch (error) {
    console.error(error);
    return send(res, 500, { error: error.message || '服务端错误' });
  }
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  if (url.pathname.startsWith('/api/')) {
    await handleApi(req, res, url);
    return;
  }
  send(res, 404, { error: '页面不存在' });
});

server.listen(port, host, () => console.log(`TestPilot 已启动：http://127.0.0.1:${port}`));
```

验证（重启后）：

```powershell
curl.exe -s http://127.0.0.1:3344/api/ping
```

预期输出（3.2 的临时路由已不存在，被骨架兜底接管）：

```json
{"error":"接口不存在"}
```

设计要点逐条拆：

1. **路径分段法**——`'/api/tasks/abc'.split('/')` 得到 `['', 'api', 'tasks', 'abc']`，`filter(Boolean)` 去掉空串后是 `['api', 'tasks', 'abc']`。于是判断 `parts[0]`（域）、`parts[1]`（资源名）、`parts[2]`（id）就像读数组一样简单——**不需要正则、不需要路由库**。真实项目约 30 条路由全部建立在这几个位置的判断之上（第 5 章逐条展开）；
2. **统一入口 + 兜底**——任何没命中的请求在函数末尾统一返回 404。新增路由 = 在 `try` 里加一行 `if`，对已有一条路由零影响：这就是「顺序匹配」路由模型，简单、可预测；
3. **`try/catch` 包住全部业务逻辑**——任何一行抛异常（第 5 章会出现 JSON 解析失败、SQL 报错等），都会被捕获：先 `console.error` 把现场打印到终端（给你排查），再返回 500 JSON（给客户端响应）。**服务器绝不静默死亡**；
4. **回调变成 `async`**——因为 `handleApi` 是异步的（它要 `await` 读请求体、查数据库）。但注意一个陷阱：`async` 回调里逃逸的异常**不会**被 Node 自动捕获，请求会挂起不返回。所以 `handleApi` 内部必须自己 `try/catch`（第 3 条）——两件事是一套的；
5. `if (parts[0] !== 'api') return false;`——调用处已经判断过 `/api/` 前缀，这里再核验一次，属于纵深防御写法；真实项目逐字保留。

---

## 3.4 静态文件服务：生产模式的四件套

现在给「非 API 请求」装上真正的处理能力：从 `dist/` 目录读文件返回——这是生产模式下页面与资源的来源。

先在文件顶部补充 import（`server.mjs` 现在的 import 区）：

```js
import http from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('.', import.meta.url));
```

关于 `root` 这一行，值得展开说：

- `import.meta.url` 是「本文件自己的 `file://` 地址」；`new URL('.', ...)` 取同目录；`fileURLToPath()` 转成操作系统路径（Windows 下带盘符）；
- **为什么不用 `process.cwd()`？** 因为 cwd 取决于「从哪里启动」——双击 `start.bat` 时工作目录可能是 `C:\Windows\System32`。用**文件自身位置**推导的路径，无论从哪启动都指向正确的项目目录。这是一次写好、终生受益的工程习惯。

接着新增 MIME 表，并把 `createServer` 回调里原来的 `send(res, 404, { error: '页面不存在' })` 替换为静态服务分支：

```js
const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.svg': 'image/svg+xml', '.json': 'application/json; charset=utf-8' };

// createServer 回调中（替换原 404 那行）：
  try {
    const base = join(root, 'dist');
    const requested = normalize(url.pathname).replace(/^(\.\.(\/|\\|$))+/, '');
    let filePath = join(base, requested === '/' ? 'index.html' : requested);
    if (!existsSync(filePath) || (await stat(filePath)).isDirectory()) filePath = join(base, 'index.html');
    const content = await readFile(filePath);
    res.writeHead(200, { 'Content-Type': types[extname(filePath)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
    res.end(content);
  } catch (error) {
    res.writeHead(500);
    res.end(error.message);
  }
```

> 这段代码只有生产模式会执行（开发模式下走 3.5 的 Vite 中间件）——直接跳到 3.6 验证 D 看效果。

「四件套」逐件讲解：

**① MIME 表（`types`）——告诉浏览器「这是什么文件」。** 浏览器完全依赖响应头 `Content-Type` 决定处理方式：缺失或错误时，`.css` 会被当成纯文本直接显示出来、`.js` 会拒绝执行。这 5 条覆盖了构建产物会出现的全部文件类型；查不到的类型用 `application/octet-stream`（「未知二进制流」）兜底。真实项目的表与此逐字一致。

**② 防目录穿越（normalize + replace）——最关键的一行。** 静态服务的经典漏洞：攻击者请求 `GET /../../../etc/passwd`，如果天真地 `join(root, req.url)`，路径就会**跳出 dist 目录**，把服务器上的任意文件交出去。本项目三层防御：

1. 第一层：`new URL()` 解析时，标准 URL 解析器已经把 `..`、`%2e%2e` 这类「上一级」路径段折叠掉了；
2. 第二层：`normalize()` 归一路径——把 `..`、`.`、重复斜杠收敛成最简形式；
3. 第三层：正则 `replace(/^(\.\.(\/|\\|$))+/, '')` 把开头残留的 `../`、`..\` 序列直接剥掉——即使将来有人改掉前两层写法，这一行仍能兜住。

用 5 行实验「看见」这三层的价值（临时开个终端，不需要服务器）：

```powershell
node -e "const {join,normalize}=require('node:path'); console.log(join('D:/app/dist','../../secret.txt')); console.log(join('D:/app/dist',normalize('/../../secret.txt')));"
```

预期输出（Windows）：

```text
D:\secret.txt
D:\app\dist\secret.txt
```

第一行：不经过 `normalize` 直接 join，路径**逃出**了 dist（去了 `D:\secret.txt`）；第二行：先 `normalize` 收敛，join 后仍被「钉」在 dist 里。原理一句话：**用户输入永远不可信，进入文件系统之前必须先规范化收敛**。

**③ SPA 回退（index.html 兜底）——刷新子页面不 404。** SPA 在磁盘上只有 `index.html` 一个真实页面，其余「页面路径」都不存在。所以规则是：请求的文件不存在、或指向一个目录 → 返回 `index.html`，由前端 JS 决定渲染什么。

> 顺带一个跨平台细节：`normalize('/')` 在 Windows 上返回 `\`，所以首页实际走的是「目录 → 回退 index.html」这条路，而不是 `requested === '/'` 那条——两条路殊途同归，这就是那个 `isDirectory()` 判断存在的意义。

**④ `Cache-Control: no-store`——开发工具不需要缓存。** 理由同 3.2：防止「改了代码/数据，浏览器却还显示旧的」。

> ⚠️ 权衡说明：本章的回退比较「激进」——请求不存在的 `.js` 也会得到 index.html（浏览器控制台会报 MIME 类型错误）。真实项目就是这几行「够用就好」的写法；要更严谨，可以按扩展名区分「资源 404」与「页面回退」（练习 3 动手改进）。

---

## 3.5 dev / prod 双模式：一个端口的两副面孔

本章最关键的一段来了：**让同一份 `server.mjs`，开发时内嵌 Vite、生产时只发静态文件**。

先加模式判定与 Vite 启动逻辑（放在 `types` 表之后、`createServer` 之前）：

```js
const production = process.argv.includes('--production');

if (production && !existsSync(join(root, 'dist/index.html'))) {
  throw new Error('缺少构建文件，请使用 start.command 或 start.bat 完成部署。');
}

let vite;
if (!production) {
  const { createServer } = await import('vite');
  vite = await createServer({ root, server: { middlewareMode: true }, appType: 'spa' });
}
```

再在 `createServer` 回调里，把 Vite 分支**插到静态服务之前**：

```js
  if (vite) {
    vite.middlewares(req, res, () => send(res, 404, { error: '页面不存在' }));
    return;
  }
```

逐条解释（这一段是全章的信息密度最高点）：

1. **`process.argv` 是命令行参数数组**：`npm run dev` 执行 `node server.mjs`（无参数 → 开发模式）；`npm start` 执行 `node server.mjs --production`（带标志 → 生产模式）。用参数做模式开关是 CLI 工具的常规做法，两条命令在第 2 章 `package.json` 里已经埋好了；
2. **生产模式缺 `dist` 就立即报错（fail fast）**：与其让用户打开浏览器看到一片 404 再疑惑，不如启动瞬间打印「缺少构建文件……」并退出（退出码非 0，第 10 章的部署脚本能感知到失败）。注意这是模块加载期的**同步 throw**——服务器根本不会开始监听；
3. **`await import('vite')` 为什么用「动态导入」而不是顶部 `import`？** 三个理由：① 生产模式根本不需要 Vite（它是 devDependency，部署后可能压根没装），动态导入 = 用到才加载；② 首次加载 Vite 有几百毫秒成本，生产启动没必要付；③ 顶层 `await` 在 ES Module 里合法——模块会「等」这一步完成再继续，保证服务器开始接待请求时 Vite 已就绪；
4. **`middlewareMode: true` 是本项目的点睛之笔**：让 Vite **不要自己监听端口**，而是把整套开发服务（即时编译 TSX、模块解析、热更新通道、注入 HMR 客户端）打包成一个 `(req, res, next)` 的中间件，挂到我们的服务器上。于是浏览器访问 3344 时：`/api/*` 归后端，页面/模块请求归 Vite 即时编译返回——**一个端口同时跑着后端接口和前端热更新**。第 2 章的悬念至此揭晓；
5. **`appType: 'spa'`**：告诉 Vite 按「单页应用」模型工作——自动处理 index.html 的注入与回退。这就是开发模式下我们不需要 3.4 那套静态逻辑的原因；
6. **顺序**：回调里的分支顺序必须是 ①`/api/` 判断 → ②Vite 分支 → ③静态服务。理由：API 永远归后端（不被 Vite 截胡）；开发走 Vite、生产走静态（两个分支互斥）；404 兜底放在 Vite 的 `next` 回调里；
7. `vite.middlewares(req, res, () => send(res, 404, { error: '页面不存在' }))` 的**第三个参数是「next 回调」**：Vite 自己处理不了时调用它，由我们兜底。真实项目逐字如此。

---

## 3.6 组装完整文件与里程碑验证

### 先补上最后一块：端口占用保护

`listen` 失败（最常见的是端口被别的程序占了）不会同步报错，而是异步触发服务器的 `'error'` 事件——不监听它，进程会带着一段难懂的英文堆栈崩掉。在 `server.listen` 之前加：

```js
server.on('error', (error) => {
  console.error(`服务启动失败：${error.code}，请检查端口 ${port} 是否被占用。`);
  process.exitCode = 1;
});
```

两个细节：`error.code` 是系统错误码（端口占用是 `EADDRINUSE`）；用 `process.exitCode = 1` 而不是 `process.exit()`——让已排队的日志正常输出完再优雅退出，同时给调用方一个「失败」的退出码。

### 本章成稿：完整的 server.mjs（约 80 行）

把前面五节组装起来，就是下面的完整文件（逐字对照你的文件，或整段覆盖）：

```js
import http from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('.', import.meta.url));
const port = Number(process.env.PORT || 3344);
const host = process.env.HOST || '127.0.0.1';
const production = process.argv.includes('--production');

function send(res, status, payload) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(payload));
}

// ↓↓↓ 第 4 章装数据库、第 5 章装 8 个资源的通用路由 ↓↓↓
async function handleApi(req, res, url) {
  const parts = url.pathname.split('/').filter(Boolean);
  if (parts[0] !== 'api') return false;
  try {
    return send(res, 404, { error: '接口不存在' });
  } catch (error) {
    console.error(error);
    return send(res, 500, { error: error.message || '服务端错误' });
  }
}

const types = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.json': 'application/json; charset=utf-8'
};

if (production && !existsSync(join(root, 'dist/index.html'))) {
  throw new Error('缺少构建文件，请使用 start.command 或 start.bat 完成部署。');
}

let vite;
if (!production) {
  const { createServer } = await import('vite');
  vite = await createServer({ root, server: { middlewareMode: true }, appType: 'spa' });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  if (url.pathname.startsWith('/api/')) {
    await handleApi(req, res, url);
    return;
  }
  if (vite) {
    vite.middlewares(req, res, () => send(res, 404, { error: '页面不存在' }));
    return;
  }
  try {
    const base = join(root, 'dist');
    const requested = normalize(url.pathname).replace(/^(\.\.(\/|\\|$))+/, '');
    let filePath = join(base, requested === '/' ? 'index.html' : requested);
    if (!existsSync(filePath) || (await stat(filePath)).isDirectory()) filePath = join(base, 'index.html');
    const content = await readFile(filePath);
    res.writeHead(200, { 'Content-Type': types[extname(filePath)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
    res.end(content);
  } catch (error) {
    res.writeHead(500);
    res.end(error.message);
  }
});

server.on('error', (error) => {
  console.error(`服务启动失败：${error.code}，请检查端口 ${port} 是否被占用。`);
  process.exitCode = 1;
});

server.listen(port, host, () => console.log(`TestPilot 已启动：http://127.0.0.1:${port}（局域网 http://<本机IP>:${port}）`));
```

### 里程碑验证（按顺序做一遍）

**验证 A：开发模式启动（悬念揭晓）**

```powershell
npm run dev
```

预期终端输出：

```text
TestPilot 已启动：http://127.0.0.1:3344（局域网 http://<本机IP>:3344）
```

（首次启动可能夹杂 Vite 预构建依赖的提示行，正常。）浏览器打开 `http://127.0.0.1:3344`——**注意地址栏是 3344 而不是 5173**，显示第 2 章写的「TestPilot 启动成功」。再改一下 `src/main.tsx` 里的文字并保存：浏览器不刷新而文字即时变化——**Vite 的热更新在 3344 端口照常工作**。

**验证 B：API 占位与开发回退（dev）**

保持服务器运行，另开一个终端：

```powershell
curl.exe -s http://127.0.0.1:3344/api/ping
curl.exe -s -H "Accept: text/html" http://127.0.0.1:3344/hello/world
```

预期：

- 第一条输出 `{"error":"接口不存在"}`（handleApi 骨架的兜底）；
- 第二条输出一段以 `<!doctype html>` 开头的 HTML——Vite 的 SPA 回退在工作（浏览器访问不存在的路径也会显示界面，因为浏览器默认就带着 `Accept: text/html`）。

**验证 C：生产模式的「缺文件保护」与构建**

在 dev 终端按 `Ctrl+C` 停掉。然后**故意先生产启动一次**：

```powershell
npm start
```

预期立即报错退出：

```text
Error: 缺少构建文件，请使用 start.command 或 start.bat 完成部署。
    at file:///.../server.mjs:41:9
```

（`Error:` 那行是关键信息，下面跟一段调用栈属正常——`throw` 出来的错误都会带栈。）这就是 fail fast。接着构建、再启动：

```powershell
npm run build
npm start
```

预期：终端只有一行「TestPilot 已启动……」（没有任何 Vite 输出——生产不加载 Vite）。浏览器刷新 3344，页面正常显示，但**热更新消失了**：改 `main.tsx` 不再生效，要重新 `npm run build`——这就是两种模式的本质区别。

**验证 D：静态服务四件套的「证据链」（prod）**

保持 `npm start` 运行，另开终端：

```powershell
curl.exe -I http://127.0.0.1:3344/
```

预期（节选）：

```text
HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8
Cache-Control: no-store
```

依次验证：②找不到的文件回退成 HTML（SPA 回退）、③`.js` 资源的 MIME 类型正确（用 `dist/` 里实际存在的文件名替换下面示例）：

```powershell
curl.exe -s http://127.0.0.1:3344/some/unknown/page | Select-Object -First 1
curl.exe -I http://127.0.0.1:3344/assets/index-XXXXXX.js
```

预期：第一条输出 `<!doctype html>`（未知路径 → index.html）；第二条的 `Content-Type` 是 `text/javascript; charset=utf-8`。

**验证 E：端口占用保护**

保持 `npm start` 运行，再开一个终端运行第二个实例：

```powershell
node server.mjs --production
$LASTEXITCODE
```

预期：

```text
服务启动失败：EADDRINUSE，请检查端口 3344 是否被占用。
1
```

打印中文提示并以退出码 1 结束（`$LASTEXITCODE` 输出 `1`）；原来的服务器不受影响。

全部验证完成后，回到 `npm start` 的终端按 `Ctrl+C` 停止。

---

### 🔍 对照真实项目

| 本章内容 | 真实文件与行号 | 说明 |
| --- | --- | --- |
| 3.1 | `server.mjs` 第 1 行、第 127 行 | `node:http` 导入与 listen 日志逐字一致（含「局域网 http://<本机IP>」提示语） |
| 3.2 | `server.mjs` 第 88 行 | `send()` 与真实逐字一致（含 `no-store`） |
| 3.3 | `server.mjs` 第 93–95、118–120 行 | `parts` 分段、`try/catch`、兜底 404 的骨架逐字一致；真实函数在此骨架上装了 20+ 条路由（第 5 章） |
| 3.4 | `server.mjs` 第 122–123、125 行 | `types` 表 5 项、静态分支逻辑逐字一致 |
| 3.5 | `server.mjs` 第 13、123–124 行 | 模式判定与 Vite 中间件逐字一致 |
| 3.6 | `server.mjs` 第 126 行 | `server.on('error')` 端口保护逐字一致 |
| 本章未涉及 | `server.mjs` 第 2–8、14–92、91–92、96–117 行 | 数据库（第 4 章）、资源路由（第 5 章）、工作流转发（第 8 章）、密钥存储（第 9 章）、迁移（第 11 章） |
| 补充说明 | `server-runtime.mjs`（2 行） | 兼容入口：`import './server.mjs';`——历史上部署命令曾指向它，保留它保证旧的启动方式不失效 |
| 补充说明 | `server.mjs` 第 2 行、第 6–8 行 | 真实文件还 import 了 `mkdir/writeFile`（第 4、11 章用）、`DatabaseSync`、`providerKeys`、`createWorkbench` |

本章成稿约 80 行；真实 `server.mjs` 是 127 行——多出来的正是上表「未涉及」的部分，每章装一点，最终严丝合缝。

### ⚠️ 要点与坑

1. **`new URL(req.url)` 忘写第二个参数**——每个请求都会抛 `TypeError: Invalid URL`，终端疯狂刷屏。这是新手第一个坎；
2. **`send()`/`writeHead()` 一次请求只能调用一次**——重复调用报 `ERR_HTTP_HEADERS_SENT`，表现为接口无响应或 500。养成「每条分支处理完立刻 `return`」的习惯；
3. **async 回调的异常要自己抓**——`createServer(async ...)` 里若异常逃出 `try/catch`，Node 只打印一条 `unhandledRejection` 警告，请求**挂起**、浏览器一直转圈。所以 `handleApi` 的 `try/catch` 是必需品，未来加任何分支都在 `try` 里写；
4. **静态回退对资源文件也生效**——请求不存在的 `.js` 会得到 HTML，浏览器报「Expected a JavaScript module script but the server responded with MIME type text/html」。定位这类报错时，先确认该文件名是否真的存在于 `dist/`；
5. **PowerShell 里 `curl` ≠ curl**——`curl` 是 `Invoke-WebRequest` 的别名，`-i`、`--path-as-is`、`-I` 这些参数都不认，必须写 `curl.exe`（macOS 无此问题）；
6. **host 与日志里的小出入**：`host` 默认 `127.0.0.1`（仅本机可访问），日志里的「局域网 http://<本机IP>:3344」只是提示语——要真正开放局域网，需要把 host 改为 `0.0.0.0` 再启动。**安全提醒**：本项目没有登录鉴权，开放前想清楚；
7. **改了 `server.mjs` 必须重启才生效**——Node 不会自动重载后端文件；前端 `.tsx` 是热更新的，两边的「生效方式」不一样。多数「为什么我的改动没生效」都源于此。

### ✍️ 练习

1. **加一个健康检查接口**：在 `handleApi` 的 `try` 里加一行，让 `GET /api/ping` 返回 `{ ok: true, uptime: process.uptime(), now: new Date().toISOString() }`。提示：`if (parts[1] === 'ping' && req.method === 'GET') return send(res, 200, { ... });`。验证：`curl.exe -s http://127.0.0.1:3344/api/ping`。想一个问题：这一行为什么必须写在「兜底 404」那行**之前**？
2. **亲手发起一次「目录穿越攻击」（生产模式）**：保持 `npm start` 运行，执行 `curl.exe --path-as-is "http://127.0.0.1:3344/../server.mjs"`。观察返回的是 index.html（HTML 内容）而不是 `server.mjs` 源码——请求到达第一层防御时，`new URL()` 就把 `..` 折叠了，实际查找的是 `dist/server.mjs`（不存在）→ 回退 index.html。再想一层：为什么浏览器地址栏没法复现这个测试？（提示：浏览器会自动归一化 `..`，模拟攻击必须用 `--path-as-is` 发原始路径。）
3. **改进静态回退（进阶）**：让「带扩展名的请求且文件不存在」返回 404，只有「无扩展名的页面路径」才回退 `index.html`。提示：在回退判断前插入 `if (!existsSync(filePath) && extname(requested)) { res.writeHead(404); return res.end('Not Found'); }`。改完 `npm run build && npm start` 验证：`curl.exe -I http://127.0.0.1:3344/nope.js` 得 404，而 `curl.exe -I http://127.0.0.1:3344/nope` 仍是 200 的 index.html。
4. **换端口启动**：`$env:PORT=8080; node server.mjs`（macOS：`PORT=8080 node server.mjs`），验证日志与浏览器都变成 8080；关掉终端重开，又回到 3344——体会 `process.env.PORT || 3344` 的默认值逻辑。

---

**下一章**：[第 4 章 · 数据层设计（SQLite）](./04-数据层设计.md) →

> ⬆ [返回总目录](./README.md)
