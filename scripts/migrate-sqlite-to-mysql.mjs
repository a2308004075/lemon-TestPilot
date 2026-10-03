// 一次性迁移脚本：把 data/testpilot.sqlite（只读）里的数据导入 MySQL。
// 幂等：INSERT IGNORE 按主键去重，可重复运行；原 SQLite 文件保留不动。
import { DatabaseSync } from 'node:sqlite';
import { existsSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createDb } from '../db.mjs';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const sqlitePath = join(root, 'data', 'testpilot.sqlite');
if (!existsSync(sqlitePath)) {
  console.error(`未找到旧数据库文件：${sqlitePath}，无需迁移。`);
  process.exit(0);
}

const src = new DatabaseSync(sqlitePath, { readOnly: true });
const db = await createDb();

// 外键依赖顺序：modules 最先，workflow_runs 先于 workflow_run_steps。
const TABLES = [
  ['modules', ['id', 'project_name', 'module_name', 'submodule_name', 'status', 'sort_order', 'updated_at']],
  ['tasks', ['id', 'type', 'title', 'module_id', 'risk', 'status', 'review_status', 'knowledge_status', 'parent_id', 'input_json', 'result_json', 'model', 'created_at', 'updated_at']],
  ['cases', ['id', 'code', 'title', 'module_id', 'priority', 'case_type', 'version', 'status', 'source_task_id', 'content_json', 'archived', 'created_at', 'updated_at']],
  ['knowledge', ['id', 'title', 'category', 'module_id', 'risk', 'status', 'version', 'content', 'source_task_id', 'review_note', 'created_at', 'updated_at']],
  ['regressions', ['id', 'title', 'module_id', 'status', 'progress', 'data_json', 'created_at', 'updated_at']],
  ['reports', ['id', 'title', 'project_name', 'verdict', 'version', 'data_json', 'created_at', 'updated_at']],
  ['workflows', ['id', 'name', 'steps_json', 'status', 'created_at', 'updated_at']],
  ['providers', ['id', 'name', 'base_url', 'model', 'temperature', 'timeout', 'max_tokens', 'enabled', 'key_mask']],
  ['settings', ['key', 'value_json']],
  ['migrations', ['key', 'completed_at', 'backup_path']],
  ['relations', ['source_type', 'source_id', 'target_type', 'target_id', 'label', 'active']],
  ['audit', ['entity_type', 'entity_id', 'action', 'detail', 'created_at']],
  ['workbench_assets', ['id', 'name', 'icon', 'triggers_json', 'required_json', 'knowledge_json', 'workflow_json', 'version', 'status', 'updated_at']],
  ['workflow_runs', ['id', 'title', 'main_route', 'routes_json', 'module_id', 'status', 'risk', 'input_json', 'result_json', 'missing_json', 'review_json', 'evidence_json', 'created_at', 'updated_at']],
  ['workflow_run_steps', ['id', 'run_id', 'route_id', 'step_order', 'step_name', 'status', 'output_json', 'started_at', 'completed_at']],
  ['validation_cases', ['id', 'name', 'input_text', 'expected_routes_json', 'expected_rules_json', 'status', 'last_result_json', 'updated_at']]
];

let total = 0;
for (const [table, cols] of TABLES) {
  const order = table === 'workbench_assets' ? ' ORDER BY rowid' : '';
  const rows = src.prepare(`SELECT * FROM ${table}${order}`).all();
  const placeholders = cols.map(() => '?').join(',');
  const colList = cols.map(c => (c === 'key' ? '`key`' : c)).join(',');
  for (const row of rows) {
    await db.run(`INSERT IGNORE INTO ${table}(${colList}) VALUES(${placeholders})`, ...cols.map(c => row[c]));
    total++;
  }
  console.log(`${table}: 读取 ${rows.length} 行`);
}
await db.end();
src.close();
console.log(`\n迁移完成，共处理 ${total} 行（主键冲突的行已自动跳过）。`);
console.log(`原 SQLite 文件保留在 ${sqlitePath}，确认新库数据无误后可自行归档。`);
