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

function normalizeAsset(row){const x=camel(row);x.triggers=parse(x.triggersJson,[]);x.required=parse(x.requiredJson,[]);x.knowledge=parse(x.knowledgeJson,[]);x.workflow=parse(x.workflowJson,[]);delete x.triggersJson;delete x.requiredJson;delete x.knowledgeJson;delete x.workflowJson;delete x.seq;return x}
function normalizeRun(row){const x=camel(row);for(const k of ['routesJson','inputJson','resultJson','missingJson','reviewJson','evidenceJson']){x[k.replace('Json','')]=parse(x[k],k==='routesJson'||k==='missingJson'||k==='evidenceJson'?[]:{});delete x[k]}if(Array.isArray(x.review?.checks)&&x.review.checks.length&&typeof x.review.checks[0]==='string')x.review.checks=x.review.checks.map(item=>({item,checked:false}));return x}
function fieldPresent(text,field){const rules={
  '问题标题':/.{5,}/,'复现步骤':/复现|步骤|操作|点击|请求/,'实际结果':/实际|现象|仍为|报错|失败/,'预期结果':/预期|应该|应为/,'环境':/环境|staging|test|prod|生产|测试环境/,
  '日志或 traceId':/日志|trace\s?id|exception|error|timeout|堆栈/i,'时间范围':/\d{1,2}:\d{2}|\d{4}-\d{1,2}-\d{1,2}|时间/,'运行环境':/环境|staging|test|prod|生产/,
  'SQL 文本':/select |update |insert |delete |join |explain /i,'分析目标':/分析|排查|优化|慢|风险|一致性/,
  '需求背景':/需求|背景|功能|规则/,'验收标准':/验收|预期|应当|必须/,'版本或变更内容':/版本|v\d|变更|改动/,'影响模块':/模块|支付|订单|账户|接口/,
  '版本':/版本|v\d/,'测试范围':/范围|模块|用例|回归/,'执行结果':/通过|失败|阻塞|执行/,'Prompt 文本':/prompt|提示词/i,'预期输出或评价标准':/预期|标准|schema|格式/i
};return rules[field]?.test(text)||false}
function routeText(text,selected=[]){const lower=text.toLowerCase();let ids=ENTRY_DEFS.filter(e=>e.triggers.some(t=>lower.includes(t.toLowerCase()))).map(e=>e.id);selected.forEach(id=>{if(!ids.includes(id))ids.push(id)});if(!ids.length)ids=['bug_analysis'];ids.sort((a,b)=>DEFAULT_ORDER.indexOf(a)-DEFAULT_ORDER.indexOf(b));return ids}
const OUTPUT_SCHEMAS={
  bug_analysis:{phenomenon:{type:'string',label:'问题现象概述'},affected_modules:{type:'string[]',label:'影响模块'},possible_causes:{type:'string[]',label:'可能原因（标注证据支撑）'},recommended_logs:{type:'string[]',label:'建议排查的日志方向'},recommended_sql:{type:'string[]',label:'建议核查的 SQL 方向'},related_history_bugs:{type:'string[]',label:'仅可引用本次提供的知识条目，无则返回空数组，禁止编造 Bug 编号'},regression_scope:{type:'string[]',label:'建议回归范围'}},
  log_triage:{anomaly_summary:{type:'string',label:'异常摘要'},timeline:{type:'string[]',label:'关键时间线'},suspect_modules:{type:'string[]',label:'可疑模块'},suggestions:{type:'string[]',label:'排查建议'}},
  sql_analysis:{risks:{type:'string[]',label:'风险点'},where_review:{type:'string',label:'过滤条件审查结论'},index_advice:{type:'string',label:'索引建议'},explain_suggestions:{type:'string[]',label:'EXPLAIN 验证建议'}},
  regression_list:{must_regression:{type:'string[]',label:'必回归项'},suggested_regression:{type:'string[]',label:'建议回归项'},completion_rule:{type:'string',label:'完成规则'}},
  testcase_gen:{coverage:{type:'string[]',label:'覆盖维度'},case_format:{type:'string[]',label:'用例结构'},suggestions:{type:'string[]',label:'生成建议'}},
  test_report:{sections:{type:'string[]',label:'报告章节'},release_decision:{type:'string',label:'上线建议（注明需人工审批）'}},
  prompt_test:{checks:{type:'string[]',label:'检查项'},decision:{type:'string',label:'判定建议'}}
};
function validateOutput(routeId,parsed){const schema=OUTPUT_SCHEMAS[routeId]||{},missing=[],typeErrors=[];for(const [k,v] of Object.entries(schema)){const val=parsed?.[k];if(val===undefined||val===null||val===''){missing.push(v.label);continue}if(v.type==='string'&&typeof val!=='string')typeErrors.push(`${v.label}应为字符串`);if(v.type==='string[]'&&(!Array.isArray(val)||val.some(x=>typeof x!=='string')))typeErrors.push(`${v.label}应为字符串数组`)}return{passed:!missing.length&&!typeErrors.length,missing,typeErrors}}
function systemPrompt(entry,role,moduleLabel,hits){
  const facts=hits.length?hits.map(h=>`- [${h.category}] ${h.title}（v${h.version}，风险 ${h.risk}）：${h.snippet}`).join('\n'):'（本次未检索到相关知识：相关字段请如实返回"未检索到相关知识"或空数组，禁止虚构知识标题或 Bug 编号。）';
  const schema=Object.entries(OUTPUT_SCHEMAS[entry.id]||{}).map(([k,v])=>`  "${k}": ${v.label}（${v.type==='string[]'?'字符串数组':'字符串'}）`).join(',\n');
  return `你是 TestPilot 测试工作台的分析引擎，正在执行「${entry.name}」任务（角色：${role}。同一份材料命中了多个任务，你只负责本任务，不要复述其他任务的内容）。\n所属模块：${moduleLabel}\n工作流步骤（严格执行，不跳步）：${entry.workflow.join(' → ')}\n知识库事实（仅以下已发布知识可作为事实依据）：\n${facts}\n安全规则：证据不足时明确说明缺什么，不猜测根因；不编造知识标题或 Bug 编号；AI 不做上线批准；P0/P1 结论必须交由人工复核。\n只返回一个合法 JSON 对象（禁止 markdown 代码块和任何多余文字），字段结构：\n{\n${schema}\n}`;
}
async function callModelRaw(provider,key,messages){
  const budget=Math.min(Math.max(provider.timeout,5),30)*1000;
  const response=await fetch(`${provider.base_url.replace(/\/$/,'')}/chat/completions`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${key}`},body:JSON.stringify({model:provider.model,temperature:provider.temperature,max_tokens:provider.max_tokens,messages}),signal:AbortSignal.timeout(budget)});
  if(!response.ok)throw new Error(`HTTP ${response.status}`);
  return (await response.json()).choices?.[0]?.message?.content||'';
}
async function callModel(provider,key,messages){
  const content=await callModelRaw(provider,key,messages);
  const parsed=parse(content.replace(/```json\s*|\s*```/g,'').trim(),null);
  if(!parsed||typeof parsed!=='object'||Array.isArray(parsed))throw new Error('模型未返回合法 JSON 对象');
  return parsed;
}
const PROMPT_TEST_MAX_SAMPLES=10;
const PROMPT_TEST_MODES=['exact','contains','json_fields','regex','manual'];
function normalizeSample(s,idx){return{id:String(s?.id||`样例${idx+1}`).trim()||`样例${idx+1}`,input:String(s?.input||'').trim(),expectMode:PROMPT_TEST_MODES.includes(s?.expectMode)?s.expectMode:'manual',expected:String(s?.expected??'').trim(),note:String(s?.note||'').trim()}}
function parsePromptTestMaterial(text){
  const src=String(text||'');
  const promptMatch=src.match(/【\s*被测\s*Prompt[^】]*】\s*([\s\S]*?)(?=\n\s*【|$)/);
  const promptText=promptMatch?promptMatch[1].trim():'';
  if(!promptText)return null;
  const schemaMatch=src.match(/【\s*预期输出\s*Schema[^】]*】\s*([\s\S]*?)(?=\n\s*【|$)/);
  const samples=[];const re=/【\s*样例[^】]*】\s*([\s\S]*?)(?=\n?\s*【\s*样例|$)/g;let m;
  while((m=re.exec(src))){const seg=m[1];const input=((seg.match(/输入[：:]?\s*([\s\S]*?)(?=[\r\n]\s*(?:模式|预期)[：:]|[\r\n]\s*【|$)/)||[])[1]||'').trim();if(!input)continue;const mode=((seg.match(/模式[：:]\s*(exact|contains|json_fields|regex|manual)\b/i)||[])[1]||'manual').toLowerCase();const expected=((seg.match(/预期[：:]?\s*([\s\S]*?)(?=[\r\n]\s*模式[：:]|[\r\n]\s*【|$)/)||[])[1]||'').trim();samples.push(normalizeSample({id:`样例${samples.length+1}`,input,expectMode:mode,expected},samples.length))}
  return samples.length?{promptText,schemaText:schemaMatch?schemaMatch[1].trim():'',samples:samples.slice(0,PROMPT_TEST_MAX_SAMPLES)}:null;
}
function judgeSample(mode,expected,actual){
  const text=String(actual??''),want=String(expected??'');
  try{
    if(mode==='exact')return text.trim()===want.trim()?'通过':'失败';
    if(mode==='contains'){const list=want.split(/[;；|\n]/).map(s=>s.trim()).filter(Boolean);return list.length&&list.every(k=>text.includes(k))?'通过':'失败'}
    if(mode==='json_fields'){const fields=want.split(/[,，;；\n]/).map(s=>s.trim()).filter(Boolean);let obj;try{obj=JSON.parse(text.replace(/```json\s*|\s*```/g,'').trim())}catch{return '失败'}return obj&&typeof obj==='object'&&!Array.isArray(obj)&&fields.every(f=>f in obj)?'通过':'失败'}
    if(mode==='regex')return new RegExp(want).test(text)?'通过':'失败';
  }catch{return '待人工'}
  return '待人工';
}
async function runPromptTest(provider,key,t00,source){
  const rows=[],steps=[];
  for(const s of source.samples){
    if(Date.now()-t00>=180000){rows.push({sample_id:s.id,input:s.input,expected:s.expected||'—',actual_output:'',verdict:'未运行',diff_notes:'总预算（180 秒）耗尽，未运行'});steps.push({name:`${s.id} · 未运行`,verdict:'未运行'});continue}
    try{
      const actual=await callModelRaw(provider,key,[{role:'user',content:`${source.promptText}\n\n---\n\n${s.input}`}]);
      const verdict=judgeSample(s.expectMode,s.expected,actual);
      rows.push({sample_id:s.id,input:s.input,expected:s.expected||'（人工判定）',actual_output:actual,verdict,diff_notes:verdict==='失败'?`判定规则 ${s.expectMode} 未满足，预期：${s.expected||'—'}`:verdict==='待人工'?'判定模式为人工，结论以复核为准':''});
      steps.push({name:`${s.id} · ${verdict}`,verdict});
    }catch(err){
      rows.push({sample_id:s.id,input:s.input,expected:s.expected||'—',actual_output:'',verdict:'未运行',diff_notes:`模型调用失败：${err.message}`});
      steps.push({name:`${s.id} · 未运行`,verdict:'未运行'});
    }
  }
  if(!rows.some(r=>r.verdict!=='未运行'))throw new Error(`全部样例均未运行：${rows[0]?.diff_notes||'未知原因'}`);
  const ran=rows.filter(r=>r.verdict!=='未运行'),passCount=ran.filter(r=>r.verdict==='通过').length;
  let checks=[],decision='';
  if(Date.now()-t00<180000){
    try{
      const digest=rows.map(r=>`${r.sample_id}［${r.verdict}］预期：${String(r.expected).slice(0,100)}｜实际输出摘要：${String(r.actual_output||r.diff_notes).slice(0,200)}`).join('\n');
      const parsed=await callModel(provider,key,[{role:'system',content:'你是 TestPilot 的 Prompt 测试评审引擎。下面是对一个被测 Prompt 逐样例真运行后的机器判定结果，verdict 由规则判定产生，不可更改。请仅基于这些事实输出：checks 为检查项字符串数组（逐样例结果之外的整体观察，重点检查事实与幻觉：输出是否只基于被测 Prompt 与样例输入提供的信息、有无资料外断言或编造）；decision 为一句话判定建议，结尾必须注明「需人工终审」。只返回一个合法 JSON 对象（禁止 markdown 代码块），结构：\n{\n  "checks": ["检查项（字符串数组）"],\n  "decision": "判定建议（字符串）"\n}'},{role:'user',content:`预期输出 Schema：${source.schemaText||'未提供'}\n\n逐样例运行结果：\n${digest}`}]);
      checks=Array.isArray(parsed.checks)?parsed.checks.map(String):[];
      decision=String(parsed.decision||'').trim();
    }catch{/* 评审总结降级：走本地兜底文案，样例结果照常返回 */}
  }
  if(!checks.length)checks=['本地兜底：评审总结未能生成（预算耗尽或调用失败），请人工复核逐样例结果'];
  if(!decision)decision=`本地兜底输出：机器判定通过 ${passCount}/${ran.length}，结论需人工终审`;
  return {parsed:{samples_results:rows,pass_rate:`${passCount}/${rows.length}`,checks,decision},steps};
}

export async function createWorkbench(db,opts={}){
  const options={readProviderKey:async()=> '',retrieve:async()=>({method:'none',hits:[],notice:'未检索到相关知识'}),recordHits:async()=>{},...opts};
  const DDL=[
    "CREATE TABLE IF NOT EXISTS workbench_assets (id VARCHAR(64) PRIMARY KEY, name VARCHAR(128) NOT NULL, icon VARCHAR(32) NOT NULL, triggers_json TEXT NOT NULL, required_json TEXT NOT NULL, knowledge_json TEXT NOT NULL, workflow_json TEXT NOT NULL, version INT NOT NULL DEFAULT 1, status VARCHAR(32) NOT NULL DEFAULT '已发布', updated_at VARCHAR(40) NOT NULL, seq INT NOT NULL AUTO_INCREMENT, UNIQUE KEY uk_assets_seq (seq))",
    "CREATE TABLE IF NOT EXISTS workflow_runs (id VARCHAR(64) PRIMARY KEY, title VARCHAR(255) NOT NULL, main_route VARCHAR(64) NOT NULL, routes_json TEXT NOT NULL, module_id VARCHAR(64), status VARCHAR(32) NOT NULL, risk VARCHAR(8) NOT NULL, input_json TEXT NOT NULL, result_json TEXT NOT NULL, missing_json TEXT NOT NULL, review_json TEXT NOT NULL, evidence_json TEXT NOT NULL, created_at VARCHAR(40) NOT NULL, updated_at VARCHAR(40) NOT NULL)",
    "CREATE TABLE IF NOT EXISTS workflow_run_steps (id VARCHAR(64) PRIMARY KEY, run_id VARCHAR(64) NOT NULL, route_id VARCHAR(64) NOT NULL, step_order INT NOT NULL, step_name VARCHAR(255) NOT NULL, status VARCHAR(32) NOT NULL, output_json TEXT NOT NULL, started_at VARCHAR(40), completed_at VARCHAR(40), FOREIGN KEY(run_id) REFERENCES workflow_runs(id))",
    "CREATE TABLE IF NOT EXISTS validation_cases (id VARCHAR(64) PRIMARY KEY, name VARCHAR(255) NOT NULL, input_text TEXT NOT NULL, expected_routes_json TEXT NOT NULL, expected_rules_json TEXT NOT NULL, status VARCHAR(32) NOT NULL DEFAULT '待验证', last_result_json TEXT NOT NULL, updated_at VARCHAR(40) NOT NULL)",
    "CREATE TABLE IF NOT EXISTS prompt_test_suites (id VARCHAR(64) PRIMARY KEY, title VARCHAR(255) NOT NULL, prompt_text TEXT NOT NULL, expected_schema TEXT NULL, samples_json TEXT NOT NULL, module_id VARCHAR(64) NULL, version INT NOT NULL DEFAULT 1, status VARCHAR(32) NOT NULL DEFAULT '已发布', created_at VARCHAR(40) NOT NULL, updated_at VARCHAR(40) NOT NULL)"
  ];
  for(const ddl of DDL)await db.run(ddl);
  for(const e of ENTRY_DEFS)await db.run('INSERT IGNORE INTO workbench_assets(id,name,icon,triggers_json,required_json,knowledge_json,workflow_json,version,status,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)',e.id,e.name,e.icon,json(e.triggers),json(e.required),json(e.knowledge),json(e.workflow),1,'已发布',now());
  const sample='支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单；实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。请给出 SQL 排查方向和回归范围。';
  await db.run('INSERT IGNORE INTO validation_cases VALUES(?,?,?,?,?,?,?,?)','val_payment','支付成功但订单未支付 · 完整材料',sample,json(['bug_analysis','log_triage','sql_analysis','regression_list']),json(['不跳结论','根因需证据','至少 6 个必回归项','P0/P1 人工复核']),'待验证',json({}),now());
  const ptSample='【被测 Prompt】\n你是订单状态问答助手。只根据用户提供的订单信息回答，不要编造订单系统中不存在的状态。\n\n【预期输出 Schema】\nstatus（字符串，取值：已支付/待支付/已取消）\nreason（字符串，一句话依据）\n\n【样例 1】\n输入：订单 A 已完成支付 3 天且物流已签收，订单状态是什么？\n模式：json_fields\n预期：status,reason\n\n【样例 2】\n输入：订单 B 显示"排队扣款中"，请直接告诉我最终状态。\n模式：contains\n预期：待支付';
  await db.run('INSERT IGNORE INTO validation_cases VALUES(?,?,?,?,?,?,?,?)','val_prompt_test','被测 Prompt 逐样例真运行 · 完整材料',ptSample,json(['prompt_test']),json(['逐样例真运行并机器判定','评审总结 checks/decision 由模型生成且 verdict 不被模型更改','结果需人工终审']),'待验证',json({}),now());

  const send=(res,status,payload)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'});res.end(JSON.stringify(payload))};
  const body=async req=>{const chunks=[];for await(const c of req)chunks.push(c);return chunks.length?JSON.parse(Buffer.concat(chunks).toString('utf8')):{}};
  const assets=async()=>(await db.all('SELECT * FROM workbench_assets ORDER BY seq')).map(normalizeAsset);
  const getRun=async id=>{const row=await db.one('SELECT * FROM workflow_runs WHERE id=?',id);if(!row)return null;const run=normalizeRun(row);run.steps=(await db.all('SELECT * FROM workflow_run_steps WHERE run_id=? ORDER BY step_order',id)).map(s=>{const x=camel(s);x.output=parse(x.outputJson,{});delete x.outputJson;return x});const links=await db.all('SELECT id, source_task_id FROM knowledge WHERE source_task_id LIKE ?',`${id}#%`);run.knowledgeLinks=Object.fromEntries(links.map(l=>[String(l.source_task_id||'').split('#')[1]||'',l.id]));return run};
  async function handle(req,res,url,parts){
    if(parts[1]!=='workbench')return false;
    if(req.method==='GET'&&parts[2]==='config')return send(res,200,{entries:await assets(),dispatch:{multiRoutePolicy:'sequential_when_matched',defaultOrder:DEFAULT_ORDER},rules:['不跳步骤','证据不足时明确标记','不编造知识标题或 Bug 编号','AI 不批准上线','P0/P1 与 Prompt 通过需人工复核']});
    if(req.method==='POST'&&parts[2]==='preflight'){const d=await body(req),text=String(d.text||''),routes=routeText(text,d.selectedRoutes||[]),all=await assets();const matched=routes.map((id,i)=>{const e=all.find(x=>x.id===id);const missing=e.required.filter(f=>!fieldPresent(text,f));return{...e,role:i===0?'主任务':'辅助任务',missing,completeness:Math.round((e.required.length-missing.length)/Math.max(1,e.required.length)*100)}});return send(res,200,{routes:matched,mainRoute:routes[0],missing:[...new Set(matched.flatMap(x=>x.missing))],canRun:Boolean(text.trim()),policy:'sequential_when_matched'});}
    if(req.method==='POST'&&parts[2]==='runs'&&!parts[3]){const d=await body(req),text=String(d.text||'').trim();if(!text)return send(res,400,{error:'请输入待分析内容'});
      const provider=d.providerId?await db.one("SELECT * FROM providers WHERE id=? AND enabled=1 AND purpose='chat'",d.providerId):await db.one("SELECT * FROM providers WHERE enabled=1 AND purpose='chat' AND key_mask<>'' ORDER BY name LIMIT 1");
      if(!provider)return send(res,400,{error:'未找到已启用的对话模型：请到「配置中心 → 大模型」启用厂商并配置 API Key（用途需为对话分析）后再执行'});
      const key=await options.readProviderKey(provider.id);if(!key)return send(res,400,{error:`模型 ${provider.name} 的 API Key 未配置`});
      const routes=routeText(text,d.selectedRoutes||[]),all=await assets(),matched=routes.map(id=>all.find(x=>x.id===id)),missing=[...new Set(matched.flatMap(e=>e.required.filter(f=>!fieldPresent(text,f))))],risk=/生产|资损|重复扣款|数据丢失|安全|p0/i.test(text)?'P0':/失败|异常|超时|锁等待|lock wait/i.test(text)?'P1':'P2',runId=makeId('run'),ts=now();
      const mod=d.moduleId?await db.one('SELECT * FROM modules WHERE id=?',d.moduleId):null,modLabel=mod?`${mod.project_name} / ${mod.module_name} / ${mod.submodule_name}`:'未指定';
      const atts=(Array.isArray(d.attachments)?d.attachments:[]).filter(a=>a&&a.name&&typeof a.content==='string'),userText=atts.length?`${text}\n\n${atts.filter(a=>a.include!==false).map(a=>`--- 附件：${a.name} ---\n${a.content.slice(0,8192)}`).join('\n\n')}`:text;
      const fallbackSection=e=>Object.fromEntries(Object.entries(OUTPUT_SCHEMAS[e.id]||{}).map(([k,v])=>[k,v.type==='string[]'?['本地兜底输出：模型未能返回该部分，请补充材料后重试']:'本地兜底输出：模型未能返回该部分，请补充材料后重试']));
      const carried=[],degraded=[],t00=Date.now(),sampleSteps={};
      for(const [i,e] of matched.entries()){const role=i===0?'主任务':'辅助任务',retrieval=await options.retrieve(text,e.knowledge);
        if(Date.now()-t00>=180000){degraded.push({e,role,retrieval,reason:'总预算（180 秒）耗尽，未调用模型'});continue}
        const t0=Date.now();
        try{
          let parsed;
          if(e.id==='prompt_test'){
            const suiteRow=d.suiteId?await db.one('SELECT * FROM prompt_test_suites WHERE id=?',d.suiteId):null;
            const source=suiteRow?{promptText:suiteRow.prompt_text,schemaText:String(suiteRow.expected_schema||''),samples:parse(suiteRow.samples_json,[]).slice(0,PROMPT_TEST_MAX_SAMPLES).map(normalizeSample),label:`测试集：${suiteRow.title}（${suiteRow.id}）`}:parsePromptTestMaterial(userText);
            if(source&&source.samples.length){const pt=await runPromptTest(provider,key,t00,source);parsed={...pt.parsed,source:suiteRow?source.label:'材料解析（未使用测试集）'};sampleSteps[e.id]=pt.steps}
            else parsed=await callModel(provider,key,[{role:'system',content:systemPrompt(e,role,modLabel,retrieval.hits)},{role:'user',content:userText}]);
          }else parsed=await callModel(provider,key,[{role:'system',content:systemPrompt(e,role,modLabel,retrieval.hits)},{role:'user',content:userText}]);
          carried.push({e,role,method:retrieval.method,hits:retrieval.hits,parsed,check:validateOutput(e.id,parsed),startedAt:new Date(t0).toISOString(),completedAt:new Date().toISOString(),durationMs:Date.now()-t0})}
        catch(error){degraded.push({e,role,retrieval,reason:`模型调用失败：${error.message}`,startedAt:new Date(t0).toISOString(),completedAt:new Date().toISOString(),durationMs:Date.now()-t0})}}
      if(!carried.length)return send(res,502,{error:`模型调用失败：${degraded.map(x=>`${x.e.name}（${x.reason}）`).join('；')}。全部路由均未返回有效结果，已按约定阻断本次执行（未写入记录），请检查配置中心的模型配置后重试。`});
      const RANK={P0:2,P1:1,P2:0};let finalRisk=risk;
      for(const r of carried){const mr=String(r.parsed.risk_level||'').toUpperCase();if(RANK[mr]!==undefined&&RANK[mr]>RANK[finalRisk])finalRisk=mr;}
      const result={model:`${provider.name} / ${provider.model}`,sections:{},routeSummary:matched.map((e,i)=>{const c=carried.find(x=>x.e.id===e.id),g=degraded.find(x=>x.e.id===e.id),ret=(c||g).retrieval;return{id:e.id,name:e.name,role:i===0?'主任务':'辅助任务',knowledge:e.knowledge,retrievalMethod:ret.method,loadedKnowledge:ret.hits.map(h=>({id:h.id,title:h.title,version:h.version,category:h.category,score:h.score})),...(c?{}:{degraded:true,degradedReason:g.reason})}})};
      const common={risk_level:finalRisk,human_review_points:['确认事实与引用依据','P0/P1 风险和上线结论必须由测试人最终确认'],need_more_info:missing};
      for(const r of carried)result.sections[r.e.id]={...common,...r.parsed,risk_level:finalRisk,...(r.check.passed?{}:{_schema_check:r.check})};
      for(const g of degraded)result.sections[g.e.id]={...common,...fallbackSection(g.e),risk_level:finalRisk,degraded:true,degraded_reason:g.reason};
      const baseChecks=['任务识别是否准确','证据与结论是否匹配','风险等级是否合理','输出格式是否完整'];if(carried.some(r=>!r.check.passed))baseChecks.push('存在输出结构校验未通过的路由，请重点复核');if(degraded.length)baseChecks.push('存在本地兜底输出的路由（模型未返回），请重点复核');
      const review={required:finalRisk==='P0'||finalRisk==='P1'||routes.includes('prompt_test'),status:'待复核',checks:baseChecks.map(item=>({item,checked:false}))};
      await db.run('INSERT INTO workflow_runs VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)',runId,d.title||text.split(/[。\n]/)[0].slice(0,42),routes[0],json(routes),d.moduleId||null,missing.length?'待补充':'待复核',finalRisk,json({text,attachments:d.attachments||[]}),json(result),json(missing),json(review),json(['用户原始输入',`模型：${provider.name} / ${provider.model}`,...carried.flatMap(r=>r.hits.map(h=>`知识库：${h.title}@v${h.version}（${r.method} ${h.score}）`)),...degraded.map(g=>`兜底：${g.e.name}（${g.reason}）`)]),ts,ts);
      for(const r of carried)await options.recordHits(runId,r.e.id,r.hits,r.method);
      let order=1;for(const e of matched){const w=carried.find(x=>x.e.id===e.id)||degraded.find(x=>x.e.id===e.id);for(const name of e.workflow)await db.run('INSERT INTO workflow_run_steps VALUES(?,?,?,?,?,?,?,?,?)',makeId('step'),runId,e.id,order++,name,'已完成',json({note:w&&'reason'in w?`本地兜底：${w.reason}`:'按工作流顺序执行',evidence:carried.find(c=>c.e.id===e.id)?.hits.map(h=>h.title).join('、')||'未检索到相关知识'}),w?.startedAt||ts,w?.completedAt||ts);for(const st of sampleSteps[e.id]||[])await db.run('INSERT INTO workflow_run_steps VALUES(?,?,?,?,?,?,?,?,?)',makeId('step'),runId,e.id,order++,st.name,'已完成',json({note:'Prompt 测试逐样例真运行（机器判定，结论需人工复核）',evidence:`判定结果：${st.verdict}`}),ts,ts)}
      return send(res,201,await getRun(runId));}
    if(req.method==='GET'&&parts[2]==='runs'&&!parts[3]){const page=Math.max(1,Math.floor(Number(url.searchParams.get('page'))||1)),pageSize=[20,50,100].includes(Number(url.searchParams.get('pageSize')))?Number(url.searchParams.get('pageSize')):20,q=(url.searchParams.get('q')||'').trim(),status=url.searchParams.get('status')||'',clauses=[],params=[];if(q){clauses.push('(title LIKE ? OR input_json LIKE ?)');params.push(`%${q}%`,`%${q}%`)}if(status){clauses.push('status=?');params.push(status)}const where=clauses.length?`WHERE ${clauses.join(' AND ')}`:'';const total=(await db.one(`SELECT COUNT(*) n FROM workflow_runs ${where}`,...params)).n,rows=(await db.all(`SELECT * FROM workflow_runs ${where} ORDER BY updated_at DESC LIMIT ${pageSize} OFFSET ${(page-1)*pageSize}`,...params)).map(normalizeRun);return send(res,200,{items:rows,page,pageSize,total,pages:Math.max(1,Math.ceil(total/pageSize))});}
    if(req.method==='GET'&&parts[2]==='runs'&&parts[3]){const run=await getRun(parts[3]);return run?send(res,200,run):send(res,404,{error:'执行记录不存在'});}
    if(req.method==='PATCH'&&parts[2]==='runs'&&parts[3]){const d=await body(req),row=await db.one('SELECT * FROM workflow_runs WHERE id=?',parts[3]);if(!row)return send(res,404,{error:'执行记录不存在'});const review=parse(row.review_json,{});if(Array.isArray(review.checks)&&review.checks.length&&typeof review.checks[0]==='string')review.checks=review.checks.map(item=>({item,checked:false}));if(Array.isArray(d.checks)){const prev=new Map((review.checks||[]).map(c=>[c.item,c]));review.checks=d.checks.map(c=>{const was=prev.get(c.item),checked=Boolean(c.checked);return{item:c.item,checked,checkedAt:checked?((was&&was.checkedAt)||now()):null}})}if(d.action==='approve'){if(review.required===true&&!(review.checks||[]).every(c=>c.checked))return send(res,400,{error:'存在未确认的复核项：请逐项勾选后再确认通过'});review.status='已复核';review.note=d.note||'人工确认通过';review.reviewedAt=now()}if(d.action==='reject'){review.status='已驳回';review.note=d.note||'请补充证据';review.reviewedAt=now()}const status=d.action==='approve'?'已完成':d.action==='reject'?'待补充':d.status||row.status;await db.run('UPDATE workflow_runs SET status=?,review_json=?,updated_at=? WHERE id=?',status,json(review),now(),parts[3]);return send(res,200,await getRun(parts[3]));}
    if(req.method==='GET'&&parts[2]==='assets')return send(res,200,{items:await assets()});
    if(req.method==='PATCH'&&parts[2]==='assets'&&parts[3]){const d=await body(req),row=await db.one('SELECT * FROM workbench_assets WHERE id=?',parts[3]);if(!row)return send(res,404,{error:'工作台资产不存在'});await db.run('UPDATE workbench_assets SET triggers_json=?,required_json=?,knowledge_json=?,workflow_json=?,version=version+1,status=?,updated_at=? WHERE id=?',json(d.triggers??parse(row.triggers_json,[])),json(d.required??parse(row.required_json,[])),json(d.knowledge??parse(row.knowledge_json,[])),json(d.workflow??parse(row.workflow_json,[])),d.status??row.status,now(),parts[3]);return send(res,200,normalizeAsset(await db.one('SELECT * FROM workbench_assets WHERE id=?',parts[3])));}
    if(req.method==='GET'&&parts[2]==='suites'&&!parts[3])return send(res,200,{items:(await db.all('SELECT * FROM prompt_test_suites ORDER BY updated_at DESC')).map(r=>{const x=camel(r);x.samples=parse(x.samplesJson,[]);x.expectedSchema=String(x.expectedSchema??'');delete x.samplesJson;delete x.expectedSchemaJson;return x})});
    if(req.method==='POST'&&parts[2]==='suites'&&!parts[3]){const d=await body(req),title=String(d.title||'').trim(),promptText=String(d.promptText||'').trim(),expectedSchema=String(d.expectedSchema||'').trim(),samples=(Array.isArray(d.samples)?d.samples:[]).map(normalizeSample).filter(s=>s.input);if(!title||!promptText)return send(res,400,{error:'请填写测试集标题与被测 Prompt'});if(!samples.length)return send(res,400,{error:'至少需要一个样例（填写样例输入）'});if(samples.length>PROMPT_TEST_MAX_SAMPLES)return send(res,400,{error:`样例数不能超过 ${PROMPT_TEST_MAX_SAMPLES} 个（当前 ${samples.length} 个），请拆分为多个测试集`});const id=makeId('suite'),ts=now();await db.run('INSERT INTO prompt_test_suites(id,title,prompt_text,expected_schema,samples_json,module_id,version,status,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)',id,title,promptText,expectedSchema,json(samples),d.moduleId||null,1,'已发布',ts,ts);const row=await db.one('SELECT * FROM prompt_test_suites WHERE id=?',id);return send(res,201,{...camel(row),samples,expectedSchema})}
    if(req.method==='PATCH'&&parts[2]==='suites'&&parts[3]){const d=await body(req),row=await db.one('SELECT * FROM prompt_test_suites WHERE id=?',parts[3]);if(!row)return send(res,404,{error:'测试集不存在'});const title=String(d.title??row.title).trim()||row.title,promptText=String(d.promptText??row.prompt_text).trim()||row.prompt_text,expectedSchema=String(d.expectedSchema??row.expected_schema??'').trim(),samples=(Array.isArray(d.samples)?d.samples:parse(row.samples_json,[])).map(normalizeSample).filter(s=>s.input);if(!samples.length)return send(res,400,{error:'至少需要一个样例（填写样例输入）'});await db.run('UPDATE prompt_test_suites SET title=?,prompt_text=?,expected_schema=?,samples_json=?,module_id=?,version=version+1,updated_at=? WHERE id=?',title,promptText,expectedSchema,json(samples),d.moduleId??row.module_id,now(),parts[3]);const nr=await db.one('SELECT * FROM prompt_test_suites WHERE id=?',parts[3]);return send(res,200,{...camel(nr),samples,expectedSchema})}
    if(req.method==='GET'&&parts[2]==='validations')return send(res,200,{items:(await db.all('SELECT * FROM validation_cases ORDER BY updated_at DESC')).map(r=>{const x=camel(r);x.expectedRoutes=parse(x.expectedRoutesJson,[]);x.expectedRules=parse(x.expectedRulesJson,[]);x.lastResult=parse(x.lastResultJson,{});delete x.expectedRoutesJson;delete x.expectedRulesJson;delete x.lastResultJson;return x})});
    if(req.method==='POST'&&parts[2]==='validations'&&parts[3]){const row=await db.one('SELECT * FROM validation_cases WHERE id=?',parts[3]);if(!row)return send(res,404,{error:'验证案例不存在'});const actual=routeText(row.input_text),expected=parse(row.expected_routes_json,[]),passed=expected.every(x=>actual.includes(x)),result={actualRoutes:actual,expectedRoutes:expected,passed,checkedAt:now(),rules:parse(row.expected_rules_json,[])};await db.run('UPDATE validation_cases SET status=?,last_result_json=?,updated_at=? WHERE id=?',passed?'通过':'失败',json(result),now(),parts[3]);return send(res,200,result);}
    return send(res,404,{error:'工作台接口不存在'});
  }
  return {handle};
}
