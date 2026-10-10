<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  Download, Plus, Search, Archive, Layers3, Check, ChevronDown, ChevronRight,
  ListChecks, Sparkles
} from 'lucide-vue-next'
import Badge from '../components/ui/Badge.vue'
import Empty from '../components/ui/Empty.vue'
import TPagination from '../components/ui/TPagination.vue'
import TDrawer from '../components/ui/TDrawer.vue'
import TModal from '../components/ui/TModal.vue'
import {
  apiCases, apiCaseTree, apiCaseModules, apiCaseCreate, apiCaseUpdate, apiCaseCopy,
  apiCaseGenerateDrafts, apiAudit
} from '../api'
import { fmtTime, fmtDate } from '../utils/format'

const router = useRouter()
const tab = ref('library')
const query = ref({ keyword: '', priority: '', status: '', moduleName: '', submoduleName: '', page: 1, size: 20 })
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
  title: '', priority: 'P1', type: '功能', preconditions: '', steps: '', expected: '', testData: '', linkedBugNo: ''
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
    title: row.title, priority: row.priority, type: row.type,
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

const exportCases = (format, items) => {
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
  const sheet = XLSX.utils.json_to_sheet(data)
  const filename = `用例库_${list.length}条`
  if (format === 'xlsx') {
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '用例库')
    XLSX.writeFile(wb, `${filename}.xlsx`)
  } else if (format === 'csv') {
    downloadBlob(new Blob(['\ufeff' + XLSX.utils.sheet_to_csv(sheet)], { type: 'text/csv;charset=utf-8' }), `${filename}.csv`)
  } else {
    downloadBlob(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' }), `${filename}.json`)
  }
  ElMessage.success(`已导出 ${list.length} 条用例（${format.toUpperCase()}）`)
}

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

const bulkCreateRun = () => ElMessage.success('已创建用例执行任务，执行结果会按批次归档到「执行记录」')
const bulkJointAnalysis = () => ElMessage.success('已带入 Bug 联合分析')

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
      <div class="page-actions">
        <button class="button" @click="exportCases('xlsx')"><Download />导出 XLSX</button>
        <button class="button primary" @click="openCreate"><Plus />新增用例</button>
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
        <button @click="bulkCreateRun">创建执行</button>
        <button @click="bulkJointAnalysis">联合分析</button>
        <button @click="router.push('/regression')">生成回归</button>
        <button @click="router.push('/reports')">生成报告</button>
        <button @click="exportCases('csv', selection)">下载 CSV</button>
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
            <button class="icon-button" title="备份 JSON" @click="exportCases('json')"><Archive /></button>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style="width: 36px">
                    <input type="checkbox" :checked="allSelected" @click.prevent="toggleAll">
                  </th>
                  <th>用例</th>
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
                  <td>
                    <strong>{{ c.title }}</strong>
                    <small class="id">{{ c.caseNo }}</small>
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
                  <td colspan="9" style="text-align: center; color: #9a9e94; cursor: default">没有匹配的用例</td>
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

    <section v-else class="panel">
      <Empty title="暂无独立执行批次" desc="勾选用例后点击「创建执行」，执行结果会在这里按批次归档。" />
    </section>

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
