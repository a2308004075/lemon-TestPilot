# 第 5 章 · 通用 REST API 层

> ⬆ [返回总目录](./README.md) · 上一章：[第 4 章 · 数据层设计（SQLite）](./04-数据层设计.md) · 下一章：[第 6 章 · 前端骨架](./06-前端骨架.md) →

---

### 🎯 本章目标

第 3 章留好的 `handleApi` 骨架、第 4 章建好的数据库，本章把它们接上：**一个查询引擎 + 一份路由表，撑起 8 个资源的全部接口**。这就是整个项目最核心的「通用 REST API 层」。学完你将得到：

1. 配置驱动的思路落地：`resources` 表 + `listResource` 引擎，8 个资源共用一套分页/搜索/过滤逻辑；
2. 手写 `readBody()`（请求体解析）与 `audit()`（审计落库）两个工具函数；
3. `handleApi` 从 10 行骨架膨胀到完整版——约 20 条路由逐条讲解；
4. 六个「后端业务规则」的完整实现：`??` 并入式更新、自动升版、知识状态机、**409 引用保护**、回归进度服务端计算、报告来源写关系；
5. 一套 curl 全流程验证清单：聚合 → 列表 → 详情 → 创建 → 更新 → 状态机 → 审计追溯。

> 说明：本章的 `handleApi` 完整版对应真实项目第 93–120 行；其中工作流转发（第 8 章）、模型厂商与 AI 调用（第 9 章）、数据迁移（第 11 章）三处先留占位注释，章节到了再接入。

### 🚧 前置条件

- 第 4 章完成：数据库、12 张表、种子数据就绪，`npm run dev` 正常；
- 本章继续在 `server.mjs` 中插入代码：`resources`/`listResource` 插到 `mapRow` 之后，`readBody`/`audit` 插到 `send` 之后，替换掉 `handleApi` 的整个函数体。

---

### 🧠 设计思路与原理

**① REST 语义怎么落到代码里？** 本项目用三个动词表达三种意图：

| 语义 | HTTP 方法 | 本项目的形态 |
| --- | --- | --- |
| 读 | `GET` | `/api/{resource}` 列表、`/api/{resource}/:id` 详情 |
| 建 | `POST` | `POST /api/{resource}`（body 是新对象的字段） |
| 改 | `PATCH` | `PATCH /api/{resource}/:id`（body 只放**要改的字段**） |

为什么「改」用 `PATCH` 而不是 `PUT`？`PUT` 的语义是「整个对象替换」——客户端必须把十几个字段全传回来，漏一个就会把那个字段冲掉；`PATCH` 是「打补丁」——只传要改的字段，其余不动。本项目所有更新接口都是 PATCH（含状态流转，如 `{"action":"approve"}`），这让前端可以「只发差异」，也让接口天然兼容「并发时别人改过的字段」。

**② 8 个资源的 CRUD 有 90% 相同，为什么要写 8 份代码？**

对比一下 tasks 和 cases 的「列表查询」：都是「可选搜索 + 可选过滤 + 分页 + 排序 + 总数」。差异只有四个点：表名、可搜索的列、固定条件（如 cases 要排除已归档）、排序列。**描述差异、复用逻辑**——这就是配置驱动（data-driven）：

```js
const resources = {
  cases: { table:'cases', search:['code','title',...], order:'updated_at', where:'archived=0' },
  ...
};
```

一个 30 行的 `listResource()` 读着这份配置，服务所有资源。将来加一个新资源（比如「测试计划」）只需：建表 + 在 `resources` 里加一行。框架（第 1 章说过的 Express 等）用插件体系解决这个问题；我们用一张表加一个函数。

**③ 手写 SQL 怎么防注入？** 本章所有 SQL 的「变量位置」都写作 `?`（参数占位符），值通过 `run(...)` / `.all(...)` 单独传入——**SQL 文本与数据走两条完全不同的通道**，无论用户输入什么（引号、分号、`OR 1=1`），都只会被当作**数据**处理。唯一能拼进 SQL 文本的是**代码里写死的配置**（表名、列名、排序方向），永远不来自用户输入——这一点本章的 `listResource` 和练习 2 会反复印证。

---

## 5.1 资源表与查询引擎（resources + listResource）

在 `mapRow` 函数之后插入以下代码（真实项目第 86–87 行，格式化后）：

```js
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
```

### 资源表逐字段读

| 字段 | 作用 | 例子 |
| --- | --- | --- |
| `table` | 对应的数据库表名 | `cases` → `cases` 表 |
| `search` | `q` 关键字要模糊匹配的列（`LIKE`） | 搜「支付」会同时匹配用例的 code/title/priority/status |
| `order` | 列表默认按哪列排序（DESC 倒序） | 业务表都按 `updated_at`（最近动过的在前）；modules 按 `sort_order`；providers 按 `name` |
| `where` | 拼进每条查询的固定条件（可选） | cases 排除软删：`archived=0` |

### 查询引擎逐段读（这是全项目复用度最高的函数）

**① 分页参数的安全化**：

```js
const page = Math.max(1, Number(url.searchParams.get('page') || 1));
const pageSize = [20, 50, 100].includes(Number(url.searchParams.get('pageSize'))) ? ... : 20;
```

- `page` 最小值钳到 1—— 负数、0 都会被归为 1；非数字（如 `page=abc`）经 `Number()` 是 `NaN`，真实项目没有再兜这一层（前端永远发数字，本地工具「够用就好」）。这是全章唯一一处「不完美的边界」，知道即可；
- `pageSize` 只认 **20 / 50 / 100 白名单**——传 `pageSize=5000` 会被**回落成 20**。为什么用白名单而不是 `Math.min(100, ...)`：既防住「一次拉全库」，也让前端的选择项固定（第 7 章分页组件只有三个选项）。

**② 动态 WHERE 的组装法**——数组收集 + `join`：

```js
const clauses = c.where ? [c.where] : [];   // 固定条件先入列
const params = [];                          // 与 clauses 里的 ? 同步收集的参数
if (q) {
  clauses.push(`(${c.search.map(k => `${k} LIKE ?`).join(' OR ')})`);
  params.push(...c.search.map(() => `%${q}%`));
}
```

- `clauses`（条件）与 `params`（参数）**成对增长**——最后 `clauses.join(' AND ')` 拼出 WHERE，参数按同样顺序展开。顺序一致是关键；
- 搜索 SQL 形如：`(title LIKE ? OR type LIKE ? OR status LIKE ? OR review_status LIKE ?)`，每个 `?` 绑一个 `%支付%`——搜「支付」= 任一列包含「支付」；
- 过滤是以**白名单键名**循环收的：`['type','status','module_id','category','priority','risk']`——注意参数名用的是**表列名风格**（`module_id` 而不是 `moduleId`），前端拼 URL 时直接写 `?module_id=mod_pay_order`。这是真实项目的选择（与 camelCase 的输出形成一点小不统一，属于可改进点）。

**③ 两次查询：先数总数、再取当页**：

```js
const total = db.prepare(`SELECT COUNT(*) AS n FROM ${c.table} ${where}`).get(...params).n;
const rows = db.prepare(`SELECT * FROM ${c.table} ${where} ORDER BY ${c.order} DESC LIMIT ? OFFSET ?`).all(...params, pageSize, (page - 1) * pageSize);
```

- 两条 SQL 共用同一个 `where` 与参数，只有第二条多 `LIMIT ? OFFSET ?`（分页三要素：每页多少、跳过多少——`(page-1) * pageSize`）；
- `ORDER BY ${c.order} DESC` 的列名来自**代码内配置**（不是用户输入），这是唯一允许拼接的位置；用户能影响的一切都走 `?`；
- 返回值固定五件套：`{ items, page, pageSize, total, pages }`。`pages = Math.max(1, Math.ceil(total/pageSize))`——空表也至少报 1 页，前端分页器不用处理 0 的特例；
- `rows.map(row => mapRow(resource, row))`——第 4 章架好的「翻译桥」在这里正式上岗：列表里的每一行，出 API 时已经是 `camelCase + moduleLabel + 对象化 JSON` 的形态。

## 5.2 两个小工具：readBody 与 audit

**① `readBody()`——把请求体读出来并解析成对象**（真实项目第 89 行）：

```js
async function readBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  return chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {};
}
```

- `req` 是流式对象，body 不会一次性出现——`for await...of` 依次读光所有数据块（chunk 是 Buffer）；
- `Buffer.concat(chunks).toString('utf8')` 把数据块拼成完整的 UTF-8 字符串；
- `JSON.parse` 解析成对象；**空 body 返回 `{}`**——让后面的 `d.title ?? c.title` 逻辑自然兼容「什么都没传」；
- 非 JSON 的脏 body 会在这里抛异常 → 被 `handleApi` 的 `catch` 兜住返回 500。**理想做法**是判 Content-Type 返回 400（「请求格式错误」），真实项目选择了最简形态（本地工具，前端永远发 JSON）——知道差距在哪即可；
- 注意它是 `async` 的：调用处一律 `await readBody(req)`。

**② `audit()`——每个写操作留一条审计**（真实项目第 90 行）：

```js
function audit(type, entityId, action, detail = '') {
  db.prepare('INSERT INTO audit(entity_type,entity_id,action,detail,created_at) VALUES(?,?,?,?,?)')
    .run(type, entityId, action, detail, now());
}
```

- 参数即审计四要素：实体类型（`task`/`case`/`knowledge`…）、实体 id、动作（创建/更新/审核通过…）、细节；
- `audit` 表**只增不改**——第 6 章的详情抽屉会以时间线展示它（「这条记录一路发生过什么」）；
- 本章后面每个写路由的最后一步都是 `audit(...)`——**写数据必留痕**是贯穿全项目的纪律。

## 5.3 handleApi 完整版：全量展开

把第 3 章的 `handleApi` 骨架整个替换为下面的完整版（对应真实项目第 93–120 行；三处占位注释在第 8、9、11 章逐段点亮）：

```js
async function handleApi(req, res, url) {
  const parts = url.pathname.split('/').filter(Boolean);
  if (parts[0] !== 'api') return false;
  try {
    // if (parts[1] === 'workbench') return await workbench.handle(req, res, url, parts);  // 第 8 章接入

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

    // —— 配置与审计（providers 的 PATCH / test 与 ai 接口在第 9 章，migrate 在第 11 章）——
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

> 结构总览：函数体是「顺序匹配」的平铺 `if`——每行一条路由，命中即 `return`，最后一行兜底 404。真实项目约 20 条路由全部如此，没有任何隐藏逻辑。下面按业务分组深挖。

## 5.4 逐条路由深挖

### ① bootstrap：首屏聚合接口

```js
if (req.method === 'GET' && parts[1] === 'bootstrap') { ... }
```

一个请求返回前端首屏需要的全部「家底」：`counts`（8 个资源的记录数，循环 `resources` 配置生成）、`modules`（全部模块）、`providers`（全部模型配置）。为什么不做成三个接口、或懒加载？**因为这是个人工具**：模块几十条、providers 四条、counts 就是 8 个数字——一次拉全让前端开屏逻辑变成「一个请求 + 一个状态」，第 6 章你会看到 `bootstrap` 在前端是全局唯一的「数据底座」。

### ② 通用列表 / 详情（2 条路由服务 8 个资源）

```js
if (req.method === 'GET' && resources[parts[1]] && !parts[2]) return send(res, 200, listResource(parts[1], url));
if (req.method === 'GET' && resources[parts[1]] && parts[2]) { ...查单条... }
```

- **判断链就是路由**：`resources[parts[1]]` 存在（是 8 个资源之一），且无 `parts[2]` → 列表；有 `parts[2]`（id）→ 详情。**两条路由覆盖 16 个端点**，这就是第 1 章「不用路由框架」的底气；
- 详情的特化：`knowledge` 的详情会附带 `dependencies`（哪些实体的关联指向它，`active=1`）与 `referenceCount`——两个字段就是「引用保护」的展示面：第 7 章的详情抽屉据此显示「被 N 处引用，不可撤销」；
- 查不到统一 `404 { error: '记录不存在' }`。

### ③ 任务的创建与更新（回到第 0 章的压缩行）

**创建**（就是第 0 章拆解过的真实项目第 100 行）：

```js
db.prepare('INSERT INTO tasks VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)').run(
  taskId, d.type, d.title, d.moduleId || null, d.risk || 'P1', d.status || '草稿',
  '未提交', '未入库', d.parentId || null, json(d.input), json(d.result), d.model || '本地规则', ts, ts
);
```

- 14 个 `?` 对 14 列：id 由 `makeId('task')` 生成；`review_status` / `knowledge_status` **写死初值**（`未提交` / `未入库`）——客户端无权决定复核状态，这是**状态机纪律**；
- `d.risk || 'P1'` 之类的 `||` 兜底默认值；`json(d.input)` 把对象序列化进 JSON 列；
- 之后立刻 `audit('task', taskId, '创建', d.type)`——审计的「细节」存任务类型；
- 最后**回读**一次再返回（`SELECT * ... WHERE id=?` 后 `mapRow`）：保证返回值带上数据库补齐的所有默认值与时间戳，`201`（Created）状态码表示「已创建」。

**更新**——本项目所有 PATCH 的通用范式，值得单独命名：**「先查、再并、后写」**：

```js
const c = db.prepare('SELECT * FROM tasks WHERE id=?').get(parts[2]);   // 1. 先查当前行
if (!c) return send(res, 404, { error: '任务不存在' });
db.prepare('UPDATE tasks SET ...').run(d.title ?? c.title, ...);        // 2. 逐字段并入
```

逐字段并入的表达式 `d.title ?? c.title`：**请求里传了就用新的，没传保持旧值**。为什么用 `??`（空值合并）而不是 `||`？`||` 会把**合法但为假**的值也回退掉——比如 `d.status = ''`（不是有效状态，但假设你想存空）或数字 `0`、布尔 `false`。`??` 只对 `null` / `undefined` 回退，语义精确。**这是 PATCH 部分更新语义在代码层的实现**，六处 PATCH 全部用它。

JSON 列怎么并入？`json(d.input ?? parse(c.input_json))`——传了新对象就整体替换，没传就把原 JSON 解析回来再写回，**保持原样**。注意 `parse` 是为了避免把 JSON 字符串再 `stringify` 一次变成「字符串的 JSON」。

`audit(..., d.action || '编辑任务')`：前端做状态流转时会带 `action`（如 `提交审核`），普通编辑则不带——审计细节就把「谁改的？什么动作」记全了。

### ④ 用例：自动编号、自动升版、软删除

**创建**：`code = d.code || 'TC-' + 时间戳后 6 位`（没给编号就自动生成）；`version` 固定从 1 开始；`archived` 固定 0。

**更新**——两处与任务不同：

```js
db.prepare('UPDATE cases SET ..., version=version+1, ..., archived=?, updated_at=? WHERE id=?')
```

- **`version=version+1` 写在 SQL 里**（不是先读出来加一再写回）——自增由数据库自己完成，简单、准确。真实项目的业务规则：**用例每次编辑都升一版**，配合 `updated_at` 构成「用例演化史」；
- `d.archived ?? c.archived`：**软删除**——把 `archived` 置 1 而不是物理删除。列表查询的 `where:'archived=0'` 会让它从列表消失，但数据、审计、引用关系都保留。这就是本项目没有 DELETE 接口的原因：**可追溯优先**（练习 1 亲手体验）。

### ⑤ 知识：动作驱动的状态机 + 409 引用保护

**创建**固定进「待审核」，防客户端「自己给自己发证」。

**更新**是本章业务逻辑最重的一条：

```js
if (d.action === 'withdraw') {
  const deps = db.prepare("SELECT * FROM relations WHERE target_type='knowledge' AND target_id=? AND active=1").all(parts[2]);
  if (deps.length) return send(res, 409, { error: '存在有效关联，不能撤销入库', dependencies: deps });
}
const status = d.action === 'approve' ? '已发布' : d.action === 'reject' ? '已驳回' : d.action === 'withdraw' ? '已撤销' : d.status ?? c.status;
```

- **动作 → 状态映射**：`approve` → 已发布、`reject` → 已驳回、`withdraw` → 已撤销、其他 → 保留原状态（配合 `d.status` 允许直接改）。状态只能经由「动作」流转，不是随便写个字符串；
- **409 引用保护**是本章最重要的业务规则：撤销（withdraw）前先查 `relations` 中「谁引用了这条知识且仍然有效（`active=1`）」。有引用 → 拒绝，返回 `409`（**Conflict**，语义正是「请求与当前资源状态冲突」）+ 引用清单。为什么保护？知识被任务/报告引用后撤销，会让引用**悬空**——宁可让用户先处理引用，也不产生「指向不存在知识」的烂账。第 9 章的 AI 任务与第 8 章的工作流都会往这里写引用；
- 升版同用例（`version=version+1`）；`audit` 的细节存 `reviewNote`（审核意见进审计）。

### ⑥ 回归：进度由服务端计算

```js
const complete = items.filter(i => !['未执行','阻塞'].includes(i.status)).length;
const progress = items.length ? Math.round(complete / items.length * 100) : 0;
... .run(d.status ?? (progress === 100 ? '已完成' : '执行中'), progress, ...)
```

- **进度公式**：非「未执行/阻塞」的条目算完成——**「失败」也算完成**（执行过了）；`2/3 → Math.round(66.67) = 67`，和第 4 章种子数据里的 `progress: 67` 严丝合缝；
- 进度**永远由服务端计算**：客户端传什么都不信（否则「把进度报成 100」就成了前端说了算）；
- 条目全完成（`progress === 100`）时状态**自动**变「已完成」；否则默认回到「执行中」（显式传 `d.status` 可覆盖）；
- `value = d.data ?? parse(c.data_json)`：整个 `items` 数组整体替换（这种「列表里的列表」用整体替换比逐项差量简单得多）。

### ⑦ 报告 / 工作流 / 模块 / 配置

- **报告（POST）**：创建后有一行容易被忽略的关键逻辑——`(d.data?.sources || []).forEach(...)` 把来源**写成 relations 关联**（`report → 各类资产`，label 为「报告来源」）。于是「这份报告引用了哪些回归/任务」不仅能显示，还纳入了统一的引用网络（第 9 章「知识被 N 处引用」会连报告一起算）。**注意**：`sources` 的元素必须是 `{ type, id }` 对象（种子数据里的字符串只是展示值，创建接口不消费——这是真实项目里一个小不一致，照实说明）；
- **工作流（POST）**：最小写入模板——`steps` 序列进 JSON 列；
- **模块（POST/PATCH）**：`sortOrder` 默认 99（新建的排最后）；三级名称与状态可改；
- **settings（GET/PATCH）**：PATCH 用了 SQLite 的 **upsert**：`INSERT ... ON CONFLICT(key) DO UPDATE SET value_json=excluded.value_json`——「有则更新、无则插入」一句 SQL 完成，不用先查再分支。`excluded` 指「本次本想插入的那行」；
- **audit（GET）**：按 `entity_type + entity_id` 查某实体的历史，`ORDER BY id DESC`（最新在前），`mapRow('audit')` 顺手转 camelCase。

> 尚未接入的三处（已留注释）：`workbench` 转发（第 8 章）、providers 的 PATCH/test 与 `ai/analyze`（第 9 章）、`migrate`（第 11 章）。它们都遵循同样的「一行一路由」模式。

## 5.5 里程碑验证（curl 全流程）

保持 `npm run dev` 运行，另开终端。**Windows 提示**：以下都用 `curl.exe`；PowerShell 5.1 传 JSON 时**引号是老大难**（见「要点与坑」第 4 条），本清单的 JSON 均不含空格；更省心的替代方案见验证 D 的 `Invoke-RestMethod` 版。

**验证 A：bootstrap 聚合**

```powershell
curl.exe -s http://127.0.0.1:3344/api/bootstrap
```

预期（节选，注意 `counts` 里 8 个资源的数字和第 4 章数的一致）：

```json
{"counts":{"tasks":4,"cases":4,"knowledge":4,"regressions":1,"reports":1,"workflows":1,"modules":5,"providers":4},
 "modules":[{"id":"mod_pay_callback","projectName":"电商平台","moduleName":"支付中心","submoduleName":"支付回调",...}],
 "providers":[{"id":"custom","name":"自定义兼容接口","keyConfigured":false,...}]}
```

一眼验证两件事：字段已是 `camelCase`（`projectName`），providers 带 `keyConfigured` 派生字段——`mapRow` 在工作。

**验证 B：列表引擎三连**

```powershell
curl.exe -s "http://127.0.0.1:3344/api/tasks?q=支付&type=bug"
curl.exe -s "http://127.0.0.1:3344/api/cases?status=通过"
curl.exe -s "http://127.0.0.1:3344/api/tasks?pageSize=500"
```

预期：

1. 第一条 `total: 1`（标题含「支付」且类型是 bug 的任务——`q` 与过滤条件**叠加**生效）；
2. 第二条 `total: 1`（只有 `case_001` 状态是通过）；还带 `items[0].moduleLabel`（「电商平台 / 支付中心 / 支付回调」）；
3. 第三条 `pageSize: 20`——**500 不在白名单，被回落**。

**验证 C：详情与知识依赖**

```powershell
curl.exe -s http://127.0.0.1:3344/api/knowledge/kb_004
curl.exe -s http://127.0.0.1:3344/api/tasks/不存在
```

预期：

1. 第一条返回 kb_004 全字段 + `"referenceCount":1` + `dependencies` 含一条 `sourceType:"task"`、`sourceId:"task_bug_001"`（第 4 章种下的那条关联）；
2. 第二条 `{"error":"记录不存在"}`（404）。

**验证 D：创建任务（POST）**

```powershell
curl.exe -s -X POST http://127.0.0.1:3344/api/tasks -H "Content-Type: application/json" -d '{"type":"bug","title":"接口验证任务","moduleId":"mod_pay_callback","input":{"phenomenon":"演示"}}'
```

预期：`201` + 新任务对象——id 形如 `task_ab12cd34`，且**没传的字段已按默认值补齐**：`"risk":"P1","status":"草稿","reviewStatus":"未提交","knowledgeStatus":"未入库","model":"本地规则"`。

若上面的命令在你的 PowerShell 里因引号报错，改用原生 cmdlet（对引号更宽容）：

```powershell
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:3344/api/tasks -ContentType 'application/json' -Body '{"type":"bug","title":"接口验证任务","moduleId":"mod_pay_callback","input":{"phenomenon":"演示"}}'
```

**验证 E：PATCH 并入 + 审计追溯**

把刚才创建的 task id（下文用 `<task_id>` 指代）拿来用：

```powershell
curl.exe -s -X PATCH http://127.0.0.1:3344/api/tasks/<task_id> -H "Content-Type: application/json" -d '{"action":"提交审核","status":"待审核"}'
curl.exe -s "http://127.0.0.1:3344/api/audit?type=task&id=<task_id>"
```

预期：

1. 第一条返回的任务：`status` 变成 `待审核`、`updatedAt` 更新，而 `title`、`risk` 等**没传的字段原样保留**（并入式更新）；
2. 第二条返回两条审计（`创建` 与 `更新`），最新在前——这就是第 6 章详情抽屉的「时间线」数据源。

**验证 F：知识状态机与 409 保护**

```powershell
curl.exe -s -X PATCH http://127.0.0.1:3344/api/knowledge/kb_003 -H "Content-Type: application/json" -d '{"action":"approve","reviewNote":"验证通过"}'
curl.exe -s -X PATCH http://127.0.0.1:3344/api/knowledge/kb_004 -H "Content-Type: application/json" -d '{"action":"withdraw"}'
```

预期：

1. 第一条：kb_003 从「待审核」变「已发布」，`version` 1→2；
2. 第二条：`409` + `{"error":"存在有效关联，不能撤销入库","dependencies":[...]}`——kb_004 正被 `task_bug_001` 引用，撤销被拦下。**这就是引用保护在工作**。

---

### 🔍 对照真实项目

| 本章内容 | 真实文件与行号 | 说明 |
| --- | --- | --- |
| 5.1 资源表 | `server.mjs` 第 86 行 | 8 个资源的配置逐字一致（仅换行） |
| 5.1 查询引擎 | `server.mjs` 第 87 行 | 分页白名单、LIKE 搜索、等值过滤、两次查询逐字一致 |
| 5.2 | `server.mjs` 第 89–90 行 | `readBody` / `audit` 逐字一致 |
| 5.3 聚合 | `server.mjs` 第 97 行 | bootstrap 逐字一致 |
| 5.3 通用读 | `server.mjs` 第 98–99 行 | 列表/详情（含 knowledge 特化）逐字一致 |
| 5.3 任务 | `server.mjs` 第 100–101 行 | 创建/更新逐字一致（第 0 章拆解过第 100 行） |
| 5.3 用例 | `server.mjs` 第 102–103 行 | 逐字一致 |
| 5.3 知识 | `server.mjs` 第 104–105 行 | 含 409 引用保护，逐字一致 |
| 5.3 回归 | `server.mjs` 第 106 行 | 进度计算逐字一致 |
| 5.3 报告/工作流/模块 | `server.mjs` 第 107–110 行 | 逐字一致 |
| 5.3 配置/审计 | `server.mjs` 第 115–117 行 | 逐字一致 |
| 本章未涉及 | `server.mjs` 第 96、111–114 行 | 工作流转发（第 8 章）、providers 与 AI（第 9 章）、migrate（第 11 章） |
| 本章未涉及 | `server.mjs` 第 91 行 | `providerKeys(dataDir)` 工厂——第 9 章 |

### ⚠️ 要点与坑

1. **PATCH 用 `??` 不用 `||`**——`||` 会把 `0`、`''`、`false` 这些**合法值**也当作「没传」回退掉；`??` 只认 `null`/`undefined`。六个 PATCH 路由全靠这个语义成立；
2. **路由顺序 = 匹配顺序**——某条路由只要被前面的 `if` 命中就 `return`，后面的不再执行。新增路由时想清楚它在链中的位置（尤其新资源的判断不要放在兜底 404 之后）；
3. **分页边界行为**：`pageSize` 非白名单值**回落 20**（不报错）；`page` 越界返回**空 items**（`total`/`pages` 照常给出）；`page=abc` 在真实实现里会得到 `NaN` 导致空结果——本地工具「够用就好」的边界，知道即可；
4. **PowerShell 5.1 传 JSON 的引号坑**——`curl.exe -d '...'` 在参数含**空格**时会丢引号；本文 JSON 均无空格是刻意的。含空格/复杂 JSON 请改用 `Invoke-RestMethod` 或 `-d '@body.json'`（文件）方式；
5. **SQL 拼接的唯一合法来源是代码内配置**——`table`/`order`/`search` 列名来自 `resources` 常量；任何用户输入（`q`、过滤值、body）必须走 `?`。练习 2 会演示注入被挡住的效果——**不要试图在 `ORDER BY` 等位置用参数占位符**（SQLite 语法不支持），那些位置只能用白名单校验后的枚举值;
6. **LIKE 的通配符**：用户在 `q` 里输入 `%` 或 `_` 会被当作通配符（真实项目未转义）——影响仅是搜索范围略大；要精确匹配需 `ESCAPE` 子句（扩展练习）；
7. **过滤参数名是表列名风格**（`module_id`、`case_type` 之类 camelCase 参数**不生效**）——真实项目如此；前端拼 URL 时按本章验证 B 的写法来；
8. **没有 DELETE 的设计取舍**：删除=归档（cases 的 `archived`）、审计只增——换来完整可追溯；代价是数据只涨不消，清理请直接删 `data/` 重建。

### ✍️ 练习

1. **用软删除「删掉」一条用例**：`curl.exe -s -X PATCH http://127.0.0.1:3344/api/cases/case_004 -H "Content-Type: application/json" -d '{"archived":1}'`，然后 `curl.exe -s "http://127.0.0.1:3344/api/cases"` 确认它从列表消失；再 `curl.exe -s http://127.0.0.1:3344/api/cases/case_004` 确认**详情仍能查到**。想一想：为什么「详情可见、列表不可见」是软删除的典型形态？
2. **亲手验证注入被挡住**：`curl.exe -sG http://127.0.0.1:3344/api/tasks --data-urlencode "q=' OR 1=1 --"`。预期 `total: 0`（整个输入被当作**字面字符串**去 LIKE 匹配，而不是拼进 SQL）。再想一层：如果把实现改成字符串拼接 `title LIKE '%${q}%'`，这个输入会发生什么？
3. **把回归进度打到 100**：先用 `curl.exe -s http://127.0.0.1:3344/api/regressions/reg_001` 拿到完整 `data.items`（3 条），把 `ri_3` 的 `status` 从「未执行」改成「通过」，用 PATCH（body 为 `{"data":{"version":"v2.8.3","items":[...改后的三条...]}}`）提交。预期：`progress: 100`、`status: "已完成"`——体验「进度服务端计算 + 状态自动流转」；
4. **边界实验**：分别请求 `?page=999`（预期空 items）、`?pageSize=100`（预期 `pageSize: 100`）、`?pageSize=30`（预期回落 `20`）、`?q=`（预期等价于不传）——把「回落/钳制/忽略」三类边界都摸一遍。

---

**下一章**：[第 6 章 · 前端骨架](./06-前端骨架.md) →

> ⬆ [返回总目录](./README.md)
