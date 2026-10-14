<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  Download, Search, Layers3, Check, ChevronDown, ChevronRight,
  ListChecks, Sparkles, Upload
} from 'lucide-vue-next'
import Badge from '../components/ui/Badge.vue'
import TPagination from '../components/ui/TPagination.vue'
import TDrawer from '../components/ui/TDrawer.vue'
import TModal from '../components/ui/TModal.vue'
import ExecutionBatches from './components/ExecutionBatches.vue'
import { nextTick } from 'vue'
import {
  apiCases, apiCaseTree, apiCaseModules, apiCaseCreate, apiCaseUpdate, apiCaseCopy, apiCaseDelete,
  apiCaseGenerateDrafts, apiCaseBatchCreate, apiCaseImport, apiAudit
} from '../api'
import { setAnalysisPrefill } from '../utils/analysisPrefill'
import { fmtTime, fmtDate } from '../utils/format'

const router = useRouter()
const tab = ref('library')
const query = ref({ keyword: '', priority: '', status: '', moduleName: '', submoduleName: '', modulePath: '', page: 1, size: 20 })
const rows = ref([])
const total = ref(0)
const tree = ref([])
const treeOpen = ref({})
const selection = ref([])
const moduleOptions = ref([])

const formVisible = ref(false)
const editId = ref(null)
const form = ref({})
const modulePath = ref('')

const genVisible = ref(false)
const genContext = ref('')
const generating = ref(false)
const drafts = ref([])

const detailVisible = ref(false)
const detail = ref(null)
const audits = ref([])
const creatingRun = ref(false)
const batchTitleVisible = ref(false)
const batchTitle = ref('')
const batchTitleInput = ref(null)

const p0Count = computed(() => rows.value.filter((r) => r.priority === 'P0').length)
const allSelected = computed(() => rows.value.length > 0 && selection.value.length === rows.value.length)

const shortModule = (row) => [row.moduleName, row.submoduleName].filter(Boolean).join(' / ') || '未分组'
const stepsCount = (row) => (row.steps || '').split('\n').map((s) => s.trim()).filter(Boolean).length

const load = async () => {
  const data = await apiCases(query.value)
  rows.value = data.list || []
  total.value = data.total || 0
}

// 模块树按条目数降序排列（与手册一致：数据多的项目在前）
const sortTree = (nodes) => {
  ;(nodes || []).sort((a, b) => (b.count || 0) - (a.count || 0))
  for (const n of nodes || []) {
    sortTree(n.modules)
    sortTree(n.submodules)
  }
  return nodes
}

const loadTree = async () => {
  tree.value = sortTree(await apiCaseTree())
}

const nodeOpen = (key) => treeOpen.value[key] !== false
const toggleNode = (key) => {
  treeOpen.value = { ...treeOpen.value, [key]: !nodeOpen(key) }
}
const collapseAll = () => {
  const closed = {}
  tree.value.forEach((p) => {
    closed[p.name] = false
    ;(p.modules || []).forEach((m) => { closed[`${p.name}/${m.name}`] = false })
  })
  treeOpen.value = closed
}

const onLeafClick = (moduleName, subName) => {
  query.value.moduleName = moduleName
  query.value.submoduleName = subName
  query.value.page = 1
  load()
}

const onModuleSelect = () => {
  const parts = (query.value.modulePath || '').split(' / ')
  if (!query.value.modulePath) {
    query.value.moduleName = ''
    query.value.submoduleName = ''
  } else {
    query.value.moduleName = parts[1] || ''
    query.value.submoduleName = parts[2] || ''
  }
  query.value.page = 1
  load()
}

const toggleRow = (row) => {
  const id = row.id
  selection.value = selection.value.some((r) => r.id === id)
    ? selection.value.filter((r) => r.id !== id)
    : [...selection.value, row]
}
const toggleAll = () => {
  selection.value = allSelected.value ? [] : [...rows.value]
}

const emptyForm = () => ({
  title: '', priority: 'P1', type: '功能', status: '使用',
  preconditions: '', steps: '', expected: '', testData: '', linkedBugNo: ''
})

const openCreate = () => {
  tab.value = 'new'
  editId.value = null
  form.value = emptyForm()
  modulePath.value = ''
}

const openEdit = (row) => {
  editId.value = row.id
  form.value = {
    title: row.title, priority: row.priority, type: row.type, status: row.status || '使用',
    preconditions: row.preconditions, steps: row.steps, expected: row.expected,
    testData: row.testData, linkedBugNo: row.linkedBugNo
  }
  modulePath.value = [row.projectName, row.moduleName, row.submoduleName].filter(Boolean).join(' / ')
  detailVisible.value = false
  formVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写用例标题')
    return
  }
  if (!modulePath.value) {
    ElMessage.warning('请选择所属模块')
    return
  }
  const parts = modulePath.value.split(' / ')
  const payload = {
    ...form.value,
    projectName: parts[0] || '',
    moduleName: parts[1] || '',
    submoduleName: parts[2] || ''
  }
  if (editId.value) {
    const saved = await apiCaseUpdate(editId.value, payload)
    ElMessage.success(`已保存并升版至 v${saved.version}`)
  } else {
    await apiCaseCreate(payload)
    ElMessage.success('用例已入库')
  }
  formVisible.value = false
  tab.value = 'library'
  await load()
  await loadTree()
}

const copyCase = async (row) => {
  const c = await apiCaseCopy(row.id)
  ElMessage.success(`已复制为 ${c.caseNo}（${c.title}）`)
  await load()
  await loadTree()
}

/** 删除废弃用例：二次确认后物理删除，关闭抽屉并刷新列表与模块树 */
const removeCase = async () => {
  if (!confirm(`确定删除用例「${detail.value.title}」？\n删除后不可恢复。`)) return
  await apiCaseDelete(detail.value.id)
  ElMessage.success('用例已删除')
  detailVisible.value = false
  await load()
  await loadTree()
}

const openGenerate = () => {
  genVisible.value = true
  drafts.value = []
}

const generate = async () => {
  if (!genContext.value.trim()) {
    ElMessage.warning('请粘贴需求或问题上下文')
    return
  }
  generating.value = true
  try {
    drafts.value = await apiCaseGenerateDrafts({ context: genContext.value })
    if (!drafts.value.length) ElMessage.info('未生成草稿，请补充上下文')
  } finally {
    generating.value = false
  }
}

const saveDraft = async (d) => {
  const parts = (d.modulePath || d.moduleName || '').split(' / ')
  await apiCaseCreate({
    title: d.title, projectName: '电商平台',
    moduleName: parts[parts.length - 2] || d.moduleName || '',
    submoduleName: parts[parts.length - 1] || d.submoduleName || '',
    priority: d.priority || 'P1', type: d.type || '功能',
    preconditions: d.preconditions || '', steps: d.steps || '',
    expected: d.expected || '', testData: d.testData || '', linkedBugNo: ''
  })
  ElMessage.success('草稿已入库')
  drafts.value = drafts.value.filter((x) => x !== d)
  await load()
  await loadTree()
}

const openDetail = async (row) => {
  detail.value = row
  detailVisible.value = true
  audits.value = await apiAudit({ entityType: 'case', entityNo: row.caseNo })
}

/** 勾选导出 XLSX（批量栏入口，全列快照） */
const exportCases = (items) => {
  const list = items && items.length ? items : rows.value
  if (!list.length) {
    ElMessage.info('没有可导出的用例')
    return
  }
  const data = list.map((c) => ({
    编号: c.caseNo, 标题: c.title, 项目: c.projectName, 模块: c.moduleName,
    子模块: c.submoduleName, 优先级: c.priority, 类型: c.type, 状态: c.status,
    版本: 'v' + c.version, 关联Bug: c.linkedBugNo, 前置条件: c.preconditions || '',
    测试步骤: c.steps || '', 预期结果: c.expected || '', 测试数据: c.testData || ''
  }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), '用例库')
  XLSX.writeFile(wb, `用例库_${list.length}条.xlsx`)
  ElMessage.success(`已导出 ${list.length} 条用例（XLSX）`)
}

/** 导出导入模版：11 列有效表头 + 1 行示例（示例行标题自带删除提示） */
const exportTemplate = () => {
  const data = [{
    标题: '【示例】购物车添加商品，导入前请删除本行',
    项目: '电商平台', 模块: '购物车', 子模块: '添加商品',
    优先级: 'P1', 类型: '功能', 关联Bug: '',
    前置条件: '已登录且购物车为空',
    测试步骤: '1. 打开商品详情\n2. 点击加入购物车\n3. 查看购物车角标',
    预期结果: '购物车数量 +1，金额合计正确',
    测试数据: 'SKU-001'
  }]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), '用例模版')
  XLSX.writeFile(wb, '用例导入模版.xlsx')
  ElMessage.success('已导出导入模版（示例行导入前请删除）')
}

/** 导入 XLSX：前端解析 → 预览确认 → 仅提交有效行；重复用例由后端查重跳过 */
const importFileInput = ref(null)
const importVisible = ref(false)
const importRows = ref([])
const importFileName = ref('')
const importing = ref(false)
const importValidCount = computed(() => importRows.value.filter((r) => r.valid).length)

const onImportFile = async (e) => {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  try {
    const buf = await file.arrayBuffer()
    const wb = XLSX.read(buf, { type: 'array' })
    const sheet = wb.Sheets[wb.SheetNames[0]]
    const raw = sheet ? XLSX.utils.sheet_to_json(sheet) : []
    const rows = raw.map((r) => {
      const title = String(r['标题'] ?? '').trim()
      const moduleName = String(r['模块'] ?? '').trim()
      const reason = !title ? '标题为空' : (!moduleName ? '模块为空' : '')
      return {
        title, moduleName, reason, valid: !reason,
        projectName: String(r['项目'] ?? '').trim(),
        submoduleName: String(r['子模块'] ?? '').trim(),
        priority: String(r['优先级'] ?? '').trim() || 'P1',
        type: String(r['类型'] ?? '').trim() || '功能',
        linkedBugNo: String(r['关联Bug'] ?? '').trim(),
        preconditions: String(r['前置条件'] ?? '').trim(),
        steps: String(r['测试步骤'] ?? '').trim(),
        expected: String(r['预期结果'] ?? '').trim(),
        testData: String(r['测试数据'] ?? '').trim()
      }
    })
    if (!rows.length) {
      ElMessage.info('未解析到任何用例行（需含“标题”等中文表头）')
      return
    }
    importFileName.value = file.name
    importRows.value = rows
    importVisible.value = true
  } catch (err) {
    ElMessage.error('文件解析失败，请确认是有效的 XLSX 文件')
  }
}

const confirmImport = async () => {
  const valid = importRows.value.filter((r) => r.valid)
  if (!valid.length) {
    ElMessage.warning('没有可导入的有效用例')
    return
  }
  importing.value = true
  try {
    const res = await apiCaseImport({
      cases: valid.map(({ valid: _v, reason: _r, ...c }) => c)
    })
    ElMessage.success(`导入 ${res.imported} 条${res.skipped ? `，跳过重复 ${res.skipped} 条` : ''}`)
    importVisible.value = false
    importRows.value = []
    importFileName.value = ''
    await load()
    await loadTree()
  } finally {
    importing.value = false
  }
}

/** 创建执行：先弹窗输入批次名，再勾选用例生成执行批次（6.11 W3） */
const bulkCreateRun = () => {
  if (!selection.value.length) {
    ElMessage.warning('请先勾选用例')
    return
  }
  batchTitle.value = ''
  batchTitleVisible.value = true
  nextTick(() => batchTitleInput.value?.focus())
}

const confirmBatchCreate = async () => {
  const title = batchTitle.value.trim()
  if (!title) {
    ElMessage.warning('请输入批次名')
    return
  }
  batchTitleVisible.value = false
  creatingRun.value = true
  try {
    const batch = await apiCaseBatchCreate({
      caseIds: selection.value.map((r) => r.id),
      title
    })
    ElMessage.success(`已创建批次（${batch.batch.total} 条）`)
    selection.value = []
    tab.value = 'execution'
  } finally {
    creatingRun.value = false
  }
}

/** 联合分析：拼接 ≤5 条勾选用例为材料，跳转 Bug 分析页预填（6.11 W4） */
const bulkJointAnalysis = () => {
  if (!selection.value.length) {
    ElMessage.warning('请先勾选用例')
    return
  }
  let picked = selection.value
  if (picked.length > 5) {
    picked = picked.slice(0, 5)
    ElMessage.warning('最多携带 5 条用例，已取前 5 条')
  }
  const material = picked.map((c) =>
    `【${c.caseNo}】${c.title}\n步骤：${(c.steps || '').trim() || '无'}\n预期：${(c.expected || '').trim() || '无'}`
  ).join('\n\n')
  setAnalysisPrefill({
    taskType: 'bug_analysis',
    title: picked.length === 1
      ? `联合分析：${picked[0].title}`
      : `联合分析：${picked[0].title} 等 ${picked.length} 条用例`,
    context: `以下为用例库勾选用例的验证要点，请联合分析潜在缺陷与回归风险：\n\n${material}`
  })
  router.push('/bug-analysis')
}

onMounted(async () => {
  await Promise.all([load(), loadTree()])
  const mods = await apiCaseModules()
  moduleOptions.value = (mods || []).map((m) => `${m.project} / ${m.module} / ${m.submodule}`)
})
</script>

<template>
  <div class="page stack">
    <div class="page-intro">
      <div>
        <h2>测试用例资产库</h2>
        <p>按项目、模块、子模块组织，可编辑、执行、关联 Bug、生成回归与报告。</p>
      </div>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'library' }" @click="tab = 'library'">用例库</button>
      <button :class="{ active: tab === 'new' }" @click="openCreate">新增 / 智能生成</button>
      <button :class="{ active: tab === 'execution' }" @click="tab = 'execution'">执行记录</button>
    </div>

    <template v-if="tab === 'library'">
      <div class="filterbar">
        <label class="search">
          <Search />
          <input v-model="query.keyword" placeholder="搜索编号、标题或关键词" @keyup.enter="query.page = 1; load()">
        </label>
        <select v-model="query.modulePath" @change="onModuleSelect">
          <option value="">全部模块</option>
          <option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</option>
        </select>
        <select v-model="query.priority" @change="query.page = 1; load()">
          <option value="">全部优先级</option>
          <option>P0</option>
          <option>P1</option>
          <option>P2</option>
        </select>
        <button class="button" @click="collapseAll"><Layers3 />全部收起</button>
      </div>

      <div v-if="selection.length" class="bulkbar">
        <strong>已选 {{ selection.length }} 条</strong>
        <button :disabled="creatingRun" @click="bulkCreateRun">{{ creatingRun ? '创建中…' : '创建执行' }}</button>
        <button @click="bulkJointAnalysis">联合分析</button>
        <button @click="router.push('/regression')">生成回归</button>
        <button @click="router.push('/reports')">生成报告</button>
        <button @click="exportCases(selection)">下载 XLSX</button>
        <button @click="selection = []">清空选择</button>
      </div>

      <div class="case-layout">
        <aside class="panel module-tree">
          <div class="panel-title">
            <div>
              <h3>模块树</h3>
              <p>显示当前筛选数据</p>
            </div>
          </div>
          <div v-for="p in tree" :key="p.name" class="tree-node">
            <button @click="toggleNode(p.name)">
              <ChevronDown :class="{ closed: !nodeOpen(p.name) }" />
              <strong>{{ p.name }}</strong>
              <b>{{ p.count }}</b>
            </button>
            <div v-if="nodeOpen(p.name)">
              <div v-for="m in p.modules || []" :key="m.name" class="tree-node">
                <button @click="toggleNode(`${p.name}/${m.name}`)">
                  <ChevronDown :class="{ closed: !nodeOpen(`${p.name}/${m.name}`) }" />
                  <strong>{{ m.name }}</strong>
                  <b>{{ m.count }}</b>
                </button>
                <div v-if="nodeOpen(`${p.name}/${m.name}`)">
                  <button v-for="s in m.submodules || []" :key="s.name" class="tree-leaf" @click="onLeafClick(m.name, s.name)">
                    <span>{{ s.name }}</span>
                    <b>{{ s.count }}</b>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <section class="panel table-panel">
          <div class="table-summary">
            <span>共 <strong>{{ total }}</strong> 条 · P0 <strong class="danger-text">{{ p0Count }}</strong></span>
            <div style="display: flex; gap: 8px">
              <button class="button" @click="importFileInput?.click()"><Upload />导入 XLSX</button>
              <button class="button" @click="exportTemplate()"><Download />导出模版</button>
            </div>
          </div>
          <input ref="importFileInput" type="file" accept=".xlsx" style="display: none" @change="onImportFile">
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width: 36px">
                    <input type="checkbox" :checked="allSelected" @click.prevent="toggleAll">
                  </th>
                  <th>用例编号</th>
                  <th>用例标题</th>
                  <th>模块</th>
                  <th>优先级</th>
                  <th>类型</th>
                  <th>步骤</th>
                  <th>状态</th>
                  <th>生成日期</th>
                  <th>版本</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in rows" :key="c.id" @click="openDetail(c)">
                  <td style="cursor: default" @click.stop>
                    <input type="checkbox" :checked="selection.some((r) => r.id === c.id)" @change="toggleRow(c)">
                  </td>
                  <td>{{ c.caseNo }}</td>
                  <td>
                    <strong>{{ c.title }}</strong>
                  </td>
                  <td>{{ shortModule(c) }}</td>
                  <td><Badge :value="c.priority" /></td>
                  <td>{{ c.type }}</td>
                  <td>{{ stepsCount(c) }}</td>
                  <td><Badge :value="c.status" /></td>
                  <td>{{ fmtDate(c.createdAt) }}</td>
                  <td>v{{ c.version }}</td>
                </tr>
                <tr v-if="!rows.length">
                  <td colspan="10" style="text-align: center; color: #9a9e94; cursor: default">没有匹配的用例</td>
                </tr>
              </tbody>
            </table>
          </div>
          <TPagination v-model:page="query.page" v-model:page-size="query.size" :total="total" @update:page="load" @update:page-size="load" />
        </section>
      </div>
    </template>

    <section v-else-if="tab === 'new'" class="panel smart-panel">
      <div class="panel-title">
        <div>
          <h3>新增测试用例</h3>
          <p>保存后自动进入当前模块</p>
        </div>
        <button class="button" @click="openGenerate"><Sparkles />AI 智能生成</button>
      </div>
      <div class="form-grid">
        <label class="field full">
          <span>用例标题<em>*</em></span>
          <input v-model="form.title" placeholder="清晰描述验证目标">
        </label>
        <label class="field">
          <span>模块<em>*</em></span>
          <select v-model="modulePath">
            <option value="">请选择项目 / 模块 / 子模块</option>
            <option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</option>
          </select>
        </label>
        <label class="field">
          <span>优先级</span>
          <select v-model="form.priority">
            <option>P0</option>
            <option>P1</option>
            <option>P2</option>
          </select>
        </label>
        <label class="field">
          <span>用例类型</span>
          <select v-model="form.type">
            <option>功能</option>
            <option>接口</option>
            <option>异常</option>
            <option>边界</option>
            <option>性能</option>
            <option>安全</option>
          </select>
        </label>
        <label class="field">
          <span>状态</span>
          <select v-model="form.status">
            <option>使用</option>
            <option>废弃</option>
          </select>
        </label>
        <label class="field">
          <span>关联 Bug</span>
          <input v-model="form.linkedBugNo" placeholder="如 BUG-20260901-001，可留空">
        </label>
        <label class="field full">
          <span>前置条件</span>
          <textarea v-model="form.preconditions" />
        </label>
        <label class="field full">
          <span>测试步骤（每行一步）<em>*</em></span>
          <textarea v-model="form.steps" placeholder="1. 准备数据&#10;2. 执行操作&#10;3. 检查结果" />
        </label>
        <label class="field full">
          <span>预期结果<em>*</em></span>
          <textarea v-model="form.expected" />
        </label>
        <label class="field full">
          <span>测试数据</span>
          <textarea v-model="form.testData" />
        </label>
      </div>
      <button class="button primary" @click="save"><Check />保存用例</button>
    </section>

    <ExecutionBatches v-else />

    <TModal v-if="genVisible" title="智能生成用例草稿" :width="720" @close="genVisible = false">
      <label class="field">
        <span>需求上下文<em>*</em></span>
        <textarea v-model="genContext" class="large" placeholder="粘贴需求描述 / 问题上下文，引擎将拆解测试点并覆盖正常、异常、边界场景生成结构化用例草稿" />
      </label>
      <div class="smart-actions">
        <button class="button primary" :disabled="generating" @click="generate"><Sparkles />生成草稿</button>
        <span class="tip" style="margin: 0">草稿不直接入库，逐条确认后保存</span>
      </div>
      <template v-if="drafts.length">
        <h4 style="font-size: 12px; color: var(--muted); margin: 18px 0 4px">草稿（{{ drafts.length }} 条）</h4>
        <div v-for="(d, i) in drafts" :key="i" class="detail-section">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
            <Badge :value="d.priority" />
            <strong style="flex: 1">{{ d.title }}</strong>
            <button class="button" style="min-height: 34px" @click="saveDraft(d)">入库</button>
          </div>
          <div v-if="d.preconditions" style="font-size: 12px; color: #565b52; margin-bottom: 4px"><b>前置：</b>{{ d.preconditions }}</div>
          <div style="font-size: 12px; color: #565b52; margin-bottom: 4px"><b>步骤：</b><span style="white-space: pre-line">{{ d.steps }}</span></div>
          <div style="font-size: 12px; color: #565b52"><b>预期：</b>{{ d.expected }}</div>
        </div>
      </template>
    </TModal>

    <TModal v-if="formVisible" :title="editId ? '编辑用例（保存后升版）' : '新增用例'" :width="640" @close="formVisible = false">
      <div class="form-grid">
        <label class="field full">
          <span>用例标题<em>*</em></span>
          <input v-model="form.title">
        </label>
        <label class="field full">
          <span>模块<em>*</em></span>
          <select v-model="modulePath">
            <option value="">请选择项目 / 模块 / 子模块</option>
            <option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</option>
          </select>
        </label>
        <label class="field">
          <span>优先级</span>
          <select v-model="form.priority">
            <option>P0</option>
            <option>P1</option>
            <option>P2</option>
          </select>
        </label>
        <label class="field">
          <span>用例类型</span>
          <select v-model="form.type">
            <option>功能</option>
            <option>接口</option>
            <option>异常</option>
            <option>边界</option>
            <option>性能</option>
            <option>安全</option>
          </select>
        </label>
        <label class="field">
          <span>状态</span>
          <select v-model="form.status">
            <option>使用</option>
            <option>废弃</option>
          </select>
        </label>
        <label class="field full">
          <span>关联 Bug</span>
          <input v-model="form.linkedBugNo">
        </label>
        <label class="field full">
          <span>前置条件</span>
          <textarea v-model="form.preconditions" style="min-height: 70px" />
        </label>
        <label class="field full">
          <span>测试步骤（每行一步）<em>*</em></span>
          <textarea v-model="form.steps" style="min-height: 110px" />
        </label>
        <label class="field full">
          <span>预期结果<em>*</em></span>
          <textarea v-model="form.expected" style="min-height: 70px" />
        </label>
        <label class="field full">
          <span>测试数据</span>
          <textarea v-model="form.testData" style="min-height: 70px" />
        </label>
      </div>
      <template #footer>
        <button class="button" @click="formVisible = false">取消</button>
        <button class="button primary" @click="save"><Check />保存</button>
      </template>
    </TModal>

    <TModal v-if="batchTitleVisible" title="创建执行批次" :width="480" @close="batchTitleVisible = false">
      <label class="field full">
        <span>批次名</span>
        <input ref="batchTitleInput" v-model="batchTitle" placeholder="请输入批次名" @keyup.enter="confirmBatchCreate">
      </label>
      <template #footer>
        <button class="button" @click="batchTitleVisible = false">取消</button>
        <button class="button primary" :disabled="creatingRun" @click="confirmBatchCreate">
          {{ creatingRun ? '创建中…' : '创建' }}
        </button>
      </template>
    </TModal>

    <TModal v-if="importVisible" title="导入 XLSX 预览" :width="720" @close="importVisible = false">
      <p style="font-size: 12px; color: var(--muted); margin: 0 0 10px">
        {{ importFileName }} · 共 {{ importRows.length }} 行，有效 <b>{{ importValidCount }}</b> 条
        <template v-if="importRows.length > importValidCount">，无效 {{ importRows.length - importValidCount }} 条（灰色行不会导入）</template>
        ；与库中标题+模块相同的重复用例提交时自动跳过
      </p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>标题</th>
              <th>模块</th>
              <th>优先级</th>
              <th>类型</th>
              <th>校验</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(r, i) in importRows" :key="i" :style="r.valid ? '' : 'color: #9a9e94'">
              <td>{{ r.title || '（空）' }}</td>
              <td>{{ [r.projectName, r.moduleName, r.submoduleName].filter(Boolean).join(' / ') || '（空）' }}</td>
              <td>{{ r.priority }}</td>
              <td>{{ r.type }}</td>
              <td>{{ r.valid ? '可导入' : r.reason }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <button class="button" @click="importVisible = false">取消</button>
        <button class="button primary" :disabled="importing || !importValidCount" @click="confirmImport">
          {{ importing ? '导入中…' : `导入 ${importValidCount} 条有效用例` }}
        </button>
      </template>
    </TModal>

    <TDrawer
      v-if="detailVisible && detail" eyebrow="DETAIL VIEW"
      :title="detail.title" :subtitle="detail.caseNo" @close="detailVisible = false"
    >
      <div class="detail-badges">
        <Badge :value="detail.priority" />
        <Badge :value="detail.type" />
        <Badge :value="detail.status" />
        <span>v{{ detail.version }}</span>
      </div>
      <div class="detail-grid">
        <div><span>用例编号</span><strong>{{ detail.caseNo }}</strong></div>
        <div><span>所属模块</span><strong>{{ detail.projectName }} / {{ detail.moduleName }} / {{ detail.submoduleName }}</strong></div>
        <div><span>生成日期</span><strong>{{ fmtDate(detail.createdAt) }}</strong></div>
        <div><span>关联 Bug</span><strong>{{ detail.linkedBugNo || '0' }}</strong></div>
      </div>
      <section class="detail-section">
        <h3>前置条件</h3>
        <p>{{ detail.preconditions || '无' }}</p>
      </section>
      <section class="detail-section">
        <h3>测试步骤</h3>
        <ol>
          <li v-for="(s, i) in (detail.steps || '').split('\n').map((x) => x.trim()).filter(Boolean)" :key="i">{{ s }}</li>
        </ol>
      </section>
      <section class="detail-section result-box">
        <h3>预期结果</h3>
        <p>{{ detail.expected || '无' }}</p>
      </section>
      <section v-if="detail.testData" class="detail-section">
        <h3>测试数据</h3>
        <p>{{ detail.testData }}</p>
      </section>
      <div class="drawer-actions">
        <button v-if="detail.status === '废弃'" class="button danger" @click="removeCase">删除用例</button>
        <button class="button" @click="copyCase(detail)">复制用例</button>
        <button class="button primary" @click="openEdit(detail)">编辑并升版</button>
      </div>
      <section v-if="audits.length" class="detail-section" style="margin-top: 14px">
        <h3>操作记录</h3>
        <div class="timeline">
          <div v-for="a in audits" :key="a.id">
            <i />
            <span>
              <strong>{{ a.action }}</strong>
              <small>{{ fmtTime(a.createdAt) }} {{ a.detail }}</small>
            </span>
          </div>
        </div>
      </section>
    </TDrawer>
  </div>
</template>
