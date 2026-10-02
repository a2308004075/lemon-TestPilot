# 附录 C：完整源码装配参考

> ⬆ [返回总目录](./README.md) · 上一附录：[附录 B 命令速查](./附录B-命令速查.md)

## 🎯 本附录是什么

全书代码的「一站式装配台」。第 3–8 章的代码散在各章的几十个小节里，本附录把它们**拼回成可以直接跑、可以直接对照的完整文件**。

三个用法：

| 场景 | 怎么用 |
| --- | --- |
| **对照答案** | 不确定自己敲对没有？把你的文件与 C.2 / C.3 的完整版并排比对 |
| **装配地图** | 想知道「这段代码从哪一章来、装到哪个位置」？看 C.2.1 / C.3.1 的段地图 |
| **差异排查** | 教学过程中的占位与过渡形态和最终形态有什么不同？看 C.5 |

三部分内容：**C.2** 教学版 `server.mjs` 全量展开、**C.3** 教学版 `workbench-api.mjs` 全量展开、**C.4** 前端装配映射。讲解在各章正文，本附录只呈现「全量代码 + 装配位置」。

> ⚠️ **先看口径**：正文各章为了便于阅读，代码是**展开格式**（一条语句一行）；真实源码是**压缩格式**（多语句一行，见第 0 章 0.6）。所以本文里的教学版代码与真实文件「逐字一致、换行不同」——**对照时不要数行号，要按段核对**。

---

## C.1 装配总览：谁在什么时候装进哪个文件

### 文件产出时间线

| 阶段 | 产出 / 修改的文件 | 关键动作 |
| --- | --- | --- |
| 第 2 章 | `package.json` `index.html` `vite.config.ts` `tsconfig.json` `src/main.tsx` `src/styles.css` `src/vite-env.d.ts` | 五个骨架文件 + 最小 React 应用 |
| 第 3 章 | `server.mjs`（新建，约 80 行） | HTTP 骨架、`handleApi` 空壳、静态服务、dev / prod 双模式 |
| 第 4 章 | `server.mjs`（约 155 行） | 数据层：12 张建表 SQL、`seed()`、`mapRow` |
| 第 5 章 | `server.mjs`（约 230 行） | API 层：`resources` / `listResource` + `handleApi` 全量 |
| 第 6 章 | `src/main.tsx`（1–74 行 + 12 行占位）、`src/workbench.tsx`（占位版）、`src/styles.css`（完整版） | 前端骨架：外壳 + 占位组件 + 4 个占位页面导出 |
| 第 7 章 | `src/main.tsx`（76–198 行） | 逐个替换占位：15 个页面组件、抽屉与弹窗、16 个工具部件、工具函数与导出体系 |
| 第 8 章 | `workbench-api.mjs`（新建 69 行）、`src/workbench.tsx`（替换为 35 行真实版）、`src/styles.css`（追加第 13–14 行）、`server.mjs`（三处接线） | AI 工作流引擎 + 4 个功能页面 + `wb-*` 样式 |
| 第 9 章 | `scripts/provider-keys.mjs`（新建 53 行）、`server.mjs`（import + 工厂实例 + 3 条路由）、`src/main.tsx`（117、139 行接线） | 密钥加密存储 + 模型接入 |
| 第 10 章 | `scripts/deploy.mjs`、`start.bat`、`start.command`、`启动 TestPilot.command`、`server-runtime.mjs` | 一键部署 + 双击三件套 |
| 第 11 章 | `server.mjs`（migrate 路由）、`src/main.tsx`（51、72、159 行接线） | 数据迁移闭环 |

### 文件依赖关系

```text
index.html
└─ /src/main.tsx ─┬─ ./styles.css        （全局样式）
                  └─ ./workbench.tsx      （AI 助手 4 页面）

server.mjs ─┬─ ./scripts/provider-keys.mjs   （第 9 章：密钥存储）
            └─ ./workbench-api.mjs           （第 8 章：工作流引擎）

scripts/deploy.mjs ──spawn──> node server.mjs --production
start.bat / start.command ──> node scripts/deploy.mjs
启动 TestPilot.command ──> start.command      （中文名双保险）
server-runtime.mjs ──> server.mjs             （2 行兼容垫片）
```

> 记法：**后端两依赖、前端一依赖**——`server.mjs` 只依赖两个自己写的模块（这就是第 1 章「不用框架」的结果）；`main.tsx` 只依赖一个组件文件。

### 「最终形态」的两个基准

| 对象 | 最终形态 | 权威位置 |
| --- | --- | --- |
| `server.mjs` | 教学版约 420 行（展开格式）/ 真实版 127 行（压缩格式） | C.2（展开版）+ 第 5 章 5.3、第 9 章 9.4–9.6、第 11 章 11.1 |
| `workbench-api.mjs` | 69 行（与真实逐字一致） | C.3 |

---

## C.2 教学版 server.mjs 全量装配

### C.2.1 段地图（14 段）

| # | 段 | 教学版来源 | 对应真实行号 | 一句话职责 |
| --- | --- | --- | --- | --- |
| 1 | import 区 | §3.6（5 个）＋§4.1（`mkdir` `DatabaseSync`）＋§9.3（`providerKeys`）＋§8.16（`createWorkbench`）＋§11.1（`writeFile`） | L1–8 | 8 个 import，随章节逐步补齐 |
| 2 | 四个常量 | §3.6 | L10–13 | root / port / host / production |
| 3 | 打开数据库 | §4.1 | L14–17 | dataDir、backups 目录、DatabaseSync、两条 PRAGMA |
| 4 | 12 张建表 SQL | §4.2 | L19–32 | `db.exec` 一整个多行字符串 |
| 5 | 四个小工具 | §4.3 | L34–37 | now / makeId / json / parse |
| 6 | `seed()` 与调用 | §4.4 | L39–78 | 演示数据 + 全空守卫（幂等） |
| 7 | 工作台装配 | §8.16 | L79 | `createWorkbench(db)` |
| 8 | relations 首条关联 | §4.5 | L80–82 | 守卫式插入（行级幂等） |
| 9 | `moduleLabel` / `mapRow` | §4.5 | L84–85 | 数据库语言 → API 语言的翻译层 |
| 10 | `resources` / `listResource` | §5.1 | L86–87 | 配置驱动的查询引擎 |
| 11 | `send` / `readBody` / `audit` + 密钥工厂解构 | §3.6、§5.2、§9.3 | L88–91 | 三个通用工具 + providerKeys 实例 |
| 12 | `handleApi` 全量 | §5.3 ＋§8.16 ＋§9.4–9.6 ＋§11.1 | L93–120 | 22 条路由 + 兜底 404 / 500 |
| 13 | 静态服务四件套 | §3.4 | L122–124 | MIME 表、生产校验、Vite 启动 |
| 14 | createServer / error / listen | §3.5、§3.6 | L125–127 | 请求分发 + 端口占用保护 |

### C.2.2 装配代码（分七块）

> 说明：代码中 `// —— §x.x ——` 注释是为对照方便的段标记，你敲的版本可以不带。真实的压缩版没有这些注释。

**块 1 · import 与常量（段 1–2，真实 L1–13）**

```js
import http from 'node:http';
import { readFile, stat, mkdir, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { extname, join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';
import { DatabaseSync } from 'node:sqlite';
import { providerKeys } from './scripts/provider-keys.mjs';
import { createWorkbench } from './workbench-api.mjs';

const root = fileURLToPath(new URL('.', import.meta.url));
const port = Number(process.env.PORT || 3344);
const host = process.env.HOST || '127.0.0.1';
const production = process.argv.includes('--production');
```

**块 2 · 打开数据库与 12 张表（段 3–4，真实 L14–32）**——对应 §4.1、§4.2：

```js
const dataDir = join(root, 'data');
await mkdir(join(dataDir, 'backups'), { recursive: true });
const db = new DatabaseSync(join(dataDir, 'testpilot.sqlite'));
db.exec('PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;');

db.exec(`
CREATE TABLE IF NOT EXISTS modules (
  id TEXT PRIMARY KEY, project_name TEXT NOT NULL, module_name TEXT NOT NULL, submodule_name TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT '启用', sort_order INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS tasks (
  id TEXT PRIMARY KEY, type TEXT NOT NULL, title TEXT NOT NULL, module_id TEXT,
  risk TEXT NOT NULL DEFAULT 'P1', status TEXT NOT NULL DEFAULT '草稿', review_status TEXT NOT NULL DEFAULT '未提交',
  knowledge_status TEXT NOT NULL DEFAULT '未入库', parent_id TEXT, input_json TEXT NOT NULL DEFAULT '{}',
  result_json TEXT NOT NULL DEFAULT '{}', model TEXT NOT NULL DEFAULT '本地规则',
  created_at TEXT NOT NULL, updated_at TEXT NOT NULL, FOREIGN KEY(module_id) REFERENCES modules(id)
);
CREATE TABLE IF NOT EXISTS cases (
  id TEXT PRIMARY KEY, code TEXT NOT NULL UNIQUE, title TEXT NOT NULL, module_id TEXT,
  priority TEXT NOT NULL DEFAULT 'P1', case_type TEXT NOT NULL DEFAULT '功能', version INTEGER NOT NULL DEFAULT 1,
  status TEXT NOT NULL DEFAULT '未执行', source_task_id TEXT, content_json TEXT NOT NULL DEFAULT '{}',
  archived INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL, updated_at TEXT NOT NULL,
  FOREIGN KEY(module_id) REFERENCES modules(id)
);
CREATE TABLE IF NOT EXISTS knowledge (
  id TEXT PRIMARY KEY, title TEXT NOT NULL, category TEXT NOT NULL, module_id TEXT,
  risk TEXT NOT NULL DEFAULT 'P1', status TEXT NOT NULL DEFAULT '待审核', version INTEGER NOT NULL DEFAULT 1,
  content TEXT NOT NULL, source_task_id TEXT, review_note TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL, updated_at TEXT NOT NULL, FOREIGN KEY(module_id) REFERENCES modules(id)
);
CREATE TABLE IF NOT EXISTS regressions (
  id TEXT PRIMARY KEY, title TEXT NOT NULL, module_id TEXT, status TEXT NOT NULL DEFAULT '执行中',
  progress INTEGER NOT NULL DEFAULT 0, data_json TEXT NOT NULL DEFAULT '{}', created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS reports (
  id TEXT PRIMARY KEY, title TEXT NOT NULL, project_name TEXT NOT NULL, verdict TEXT NOT NULL,
  version TEXT NOT NULL DEFAULT 'v1.0', data_json TEXT NOT NULL DEFAULT '{}', created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS workflows (
  id TEXT PRIMARY KEY, name TEXT NOT NULL, steps_json TEXT NOT NULL, status TEXT NOT NULL DEFAULT '模板',
  created_at TEXT NOT NULL, updated_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS relations (
  id INTEGER PRIMARY KEY AUTOINCREMENT, source_type TEXT NOT NULL, source_id TEXT NOT NULL,
  target_type TEXT NOT NULL, target_id TEXT NOT NULL, label TEXT NOT NULL DEFAULT '', active INTEGER NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS providers (
  id TEXT PRIMARY KEY, name TEXT NOT NULL, base_url TEXT NOT NULL, model TEXT NOT NULL,
  temperature REAL NOT NULL DEFAULT 0.2, timeout INTEGER NOT NULL DEFAULT 60, max_tokens INTEGER NOT NULL DEFAULT 4096,
  enabled INTEGER NOT NULL DEFAULT 0, key_mask TEXT NOT NULL DEFAULT ''
);
CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value_json TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS audit (
  id INTEGER PRIMARY KEY AUTOINCREMENT, entity_type TEXT NOT NULL, entity_id TEXT NOT NULL,
  action TEXT NOT NULL, detail TEXT NOT NULL DEFAULT '', created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS migrations (key TEXT PRIMARY KEY, completed_at TEXT NOT NULL, backup_path TEXT NOT NULL);
`);
```

**块 3 · 四个小工具与 `seed()`（段 5–6，真实 L34–78）**——对应 §4.3、§4.4：

```js
const now = () => new Date().toISOString();
const makeId = prefix => `${prefix}_${crypto.randomUUID().slice(0, 8)}`;
const json = value => JSON.stringify(value ?? {});
const parse = (value, fallback = {}) => { try { return JSON.parse(value); } catch { return fallback; } };

function seed() {
  if (db.prepare('SELECT COUNT(*) AS n FROM modules').get().n) return;
  const insertModule = db.prepare('INSERT INTO modules VALUES (?, ?, ?, ?, ?, ?, ?)');
  [['mod_pay_callback','电商平台','支付中心','支付回调'],['mod_pay_order','电商平台','支付中心','订单同步'],['mod_order_coupon','电商平台','订单中心','优惠券'],['mod_account','电商平台','账户中心','会员账户'],['mod_ops','测试平台','质量工具','任务编排']].forEach((m,i)=>insertModule.run(...m,'启用',i+1,now()));

  const insertCase = db.prepare('INSERT INTO cases VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
  [
    ['case_001','TC-PAY-001','支付回调幂等校验','mod_pay_callback','P0','接口',2,'通过',{precondition:'订单待支付',steps:['连续发送两次相同回调','查询支付流水与订单'],expected:'仅生成一笔流水且订单只更新一次',testData:'相同 transactionId'}],
    ['case_002','TC-PAY-002','支付成功后订单状态同步','mod_pay_order','P1','功能',1,'失败',{precondition:'正常支付订单',steps:['完成支付','等待回调消费','查询订单'],expected:'订单状态为已支付',testData:'普通订单'}],
    ['case_003','TC-COUPON-001','取消订单返还优惠券','mod_order_coupon','P1','回归',3,'未执行',{precondition:'订单使用优惠券',steps:['取消待支付订单','查询优惠券状态'],expected:'优惠券恢复可用',testData:'有效优惠券'}],
    ['case_004','TC-PAY-003','三方回调签名异常','mod_pay_callback','P0','异常',1,'阻塞',{precondition:'准备错误签名',steps:['发送异常回调'],expected:'拒绝处理并记录安全日志',testData:'错误 sign'}]
  ].forEach(r=>insertCase.run(...r.slice(0,8),null,json(r[8]),0,now(),now()));

  const insertKnowledge = db.prepare('INSERT INTO knowledge VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
  [
    ['kb_001','支付回调重复消费导致重复扣款','历史 Bug 库','mod_pay_callback','P0','已发布',2,'重复回调可能触发 MQ 重复消费；回归必须覆盖幂等键、流水唯一索引与退款。','task_bug_001'],
    ['kb_002','支付回调日志关键字','日志规律库','mod_pay_callback','P1','已发布',1,'重点检索 callback、idempotent、consume、order status 与 traceId。','task_logs_001'],
    ['kb_003','订单与支付流水一致性检查','SQL 经验库','mod_pay_order','P1','待审核',1,'核对 order_info 与 payment_record 的状态、金额和更新时间。','task_sql_001'],
    ['kb_004','支付回调改动回归规则','回归规则库','mod_pay_callback','P0','已发布',1,'必回归成功、失败、重复回调、流水、优惠券和退款。',null]
  ].forEach(r=>insertKnowledge.run(...r,'',now(),now()));

  const insertTask = db.prepare('INSERT INTO tasks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
  [
    ['task_bug_001','bug','支付成功但订单状态未同步','mod_pay_order','P0','已完成','审核通过','已入库',null,{phenomenon:'支付成功，订单仍待支付'},{summary:'回调消费后订单事务回滚',suggestions:['核对消费日志','校验事务边界']},'DeepSeek'],
    ['task_logs_001','logs','支付回调消费超时排查','mod_pay_callback','P1','已完成','审核通过','已入库',null,{traceId:'trace-demo-1024'},{timeline:['10:20:01 接收回调','10:20:04 MQ 超时'],cause:'消费端连接池等待'},'通义千问'],
    ['task_sql_001','sql','订单与支付流水一致性分析','mod_pay_order','P1','待审核','待审核','未入库',null,{sql:'SELECT ...'},{risk:'状态字段存在延迟窗口',suggestion:'按 transaction_id 建立联合索引'},'OpenAI'],
    ['task_case_001','cases','优惠券叠加规则用例生成','mod_order_coupon','P1','已完成','审核通过','未入库',null,{requirement:'优惠券叠加规则'},{count:12,p0:2},'DeepSeek']
  ].forEach(r=>insertTask.run(...r.slice(0,9),json(r[9]),json(r[10]),r[11],now(),now()));

  const items=[{id:'ri_1',caseId:'case_001',title:'支付回调幂等校验',priority:'P0',status:'通过',expected:'只处理一次',actual:'通过',owner:'我',moduleId:'mod_pay_callback'},{id:'ri_2',caseId:'case_002',title:'订单状态同步',priority:'P1',status:'失败',expected:'订单已支付',actual:'偶发仍为待支付',owner:'我',moduleId:'mod_pay_order'},{id:'ri_3',bugId:'task_bug_001',title:'重复回调历史缺陷验证',priority:'P0',status:'未执行',expected:'缺陷不再出现',actual:'',owner:'我',moduleId:'mod_pay_callback'}];
  db.prepare('INSERT INTO regressions VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run('reg_001','支付回调优化回归清单','mod_pay_callback','执行中',67,json({version:'v2.8.3',items}),now(),now());
  db.prepare('INSERT INTO reports VALUES (?, ?, ?, ?, ?, ?, ?, ?)').run('report_001','支付回调优化测试报告','电商平台','有条件上线','v2.8.3',json({modules:['支付回调','订单同步'],passed:18,failed:1,blocked:1,sources:['reg_001','task_bug_001'],risks:['订单状态同步存在偶发失败']}),now(),now());
  db.prepare('INSERT INTO workflows VALUES (?, ?, ?, ?, ?, ?)').run('wf_001','支付异常完整排查链',json(['bug','logs','sql','regression','report']),'模板',now(),now());
  const p=db.prepare('INSERT INTO providers VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
  p.run('openai','OpenAI','https://api.openai.com/v1','gpt-5-mini',0.2,60,4096,0,'');
  p.run('qwen','通义千问','https://dashscope.aliyuncs.com/compatible-mode/v1','qwen-plus',0.2,60,4096,0,'');
  p.run('deepseek','DeepSeek','https://api.deepseek.com/v1','deepseek-chat',0.2,60,4096,0,'');
  p.run('custom','自定义兼容接口','','',0.2,60,4096,0,'');
}
seed();
```

**块 4 · 装配、映射、查询引擎与审计工具（段 7–11，真实 L79–91）**——对应 §8.16、§4.5、§5.1、§5.2、§9.3：

```js
const workbench = createWorkbench(db);

if (!db.prepare("SELECT 1 FROM relations WHERE source_type='task' AND source_id='task_bug_001' AND target_type='knowledge' AND target_id='kb_004'").get()) {
  db.prepare('INSERT INTO relations(source_type,source_id,target_type,target_id,label) VALUES(?,?,?,?,?)').run('task','task_bug_001','knowledge','kb_004','Bug 分析引用回归规则');
}

function moduleLabel(moduleId){if(!moduleId)return '';const m=db.prepare('SELECT * FROM modules WHERE id=?').get(moduleId);return m?`${m.project_name} / ${m.module_name} / ${m.submodule_name}`:'';}
function mapRow(resource,row){if(!row)return null;const camel=Object.fromEntries(Object.entries(row).map(([k,v])=>[k.replace(/_([a-z])/g,(_,c)=>c.toUpperCase()),v]));for(const key of ['inputJson','resultJson','contentJson','dataJson','stepsJson','valueJson'])if(key in camel){camel[key.replace('Json','')]=parse(camel[key]);delete camel[key];}if(camel.moduleId)camel.moduleLabel=moduleLabel(camel.moduleId);if(resource==='providers')camel.keyConfigured=Boolean(camel.keyMask);return camel;}

const resources = {
  tasks: { table:'tasks', search:['title','type','status','review_status'], order:'updated_at' },
  cases: { table:'cases', search:['code','title','priority','status'], order:'updated_at', where:'archived=0' },
  knowledge: { table:'knowledge', search:['title','category','content','status'], order:'updated_at' },
  regressions: { table:'regressions', search:['title','status'], order:'updated_at' },
  reports: { table:'reports', search:['title','project_name','verdict'], order:'updated_at' },
  workflows: { table:'workflows', search:['name','status'], order:'updated_at' },
  modules: { table:'modules', search:['project_name','module_name','submodule_name','status'], order:'sort_order' },
  providers: { table:'providers', search:['name','model'], order:'name' }
};

function listResource(resource, url) {
  const c = resources[resource];
  if (!c) return null;
  const page = Math.max(1, Number(url.searchParams.get('page') || 1));
  const pageSize = [20, 50, 100].includes(Number(url.searchParams.get('pageSize')))
    ? Number(url.searchParams.get('pageSize')) : 20;
  const q = (url.searchParams.get('q') || '').trim();
  const clauses = c.where ? [c.where] : [];
  const params = [];
  if (q) {
    clauses.push(`(${c.search.map(k => `${k} LIKE ?`).join(' OR ')})`);
    params.push(...c.search.map(() => `%${q}%`));
  }
  ['type','status','module_id','category','priority','risk'].forEach(key => {
    const value = url.searchParams.get(key);
    if (value) { clauses.push(`${key}=?`); params.push(value); }
  });
  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
  const total = db.prepare(`SELECT COUNT(*) AS n FROM ${c.table} ${where}`).get(...params).n;
  const rows = db.prepare(`SELECT * FROM ${c.table} ${where} ORDER BY ${c.order} DESC LIMIT ? OFFSET ?`).all(...params, pageSize, (page - 1) * pageSize);
  return { items: rows.map(row => mapRow(resource, row)), page, pageSize, total, pages: Math.max(1, Math.ceil(total / pageSize)) };
}

function send(res, status, payload) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(payload));
}

async function readBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {};
}

function audit(type, entityId, action, detail = '') {
  db.prepare('INSERT INTO audit(entity_type,entity_id,action,detail,created_at) VALUES(?,?,?,?,?)')
    .run(type, entityId, action, detail, now());
}

const { save: saveProviderKey, read: readProviderKey } = providerKeys(dataDir);
```

> 注意段 11 里的 `send`：它由第 3 章创建（当时在常量之后）；加完查询引擎后挪到 `listResource` 之后。位置变化不影响运行（函数声明会提升），但最终形态以真实文件为准。

**块 5 · `handleApi` 全量（上：入口与读路由，真实 L93–105）**

```js
async function handleApi(req, res, url) {
  const parts = url.pathname.split('/').filter(Boolean);
  if (parts[0] !== 'api') return false;
  try {
    if (parts[1] === 'workbench') return await workbench.handle(req, res, url, parts);   // ← §8.16 点亮

    // —— 聚合与读 ——
    if (req.method === 'GET' && parts[1] === 'bootstrap') {
      const counts = {};
      for (const [k, v] of Object.entries(resources)) counts[k] = db.prepare(`SELECT COUNT(*) AS n FROM ${v.table}`).get().n;
      return send(res, 200, {
        counts,
        modules: db.prepare('SELECT * FROM modules ORDER BY project_name,module_name,sort_order').all().map(r => mapRow('modules', r)),
        providers: db.prepare('SELECT * FROM providers ORDER BY name').all().map(r => mapRow('providers', r))
      });
    }
    if (req.method === 'GET' && resources[parts[1]] && !parts[2]) return send(res, 200, listResource(parts[1], url));
    if (req.method === 'GET' && resources[parts[1]] && parts[2]) {
      const row = db.prepare(`SELECT * FROM ${resources[parts[1]].table} WHERE id=?`).get(parts[2]);
      if (!row) return send(res, 404, { error: '记录不存在' });
      const mapped = mapRow(parts[1], row);
      if (parts[1] === 'knowledge') {
        mapped.dependencies = db.prepare("SELECT * FROM relations WHERE target_type='knowledge' AND target_id=? AND active=1").all(parts[2]).map(r => mapRow('relations', r));
        mapped.referenceCount = mapped.dependencies.length;
      }
      return send(res, 200, mapped);
    }

    // —— 任务 ——
    if (req.method === 'POST' && parts[1] === 'tasks') {
      const d = await readBody(req), taskId = makeId('task'), ts = now();
      db.prepare('INSERT INTO tasks VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)').run(taskId, d.type, d.title, d.moduleId || null, d.risk || 'P1', d.status || '草稿', '未提交', '未入库', d.parentId || null, json(d.input), json(d.result), d.model || '本地规则', ts, ts);
      audit('task', taskId, '创建', d.type);
      return send(res, 201, mapRow('tasks', db.prepare('SELECT * FROM tasks WHERE id=?').get(taskId)));
    }
    if (req.method === 'PATCH' && parts[1] === 'tasks' && parts[2]) {
      const d = await readBody(req), c = db.prepare('SELECT * FROM tasks WHERE id=?').get(parts[2]);
      if (!c) return send(res, 404, { error: '任务不存在' });
      db.prepare('UPDATE tasks SET title=?,module_id=?,risk=?,status=?,review_status=?,knowledge_status=?,input_json=?,result_json=?,updated_at=? WHERE id=?').run(d.title ?? c.title, d.moduleId ?? c.module_id, d.risk ?? c.risk, d.status ?? c.status, d.reviewStatus ?? c.review_status, d.knowledgeStatus ?? c.knowledge_status, json(d.input ?? parse(c.input_json)), json(d.result ?? parse(c.result_json)), now(), parts[2]);
      audit('task', parts[2], '更新', d.action || '编辑任务');
      return send(res, 200, mapRow('tasks', db.prepare('SELECT * FROM tasks WHERE id=?').get(parts[2])));
    }

    // —— 用例 ——
    if (req.method === 'POST' && parts[1] === 'cases') {
      const d = await readBody(req), caseId = makeId('case'), ts = now(), code = d.code || `TC-${Date.now().toString().slice(-6)}`;
      db.prepare('INSERT INTO cases VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)').run(caseId, code, d.title, d.moduleId || null, d.priority || 'P1', d.caseType || '功能', 1, d.status || '未执行', d.sourceTaskId || null, json(d.content), 0, ts, ts);
      audit('case', caseId, '创建');
      return send(res, 201, mapRow('cases', db.prepare('SELECT * FROM cases WHERE id=?').get(caseId)));
    }
    if (req.method === 'PATCH' && parts[1] === 'cases' && parts[2]) {
      const d = await readBody(req), c = db.prepare('SELECT * FROM cases WHERE id=?').get(parts[2]);
      if (!c) return send(res, 404, { error: '用例不存在' });
      db.prepare('UPDATE cases SET title=?,module_id=?,priority=?,case_type=?,version=version+1,status=?,content_json=?,archived=?,updated_at=? WHERE id=?').run(d.title ?? c.title, d.moduleId ?? c.module_id, d.priority ?? c.priority, d.caseType ?? c.case_type, d.status ?? c.status, json(d.content ?? parse(c.content_json)), d.archived ?? c.archived, now(), parts[2]);
      audit('case', parts[2], '更新版本');
      return send(res, 200, mapRow('cases', db.prepare('SELECT * FROM cases WHERE id=?').get(parts[2])));
    }

    // —— 知识 ——
    if (req.method === 'POST' && parts[1] === 'knowledge') {
      const d = await readBody(req), kid = makeId('kb'), ts = now();
      db.prepare('INSERT INTO knowledge VALUES(?,?,?,?,?,?,?,?,?,?,?,?)').run(kid, d.title, d.category, d.moduleId || null, d.risk || 'P1', '待审核', 1, d.content, d.sourceTaskId || null, '', ts, ts);
      audit('knowledge', kid, '创建');
      return send(res, 201, mapRow('knowledge', db.prepare('SELECT * FROM knowledge WHERE id=?').get(kid)));
    }
    if (req.method === 'PATCH' && parts[1] === 'knowledge' && parts[2]) {
      const d = await readBody(req), c = db.prepare('SELECT * FROM knowledge WHERE id=?').get(parts[2]);
      if (!c) return send(res, 404, { error: '知识不存在' });
      if (d.action === 'withdraw') {
        const deps = db.prepare("SELECT * FROM relations WHERE target_type='knowledge' AND target_id=? AND active=1").all(parts[2]);
        if (deps.length) return send(res, 409, { error: '存在有效关联，不能撤销入库', dependencies: deps });
      }
      const status = d.action === 'approve' ? '已发布' : d.action === 'reject' ? '已驳回' : d.action === 'withdraw' ? '已撤销' : d.status ?? c.status;
      db.prepare('UPDATE knowledge SET title=?,category=?,module_id=?,risk=?,status=?,version=version+1,content=?,review_note=?,updated_at=? WHERE id=?').run(d.title ?? c.title, d.category ?? c.category, d.moduleId ?? c.module_id, d.risk ?? c.risk, status, d.content ?? c.content, d.reviewNote ?? c.review_note, now(), parts[2]);
      audit('knowledge', parts[2], d.action || '编辑', d.reviewNote || '');
      return send(res, 200, mapRow('knowledge', db.prepare('SELECT * FROM knowledge WHERE id=?').get(parts[2])));
    }

    // —— 回归 ——
    if (req.method === 'PATCH' && parts[1] === 'regressions' && parts[2]) {
      const d = await readBody(req), c = db.prepare('SELECT * FROM regressions WHERE id=?').get(parts[2]);
      if (!c) return send(res, 404, { error: '回归清单不存在' });
      const value = d.data ?? parse(c.data_json), items = value.items || [];
      const complete = items.filter(i => !['未执行','阻塞'].includes(i.status)).length;
      const progress = items.length ? Math.round(complete / items.length * 100) : 0;
      db.prepare('UPDATE regressions SET status=?,progress=?,data_json=?,updated_at=? WHERE id=?').run(d.status ?? (progress === 100 ? '已完成' : '执行中'), progress, json(value), now(), parts[2]);
      audit('regression', parts[2], '更新执行结果');
      return send(res, 200, mapRow('regressions', db.prepare('SELECT * FROM regressions WHERE id=?').get(parts[2])));
    }
```

**块 6 · `handleApi` 全量（下：写路由与收尾，真实 L106–120）**

```js
    // —— 报告 / 工作流 / 模块 ——
    if (req.method === 'POST' && parts[1] === 'reports') {
      const d = await readBody(req), rid = makeId('report'), ts = now();
      db.prepare('INSERT INTO reports VALUES(?,?,?,?,?,?,?,?)').run(rid, d.title, d.projectName || '未指定项目', d.verdict || '有条件上线', d.version || 'v1.0', json(d.data), ts, ts);
      (d.data?.sources || []).forEach(s => db.prepare('INSERT INTO relations(source_type,source_id,target_type,target_id,label) VALUES(?,?,?,?,?)').run('report', rid, s.type, s.id, '报告来源'));
      audit('report', rid, '生成');
      return send(res, 201, mapRow('reports', db.prepare('SELECT * FROM reports WHERE id=?').get(rid)));
    }
    if (req.method === 'POST' && parts[1] === 'workflows') {
      const d = await readBody(req), wid = makeId('wf'), ts = now();
      db.prepare('INSERT INTO workflows VALUES(?,?,?,?,?,?)').run(wid, d.name, json(d.steps), d.status || '模板', ts, ts);
      audit('workflow', wid, '创建');
      return send(res, 201, mapRow('workflows', db.prepare('SELECT * FROM workflows WHERE id=?').get(wid)));
    }
    if (req.method === 'POST' && parts[1] === 'modules') {
      const d = await readBody(req), mid = makeId('mod');
      db.prepare('INSERT INTO modules VALUES(?,?,?,?,?,?,?)').run(mid, d.projectName, d.moduleName, d.submoduleName, '启用', d.sortOrder || 99, now());
      audit('module', mid, '创建');
      return send(res, 201, mapRow('modules', db.prepare('SELECT * FROM modules WHERE id=?').get(mid)));
    }
    if (req.method === 'PATCH' && parts[1] === 'modules' && parts[2]) {
      const d = await readBody(req), c = db.prepare('SELECT * FROM modules WHERE id=?').get(parts[2]);
      if (!c) return send(res, 404, { error: '模块不存在' });
      db.prepare('UPDATE modules SET project_name=?,module_name=?,submodule_name=?,status=?,sort_order=?,updated_at=? WHERE id=?').run(d.projectName ?? c.project_name, d.moduleName ?? c.module_name, d.submoduleName ?? c.submodule_name, d.status ?? c.status, d.sortOrder ?? c.sort_order, now(), parts[2]);
      audit('module', parts[2], '更新');
      return send(res, 200, mapRow('modules', db.prepare('SELECT * FROM modules WHERE id=?').get(parts[2])));
    }

    // —— 模型配置（§9.4–9.6 点亮）——
    if(req.method==='PATCH'&&parts[1]==='providers'&&parts[2]){const d=await readBody(req),c=db.prepare('SELECT * FROM providers WHERE id=?').get(parts[2]);if(!c)return send(res,404,{error:'模型厂商不存在'});if(d.apiKey)await saveProviderKey(parts[2],d.apiKey);const mask=d.apiKey?`${d.apiKey.slice(0,3)}••••${d.apiKey.slice(-3)}`:c.key_mask;db.prepare('UPDATE providers SET base_url=?,model=?,temperature=?,timeout=?,max_tokens=?,enabled=?,key_mask=? WHERE id=?').run(d.baseUrl??c.base_url,d.model??c.model,d.temperature??c.temperature,d.timeout??c.timeout,d.maxTokens??c.max_tokens,d.enabled?1:0,mask,parts[2]);audit('provider',parts[2],'更新配置');return send(res,200,mapRow('providers',db.prepare('SELECT * FROM providers WHERE id=?').get(parts[2])));}
    if(req.method==='POST'&&parts[1]==='providers'&&parts[3]==='test'){const p=db.prepare('SELECT * FROM providers WHERE id=?').get(parts[2]),key=await readProviderKey(parts[2]);if(!p||!key)return send(res,400,{error:'请先保存 API Key'});const started=Date.now(),response=await fetch(`${p.base_url.replace(/\/$/,'')}/models`,{headers:{Authorization:`Bearer ${key}`},signal:AbortSignal.timeout(p.timeout*1000)});if(!response.ok)return send(res,502,{error:`连接失败：HTTP ${response.status}`});return send(res,200,{ok:true,latency:Date.now()-started});}
    if(req.method==='POST'&&parts[1]==='ai'&&parts[2]==='analyze'){const d=await readBody(req),p=db.prepare('SELECT * FROM providers WHERE id=? AND enabled=1').get(d.providerId);if(!p)return send(res,400,{error:'请选择并启用一个模型配置'});const key=await readProviderKey(p.id);if(!key)return send(res,400,{error:'模型 API Key 未配置'});const response=await fetch(`${p.base_url.replace(/\/$/,'')}/chat/completions`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${key}`},body:json({model:p.model,temperature:p.temperature,max_tokens:p.max_tokens,messages:[{role:'system',content:'你是测试分析助手。仅返回合法 JSON，包含 summary、risk、suggestions、missingFields。'},{role:'user',content:`${d.taskType}\n${d.text}` }]}),signal:AbortSignal.timeout(p.timeout*1000)});if(!response.ok)return send(res,502,{error:`模型调用失败：HTTP ${response.status}`});const result=await response.json(),content=result.choices?.[0]?.message?.content||'{}';return send(res,200,{provider:p.name,model:p.model,result:parse(content.replace(/^```json\s*|\s*```$/g,''),{summary:content})});}

    // —— 数据迁移（§11.1 点亮）——
    if (req.method === 'POST' && parts[1] === 'migrate') {
      // —— 第 ① 段：读请求体 + 幂等检查 ——
      const d = await readBody(req),
            old = db.prepare('SELECT * FROM migrations WHERE key=?').get('localstorage-v1');
      if (old) return send(res, 200, { migrated: false, reason: 'already-migrated', backupPath: old.backup_path });
      // —— 第 ② 段：备份先行 ——
      const backupPath = join(dataDir, 'backups', `localstorage-${Date.now()}.json`);
      await writeFile(backupPath, JSON.stringify(d, null, 2), { flag: 'wx' });
      // —— 第 ③ 段：导入任务 ——
      const imported = { tasks: 0, knowledge: 0 };
      for (const item of d.tasks || []) {
        const tid = item.id || makeId('legacy_task');
        if (db.prepare('SELECT 1 FROM tasks WHERE id=?').get(tid)) continue;
        db.prepare('INSERT INTO tasks VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)').run(
          tid, item.type || 'bug', item.title || '旧版任务', null, 'P1', item.status || '已完成',
          '未提交', '未入库', null, json(item.input || {}), json(item.result || {}),
          '旧版规则', item.createdAt || now(), now()
        );
        imported.tasks++;
      }
      // —— 第 ④ 段：导入知识 ——
      for (const item of d.knowledge || []) {
        const kid = item.id || makeId('legacy_kb');
        if (db.prepare('SELECT 1 FROM knowledge WHERE id=?').get(kid)) continue;
        db.prepare('INSERT INTO knowledge VALUES(?,?,?,?,?,?,?,?,?,?,?,?)').run(
          kid, item.title || '旧版知识', item.category || '业务规则库', null, item.risk || 'P1',
          item.status || '待审核', 1, item.content || '', null, '从旧版迁移',
          item.updatedAt || now(), now()
        );
        imported.knowledge++;
      }
      // —— 第 ⑤ 段：登记与审计 ——
      db.prepare('INSERT INTO migrations VALUES(?,?,?)').run('localstorage-v1', now(), backupPath);
      audit('migration', 'localstorage-v1', '完成', json(imported));
      // —— 第 ⑥ 段：响应 ——
      return send(res, 200, { migrated: true, imported, backupPath });
    }

    // —— 设置与审计 ——
    if (req.method === 'GET' && parts[1] === 'settings' && parts[2]) {
      const row = db.prepare('SELECT value_json FROM settings WHERE key=?').get(parts[2]);
      return send(res, 200, { key: parts[2], value: row ? parse(row.value_json) : null });
    }
    if (req.method === 'PATCH' && parts[1] === 'settings' && parts[2]) {
      const d = await readBody(req);
      db.prepare('INSERT INTO settings(key,value_json) VALUES(?,?) ON CONFLICT(key) DO UPDATE SET value_json=excluded.value_json').run(parts[2], json(d.value));
      audit('settings', parts[2], '全量同步模板');
      return send(res, 200, { key: parts[2], value: d.value });
    }
    if (req.method === 'GET' && parts[1] === 'audit') {
      const rows = db.prepare('SELECT * FROM audit WHERE entity_type=? AND entity_id=? ORDER BY id DESC').all(url.searchParams.get('type'), url.searchParams.get('id'));
      return send(res, 200, { items: rows.map(r => mapRow('audit', r)) });
    }

    return send(res, 404, { error: '接口不存在' });
  } catch (error) {
    console.error(error);
    return send(res, 500, { error: error.message || '服务端错误' });
  }
}
```

**块 7 · 静态服务与启动（段 13–14，真实 L122–127）**——对应 §3.4、§3.5、§3.6：

```js
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

### C.2.3 装配检查点

装完七块后，按这张表快速自检：

| 检查 | 预期 |
| --- | --- |
| `npm run check` | 无输出（`server.mjs` 不参与 tsc，此项只保前端） |
| `npm run dev` | 一行「TestPilot 已启动……」；出现 `data/` 目录 |
| `curl.exe -s http://127.0.0.1:3344/api/bootstrap` | 返回含 `counts` / `modules` / `providers` 的 JSON |
| 数一数 `if (req.method` 开头的路由 | 共 22 条（不含 `parts[1]==='workbench'` 转发行） |
| 数一数 `import` | 共 8 行 |
| 搜索 `第 8 章接入` / `在第 9 章` 之类的占位注释 | **应该全部消失**——如果还有残留，说明对应章节的「点亮」没做 |

---

## C.3 教学版 workbench-api.mjs 全量装配

### C.3.1 段地图（15 段）

| # | 段 | 教学版来源 | 真实行号 | 一句话职责 |
| --- | --- | --- | --- | --- |
| 1 | `ENTRY_DEFS` 七入口数据表 | §8.1 | L1–9 | 数据驱动的核心：触发词 / 必填 / 知识映射 / 工作流 |
| 2 | 排序基准与五个小工具 | §8.2 | L10–15 | DEFAULT_ORDER、now / parse / json / makeId / camel |
| 3 | 两个归一化函数 | §8.3 | L16–18 | normalizeAsset / normalizeRun（行 → 对象） |
| 4 | `fieldPresent` 字段规则表 | §8.4 | L19–25 | 缺项判定引擎（正则规则表） |
| 5 | `routeText` 触发词路由 | §8.5 | L26 | 一个函数定「跑哪几条」 |
| 6 | `buildOutput` 结构化输出 | §8.6 | L27–36 | 每个入口的输出 Schema（含人工复核点） |
| 7 | `createWorkbench` 装配与种子 | §8.7 | L38–48 | 4 张表 + INSERT OR IGNORE 种子 |
| 8 | 请求工具与两个读取器 | §8.8 | L50–53 | send / body / assets / getRun |
| 9 | `handle` 入口与 GET /config | §8.9 | L54–56 | 路由入口 + 引擎配置 |
| 10 | POST /preflight | §8.10 | L57 | 识别任务与缺项（不落库） |
| 11 | POST /runs | §8.11 | L58 | 执行管道（落库 + 步骤轨迹） |
| 12 | GET /runs 列表与详情 | §8.12 | L59–60 | 分页 + 单条（带步骤） |
| 13 | PATCH /runs/:id | §8.13 | L61 | 复核状态机（通过 / 驳回） |
| 14 | GET / PATCH /assets | §8.14 | L62–63 | 资产读取与版本化编辑 |
| 15 | 案例验证与收尾 | §8.15 | L64–69 | validations 读 / 跑 + 兜底 + 工厂返回 |

> 与 `server.mjs` 不同：这个文件的教学版**本身就是压缩格式**（每行就是真实文件那一行，8 章正文只是把它拆开讲解）。所以下面这段代码与真实文件**逐字一致**。

### C.3.2 完整代码（69 行）

```js
const ENTRY_DEFS = [
  {id:'testcase_gen',name:'测试用例生成',icon:'cases',triggers:['需求','验收','用例','场景','测试点'],required:['需求背景','验收标准'],knowledge:['业务规则库','接口异常库','历史 Bug 库'],workflow:['校验输入','拆解测试点','覆盖正常/异常/边界','生成结构化用例','标记人工复核']},
  {id:'bug_analysis',name:'Bug 分析',icon:'bug',triggers:['bug','缺陷','复现','实际结果','预期结果','报错','异常'],required:['问题标题','复现步骤','实际结果','预期结果','环境'],knowledge:['历史 Bug 库','日志规律库','SQL 经验库','业务规则库'],workflow:['校验输入','描述现象','识别影响模块','给出日志方向','给出 SQL 方向','检索历史 Bug','生成回归范围','判断风险','标记人工复核']},
  {id:'log_triage',name:'日志排查',icon:'logs',triggers:['日志','traceid','exception','error','timeout','堆栈','调用链','lock wait'],required:['日志或 traceId','时间范围','运行环境'],knowledge:['日志规律库','历史 Bug 库'],workflow:['提取关键字','识别异常','还原调用链','定位可疑模块','匹配历史规律','给出排查建议','标记证据缺口']},
  {id:'sql_analysis',name:'SQL 分析',icon:'sql',triggers:['sql','select ','update ','insert ','delete ','join ','explain','索引','慢查询','数据库'],required:['SQL 文本','分析目标'],knowledge:['SQL 经验库','压测经验库'],workflow:['识别风险','检查 WHERE','给出索引验证方向','分析慢查询','检查 JOIN','检查 ORDER BY','判断性能风险','建议 EXPLAIN 验证']},
  {id:'regression_list',name:'回归清单',icon:'regression',triggers:['回归','影响范围','改动模块','发布范围','变更范围'],required:['版本或变更内容','影响模块'],knowledge:['回归规则库','历史 Bug 库'],workflow:['识别直接变更','扩展上下游','还原业务链路','检索历史 Bug','评估风险','生成必测/建议项','人工复核']},
  {id:'test_report',name:'测试报告',icon:'report',triggers:['测试报告','上线结论','发布报告','质量结论'],required:['版本','测试范围','执行结果'],knowledge:['业务规则库','历史 Bug 库','回归规则库'],workflow:['汇总测试范围','统计执行结果','汇总缺陷','识别风险模块','整理遗留问题','生成上线建议','人工审批']},
  {id:'prompt_test',name:'Prompt 测试',icon:'prompt',triggers:['prompt','提示词','模型输出','回答质量','幻觉'],required:['Prompt 文本','预期输出或评价标准'],knowledge:[],workflow:['读取 Prompt','准备样例','逐项运行','对照 Schema','检查事实与幻觉','记录差异','人工判定']}
];
const DEFAULT_ORDER=ENTRY_DEFS.map(x=>x.id);
const now=()=>new Date().toISOString();
const parse=(v,f={})=>{try{return JSON.parse(v)}catch{return f}};
const json=v=>JSON.stringify(v??{});
const makeId=p=>`${p}_${crypto.randomUUID().slice(0,8)}`;
const camel=row=>row?Object.fromEntries(Object.entries(row).map(([k,v])=>[k.replace(/_([a-z])/g,(_,c)=>c.toUpperCase()),v])):null;

function normalizeAsset(row){const x=camel(row);x.triggers=parse(x.triggersJson,[]);x.required=parse(x.requiredJson,[]);x.knowledge=parse(x.knowledgeJson,[]);x.workflow=parse(x.workflowJson,[]);delete x.triggersJson;delete x.requiredJson;delete x.knowledgeJson;delete x.workflowJson;return x}
function normalizeRun(row){const x=camel(row);for(const k of ['routesJson','inputJson','resultJson','missingJson','reviewJson','evidenceJson']){x[k.replace('Json','')]=parse(x[k],k==='routesJson'||k==='missingJson'||k==='evidenceJson'?[]:{});delete x[k]}return x}
function fieldPresent(text,field){const rules={
  '问题标题':/.{5,}/,'复现步骤':/复现|步骤|操作|点击|请求/,'实际结果':/实际|现象|仍为|报错|失败/,'预期结果':/预期|应该|应为/,'环境':/环境|staging|test|prod|生产|测试环境/,
  '日志或 traceId':/日志|trace\s?id|exception|error|timeout|堆栈/i,'时间范围':/\d{1,2}:\d{2}|\d{4}-\d{1,2}-\d{1,2}|时间/,'运行环境':/环境|staging|test|prod|生产/,
  'SQL 文本':/select |update |insert |delete |join |explain /i,'分析目标':/分析|排查|优化|慢|风险|一致性/,
  '需求背景':/需求|背景|功能|规则/,'验收标准':/验收|预期|应当|必须/,'版本或变更内容':/版本|v\d|变更|改动/,'影响模块':/模块|支付|订单|账户|接口/,
  '版本':/版本|v\d/,'测试范围':/范围|模块|用例|回归/,'执行结果':/通过|失败|阻塞|执行/,'Prompt 文本':/prompt|提示词/i,'预期输出或评价标准':/预期|标准|schema|格式/i
};return rules[field]?.test(text)||false}
function routeText(text,selected=[]){const lower=text.toLowerCase();let ids=ENTRY_DEFS.filter(e=>e.triggers.some(t=>lower.includes(t.toLowerCase()))).map(e=>e.id);selected.forEach(id=>{if(!ids.includes(id))ids.push(id)});if(!ids.length)ids=['bug_analysis'];ids.sort((a,b)=>DEFAULT_ORDER.indexOf(a)-DEFAULT_ORDER.indexOf(b));return ids}
function buildOutput(id,text,risk){
  const common={risk_level:risk,human_review_points:['确认事实与引用依据','P0/P1 风险和上线结论必须由测试人最终确认'],need_more_info:[]};
  if(id==='bug_analysis')return {...common,phenomenon:text.slice(0,180),affected_modules:['根据已选三级模块确认'],possible_causes:['从现象推测存在状态同步或事务边界异常，尚需证据验证'],recommended_logs:['按 traceId 串联入口、消息消费和数据库更新日志'],recommended_sql:['核对业务主表与流水表状态、金额及更新时间'],related_history_bugs:['仅展示实际检索命中的知识，不编造 Bug 编号'],regression_scope:['直接变更模块','上下游状态同步','失败与重复请求场景']};
  if(id==='log_triage')return {...common,anomaly_summary:'已提取异常关键词与时间线，根因仍需结合完整上下文确认',timeline:['接收用户证据','定位首个异常片段','向上下游扩展调用链'],suspect_modules:['待结合 traceId 与模块配置确认'],suggestions:['补充异常前后至少 30 行日志','核对同 traceId 的入口与消费端日志']};
  if(id==='sql_analysis')return {...common,risks:['需要检查过滤条件、JOIN、排序和事务一致性'],where_review:'确认过滤条件是否命中预期数据范围',index_advice:'仅作为验证方向；需结合 EXPLAIN、数据量和基数判断',explain_suggestions:['在测试环境执行 EXPLAIN/EXPLAIN ANALYZE','保存执行计划与耗时证据']};
  if(id==='regression_list')return {...common,must_regression:['直接变更功能','核心成功链路','失败链路','重复请求与幂等','上下游状态同步','历史高风险缺陷'],suggested_regression:['边界数据','权限与兼容性'],completion_rule:'至少 6 个必回归项，经人工确认后执行'};
  if(id==='testcase_gen')return {...common,coverage:['正常','异常','边界','权限','幂等'],case_format:['标题','前置条件','步骤','预期结果','优先级'],suggestions:['生成后按功能与子模块入用例库']};
  if(id==='test_report')return {...common,sections:['测试范围','执行结果','缺陷统计','风险模块','遗留问题','上线结论'],release_decision:'待测试负责人审批，AI 不做最终上线批准'};
  return {...common,checks:['Schema 完整性','事实准确性','引用可追溯','幻觉风险'],decision:'待人工最终判定'};
}

export function createWorkbench(db){
  db.exec(`
    CREATE TABLE IF NOT EXISTS workbench_assets (id TEXT PRIMARY KEY, name TEXT NOT NULL, icon TEXT NOT NULL, triggers_json TEXT NOT NULL, required_json TEXT NOT NULL, knowledge_json TEXT NOT NULL, workflow_json TEXT NOT NULL, version INTEGER NOT NULL DEFAULT 1, status TEXT NOT NULL DEFAULT '已发布', updated_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS workflow_runs (id TEXT PRIMARY KEY, title TEXT NOT NULL, main_route TEXT NOT NULL, routes_json TEXT NOT NULL, module_id TEXT, status TEXT NOT NULL, risk TEXT NOT NULL, input_json TEXT NOT NULL, result_json TEXT NOT NULL, missing_json TEXT NOT NULL, review_json TEXT NOT NULL, evidence_json TEXT NOT NULL, created_at TEXT NOT NULL, updated_at TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS workflow_run_steps (id TEXT PRIMARY KEY, run_id TEXT NOT NULL, route_id TEXT NOT NULL, step_order INTEGER NOT NULL, step_name TEXT NOT NULL, status TEXT NOT NULL, output_json TEXT NOT NULL DEFAULT '{}', started_at TEXT, completed_at TEXT, FOREIGN KEY(run_id) REFERENCES workflow_runs(id));
    CREATE TABLE IF NOT EXISTS validation_cases (id TEXT PRIMARY KEY, name TEXT NOT NULL, input_text TEXT NOT NULL, expected_routes_json TEXT NOT NULL, expected_rules_json TEXT NOT NULL, status TEXT NOT NULL DEFAULT '待验证', last_result_json TEXT NOT NULL DEFAULT '{}', updated_at TEXT NOT NULL);
  `);
  const ins=db.prepare('INSERT OR IGNORE INTO workbench_assets VALUES(?,?,?,?,?,?,?,?,?,?)');
  ENTRY_DEFS.forEach(e=>ins.run(e.id,e.name,e.icon,json(e.triggers),json(e.required),json(e.knowledge),json(e.workflow),1,'已发布',now()));
  const sample='支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单；实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。请给出 SQL 排查方向和回归范围。';
  db.prepare('INSERT OR IGNORE INTO validation_cases VALUES(?,?,?,?,?,?,?,?)').run('val_payment','支付成功但订单未支付 · 完整材料',sample,json(['bug_analysis','log_triage','sql_analysis','regression_list']),json(['不跳结论','根因需证据','至少 6 个必回归项','P0/P1 人工复核']),'待验证',json({}),now());

  const send=(res,status,payload)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify(payload))};
  const body=async req=>{const chunks=[];for await(const c of req)chunks.push(c);return chunks.length?JSON.parse(Buffer.concat(chunks).toString('utf8')):{}};
  const assets=()=>db.prepare('SELECT * FROM workbench_assets ORDER BY rowid').all().map(normalizeAsset);
  const getRun=id=>{const row=db.prepare('SELECT * FROM workflow_runs WHERE id=?').get(id);if(!row)return null;const run=normalizeRun(row);run.steps=db.prepare('SELECT * FROM workflow_run_steps WHERE run_id=? ORDER BY step_order').all(id).map(s=>{const x=camel(s);x.output=parse(x.outputJson,{});delete x.outputJson;return x});return run};
  async function handle(req,res,url,parts){
    if(parts[1]!=='workbench')return false;
    if(req.method==='GET'&&parts[2]==='config')return send(res,200,{entries:assets(),dispatch:{multiRoutePolicy:'sequential_when_matched',defaultOrder:DEFAULT_ORDER},rules:['不跳步骤','证据不足时明确标记','不编造知识标题或 Bug 编号','AI 不批准上线','P0/P1 与 Prompt 通过需人工复核']});
    if(req.method==='POST'&&parts[2]==='preflight'){const d=await body(req),text=String(d.text||''),routes=routeText(text,d.selectedRoutes||[]),all=assets();const matched=routes.map((id,i)=>{const e=all.find(x=>x.id===id);const missing=e.required.filter(f=>!fieldPresent(text,f));return{...e,role:i===0?'主任务':'辅助任务',missing,completeness:Math.round((e.required.length-missing.length)/Math.max(1,e.required.length)*100)}});return send(res,200,{routes:matched,mainRoute:routes[0],missing:[...new Set(matched.flatMap(x=>x.missing))],canRun:Boolean(text.trim()),policy:'sequential_when_matched'});}
    if(req.method==='POST'&&parts[2]==='runs'&&!parts[3]){const d=await body(req),text=String(d.text||'').trim();if(!text)return send(res,400,{error:'请输入待分析内容'});const routes=routeText(text,d.selectedRoutes||[]),all=assets(),matched=routes.map(id=>all.find(x=>x.id===id)),missing=[...new Set(matched.flatMap(e=>e.required.filter(f=>!fieldPresent(text,f))))],risk=/生产|资损|重复扣款|数据丢失|安全|p0/i.test(text)?'P0':/失败|异常|超时|锁等待|lock wait/i.test(text)?'P1':'P2',runId=makeId('run'),ts=now();const result={sections:{},routeSummary:matched.map((e,i)=>({id:e.id,name:e.name,role:i===0?'主任务':'辅助任务',knowledge:e.knowledge}))};matched.forEach(e=>result.sections[e.id]=buildOutput(e.id,text,risk));Object.values(result.sections).forEach(o=>o.need_more_info=missing);const review={required:risk==='P0'||risk==='P1'||routes.includes('prompt_test'),status:'待复核',checks:['任务识别是否准确','证据与结论是否匹配','风险等级是否合理','输出格式是否完整']};db.prepare('INSERT INTO workflow_runs VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)').run(runId,d.title||text.split(/[。\n]/)[0].slice(0,42),routes[0],json(routes),d.moduleId||null,missing.length?'待补充':'待复核',risk,json({text,attachments:d.attachments||[]}),json(result),json(missing),json(review),json(['用户原始输入',...matched.flatMap(e=>e.knowledge.map(k=>`知识库：${k}`))]),ts,ts);let order=1;const stepIns=db.prepare('INSERT INTO workflow_run_steps VALUES(?,?,?,?,?,?,?,?,?)');matched.forEach(e=>e.workflow.forEach(name=>stepIns.run(makeId('step'),runId,e.id,order++,name,'已完成',json({note:'按工作流顺序执行',evidence:'用户输入与可用知识映射'}),ts,ts)));return send(res,201,getRun(runId));}
    if(req.method==='GET'&&parts[2]==='runs'&&!parts[3]){const page=Math.max(1,Number(url.searchParams.get('page')||1)),pageSize=[20,50,100].includes(Number(url.searchParams.get('pageSize')))?Number(url.searchParams.get('pageSize')):20,q=(url.searchParams.get('q')||'').trim(),status=url.searchParams.get('status')||'',clauses=[],params=[];if(q){clauses.push('(title LIKE ? OR input_json LIKE ?)');params.push(`%${q}%`,`%${q}%`)}if(status){clauses.push('status=?');params.push(status)}const where=clauses.length?`WHERE ${clauses.join(' AND ')}`:'';const total=db.prepare(`SELECT COUNT(*) n FROM workflow_runs ${where}`).get(...params).n,rows=db.prepare(`SELECT * FROM workflow_runs ${where} ORDER BY updated_at DESC LIMIT ? OFFSET ?`).all(...params,pageSize,(page-1)*pageSize).map(normalizeRun);return send(res,200,{items:rows,page,pageSize,total,pages:Math.max(1,Math.ceil(total/pageSize))});}
    if(req.method==='GET'&&parts[2]==='runs'&&parts[3]){const run=getRun(parts[3]);return run?send(res,200,run):send(res,404,{error:'执行记录不存在'});}
    if(req.method==='PATCH'&&parts[2]==='runs'&&parts[3]){const d=await body(req),row=db.prepare('SELECT * FROM workflow_runs WHERE id=?').get(parts[3]);if(!row)return send(res,404,{error:'执行记录不存在'});const review=parse(row.review_json,{});if(d.action==='approve'){review.status='已复核';review.note=d.note||'人工确认通过';review.reviewedAt=now()}if(d.action==='reject'){review.status='已驳回';review.note=d.note||'请补充证据';review.reviewedAt=now()}const status=d.action==='approve'?'已完成':d.action==='reject'?'待补充':d.status||row.status;db.prepare('UPDATE workflow_runs SET status=?,review_json=?,updated_at=? WHERE id=?').run(status,json(review),now(),parts[3]);return send(res,200,getRun(parts[3]));}
    if(req.method==='GET'&&parts[2]==='assets')return send(res,200,{items:assets()});
    if(req.method==='PATCH'&&parts[2]==='assets'&&parts[3]){const d=await body(req),row=db.prepare('SELECT * FROM workbench_assets WHERE id=?').get(parts[3]);if(!row)return send(res,404,{error:'工作台资产不存在'});db.prepare('UPDATE workbench_assets SET triggers_json=?,required_json=?,knowledge_json=?,workflow_json=?,version=version+1,status=?,updated_at=? WHERE id=?').run(json(d.triggers??parse(row.triggers_json,[])),json(d.required??parse(row.required_json,[])),json(d.knowledge??parse(row.knowledge_json,[])),json(d.workflow??parse(row.workflow_json,[])),d.status??row.status,now(),parts[3]);return send(res,200,normalizeAsset(db.prepare('SELECT * FROM workbench_assets WHERE id=?').get(parts[3])));}
    if(req.method==='GET'&&parts[2]==='validations')return send(res,200,{items:db.prepare('SELECT * FROM validation_cases ORDER BY updated_at DESC').all().map(r=>{const x=camel(r);x.expectedRoutes=parse(x.expectedRoutesJson,[]);x.expectedRules=parse(x.expectedRulesJson,[]);x.lastResult=parse(x.lastResultJson,{});delete x.expectedRoutesJson;delete x.expectedRulesJson;delete x.lastResultJson;return x})});
    if(req.method==='POST'&&parts[2]==='validations'&&parts[3]){const row=db.prepare('SELECT * FROM validation_cases WHERE id=?').get(parts[3]);if(!row)return send(res,404,{error:'验证案例不存在'});const actual=routeText(row.input_text),expected=parse(row.expected_routes_json,[]),passed=expected.every(x=>actual.includes(x)),result={actualRoutes:actual,expectedRoutes:expected,passed,checkedAt:now(),rules:parse(row.expected_rules_json,[])};db.prepare('UPDATE validation_cases SET status=?,last_result_json=?,updated_at=? WHERE id=?').run(passed?'通过':'失败',json(result),now(),parts[3]);return send(res,200,result);}
    return send(res,404,{error:'工作台接口不存在'});
  }
  return {handle};
}
```

### C.3.3 装配检查点

| 检查 | 预期 |
| --- | --- |
| 文件行数 | 69 行（与真实一致） |
| `server.mjs` 三处接线（§8.16） | import 1 行、`createWorkbench(db)` 1 行、`handleApi` 首行转发 1 行 |
| `curl.exe -s http://127.0.0.1:3344/api/workbench/config` | 返回 `entries`（7 项）+ `dispatch` + `rules` |
| 工作台验证页（沙箱）点「运行验证案例」 | 「验证通过：路由与规则符合预期」（§8.24 完整执行链的证据之一） |

---

## C.4 前端装配映射

前端三件套不是「从头新建」，而是**分阶段替换**。下面三张表把「哪个区段来自哪一章」映射清楚。

### C.4.1 main.tsx（200 行）装配表

| 区段 | 真实行号 | 来源 | 动作 |
| --- | --- | --- | --- |
| 导入 + 类型 + NAV(13 项) + TASK_META + KB_CATEGORIES | L1–34 | §2.7 → §6.1 | 从「最小可运行版」升级为完整静态配置（NAV 从每项一行展开为真实排版） |
| API 封装 | L35–41 | §6.2 | 后端适配层（统一 fetch + 错误抛出） |
| App 外壳 | L43–74 | §6.3 | 6 个 useState、2 个 useEffect、加载守卫、页面分发、Toast、迁移弹窗挂载 |
| 组件区（占位 → 真实） | L76–198 | §6.3 占位（12 行）→ §7.1–7.17 逐个替换 | 15 个页面组件、DetailDrawer 与 3 个详情子组件、2 个弹窗、TaskTable / KnowledgeExperience、16 个工具部件、工具函数与导出体系 |
| AI 接线 | L117、L139 | §9.7 | `AnalysisPage.run()` 走 `ai/analyze`（配置了模型时）；`ModelSettings` 的 save / test 走 providers 接口 |
| 迁移接线 | L51、L72、L159 | §11.2 | 启动检测旧数据 → 弹窗 →（完成回调清 localStorage）→ `migrate()` |
| 挂载 | L200 | §2.7（不变） | `createRoot` 渲染 `<App/>` |

### C.4.2 workbench.tsx（35 行）阶段表

| 阶段 | 内容 | 来源 |
| --- | --- | --- |
| 第 6 章 · 占位版 | 4 个导出（AssistantPage / RunsPage / OrchestrationPage / SandboxPage）仅保证可编译 | §6.4 |
| 第 8 章 · 真实版（整体替换） | L1–9 导入与工具（api / post / patch / routeIcon / dateTime）；L11–25 AssistantPage；L27 RunResult；L29 RunsPage；L31 OrchestrationPage；L33 SandboxPage；L35 WBadge | §8.17–8.22 |

### C.4.3 styles.css（14 行）阶段表

| 阶段 | 内容 | 来源 |
| --- | --- | --- |
| 第 6 章 · 设计体系（L1–11 逐字一致 + L12 空行） | 字体 @import、`:root` 令牌、应用骨架、顶栏与通用件、首页组件、表单、列表与表格、业务组件、浮层、响应式——**一整行就是一族样式** | §6.5 |
| 第 8 章 · `wb-*` 家族（L13–14 追加） | 小注释 + 工作台样式（`wb-page` / `wb-entry-grid` / `wb-result` …）——**追加，不替换** | §8.23 |

### C.4.4 前端装配顺序与验证

```text
§2.7 最小 main.tsx（Vite 自己的端口跑起来）
  → §6.1–6.3 替换三件套（导入/API/App + 12 行占位）
  → §6.4 新建 workbench.tsx 占位版          【检查点：npm run check 通过、外壳可见】
  → §6.5 替换 styles.css
  → §7.1–7.17 逐个替换组件区               【检查点：13 个导航页全部可点】
  → §8.16 后端接线 + §8.17–8.22 替换 workbench.tsx
  → §8.23 追加 wb-* 样式                    【检查点：AI 助手页可用、示例可跑】
  → §9.7 / §11.2 两处接线                   【检查点：模型配置可用、迁移弹窗可弹】
```

一句话原则：**先换成能跑的，再换成完整的**——每个检查点都是「可停留的里程碑」（对应第 12 章 M1–M4）。

---

## C.5 教学版与真实版的差异汇总

全书文件都是**分阶段生长**的：装到第 5 章时 `server.mjs` 还没有工作台转发；第 6 章的组件区全是占位。下表把「过渡形态 → 最终形态」一次铺开——对照答案时先来这里确认「不一样」是不是正常。

| # | 位置 | 过渡形态（那一刻的正确态） | 最终形态 | 切换点 |
| --- | --- | --- | --- | --- |
| 1 | `handleApi` 函数体 | §3.6 空壳：`/api/*` 全部落到兜底 `404 {error:'接口不存在'}` | 22 条路由 + 兜底 404 / 500 | §5.3 整体替换 |
| 2 | `send` 定义位置 | §3.6 定义在常量之后 | 挪到 `listResource` 之后（段 11） | §5.2–5.3 装配时移动（函数提升，位置不影响运行；最终以真实文件为准） |
| 3 | 三处注释占位 | §5.3 里 workbench 转发行被 `//` 注释、providers 与 `migrate` 区域留注释 | 三条真实路由 | §8.16 / §9.4–9.6 / §11.1 逐段点亮 |
| 4 | 组件区 | §6.3 12 行占位：`Placeholder` 小件 + 7 个页面占位 + 2 个浮层 `return null` | 15 个页面组件 + `DetailDrawer` + 弹窗 | §7.1–7.17 逐个替换 |
| 5 | `workbench.tsx` | §6.4 占位版：4 个导出只为「能编译」 | 35 行真实版 | §8.17–8.22 整体替换 |
| 6 | `styles.css` | §6.5 L1–11 逐字一致 + L12 空行 | 追加 L13–14（`wb-*` 家族） | §8.23 追加，不替换 |
| 7 | `main.tsx` 两处逻辑 | §6.3 的本地简化版（本地规则分析；无迁移入口） | `ai/analyze` + providers 接线；迁移弹窗闭环 | §9.7（117、139 行）/ §11.2（51、72、159 行） |

> **两个「遗留」不算差异**：`QuickTasks`（§7.2）与 `WorkflowPage`（§7.10）是真实项目里**定义但未挂载**的组件——不在 NAV、没有分发分支，永远不会渲染。这不是装配错误，而是产品演进留下的历史痕迹（第 7 章练习 5 与第 12 章的进阶设想都围绕它展开）。

> **行号漂移提醒**：各章标题里的「真实 X–Y 行」是**那一章写作时的基准**；后续章节在同一个文件里继续插代码，行号会漂移。定位代码永远靠**函数名与段序号**（C.2.1 / C.3.1），不要背行号。

---

## ⚠️ 使用要点（对照与排错）

1. **口径优先**：教学版 `server.mjs` 是**展开格式**（一条语句一行），真实版是**压缩格式**（多语句一行）——「逐字一致、换行不同」（第 0 章 0.6）。对照时核对语句集合，不要逐行比对。
2. **分段核对法**：以 C.2.1 / C.3.1 的段为单位，每段用「第一行 + 最后一行」做锚点，中间允许换行与顺序的展示差异。
3. **装一段、验一段**：每装完一块，回 C.2.3 / C.3.3 的检查点表逐项验证（`npm run check`、`curl`、界面可用性）——不要全部装完再回头排错。
4. **先查 C.5 再怀疑自己**：与真实文件「不一样」时，多数是上表的过渡形态或遗留组件造成的正常现象。

---

## ✍️ 练习

**练习 1（盲装）**：合上第 3–5 章，只用 C.2 的七个块从零拼出完整 `server.mjs`。验收：

- `curl.exe -s http://127.0.0.1:3344/api/bootstrap` 返回 `counts`（8 个资源键）、`modules`（5 条）、`providers`（4 条）；
- `curl.exe -s "http://127.0.0.1:3344/api/tasks?q=logs"` 只返回「支付回调消费超时排查」一条。
（提示：七块缺一不可——块 7 的静态服务与 `createServer` 也别漏。）

**练习 2（扩展第八个入口）**：给 `ENTRY_DEFS` 加一个 `perf_analysis`（「性能分析」，triggers 含「性能 / 耗时 / 响应慢」）。验收：

- `curl.exe -s http://127.0.0.1:3344/api/workbench/config` 的 `entries` 变成 8 项；
- AI 助手页输入含「性能」的文字，「任务路由」栏出现新入口。
- 想一想：`DEFAULT_ORDER` 需要同步改吗？`buildOutput` 不加分支会怎样？（线索：§8.1、§8.2、§8.6。）

**练习 3（装配诊断）**：一位同学的版本能启动、列表正常，但**任务详情里「现象 / 建议」永远为空**，接口返回的是 `inputJson` / `resultJson` 字段。对照 C.2.1 段地图：

- 他漏装的是哪一段的哪个细节？（提示：段 9 `mapRow` 的 Json 展开循环）
- 补上后验证：`curl.exe -s http://127.0.0.1:3344/api/tasks/task_bug_001` 返回里出现 `input` 与 `result`。

---

> ⬆ [返回总目录](./README.md) · 上一附录：[附录 B 命令速查](./附录B-命令速查.md)
