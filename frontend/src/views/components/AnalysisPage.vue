<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-intro">
      <div>
        <h2>{{ cfg.title }}</h2>
        <p>{{ cfg.desc }}</p>
      </div>
      <div class="page-actions">
        <button class="button primary" @click="tab = 'create'"><Plus /> 新建分析</button>
      </div>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'create' }" @click="tab = 'create'">新建分析</button>
      <button :class="{ active: tab === 'tasks' }" @click="tab = 'tasks'">任务列表</button>
      <button :class="{ active: tab === 'kb' }" @click="tab = 'kb'">已入库经验</button>
    </div>

    <!-- 新建分析 -->
    <div v-if="tab === 'create'" class="two-col" style="margin-top: 18px">
      <div class="panel">
        <div class="panel-title">
          <div>
            <h3>{{ cfg.formTitle }}</h3>
            <p>支持粘贴文本，也可逐项补充</p>
          </div>
        </div>
        <div class="field">
          <span>任务标题<em>*</em></span>
          <input v-model="form.title" :placeholder="cfg.titlePlaceholder" />
        </div>
        <div class="inline-grid">
          <div class="field">
            <span>模块</span>
            <select v-model="modulePath">
              <option value="">未选择</option>
              <option v-for="o in moduleOptions" :key="o.value" :value="o.value">{{ o.value }}</option>
            </select>
          </div>
          <div class="field">
            <span>风险</span>
            <select v-model="form.risk">
              <option value="P0">P0</option>
              <option value="P1">P1</option>
              <option value="P2">P2</option>
            </select>
          </div>
        </div>
        <div class="field">
          <span>问题上下文<em>*</em></span>
          <textarea v-model="form.context" class="large" :placeholder="cfg.placeholder"></textarea>
        </div>
        <div class="page-actions" style="margin-bottom: 14px">
          <button class="button primary" :disabled="submitting" @click="submit">
            <Sparkles /> {{ submitting ? '分析中…' : '开始分析' }}
          </button>
          <label class="button subtle file-button">
            <Paperclip /> 添加附件
            <input type="file" @change="onFilePicked" />
          </label>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <div>
            <h3>输入完整度</h3>
            <p>不会编造缺失内容</p>
          </div>
        </div>
        <div class="score"><strong>{{ completeness }}</strong><span>%</span><small style="font-size: 12px; color: var(--muted); margin-left: 8px">当前输入完整度</small></div>
        <div class="progress"><i :style="{ width: completeness + '%' }"></i></div>
        <div class="check-list">
          <template v-if="!form.context || !form.context.trim()">
            <div v-for="h in cfg.requiredHints" :key="h">
              <span><AlertTriangle /></span>
              <div>{{ h }}<small>建议补充</small></div>
            </div>
          </template>
          <template v-else-if="missingList.length">
            <div v-for="m in missingList" :key="m">
              <span><AlertTriangle /></span>
              <div>{{ m }}<small>建议补充</small></div>
            </div>
          </template>
          <div v-else>
            <span class="done"><Check /></span>
            <div>输入完整<small>可直接发起{{ cfg.shortName }}</small></div>
          </div>
        </div>
        <div class="result-preview">
          <h4>输出结构</h4>
          <div v-for="f in cfg.outputFields" :key="f"><Check /> {{ f }}</div>
        </div>
      </div>
    </div>

    <!-- 任务列表 -->
    <div v-else-if="tab === 'tasks'" class="stack" style="margin-top: 18px">
      <div class="filterbar">
        <div class="search">
          <Search />
          <input v-model="taskQuery.keyword" placeholder="搜索标题或上下文" @keyup.enter="loadTasks" />
          <button v-if="taskQuery.keyword" @click="clearKeyword"><X /></button>
        </div>
        <span class="summary">共 {{ tasks.length }} 项</span>
      </div>
      <div class="panel table-panel">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>任务</th><th>模块</th><th>风险</th><th>状态</th><th>模型</th><th>审核</th><th>知识</th><th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in tasks" :key="t.id" @click="viewTask(t)">
                <td><strong>{{ t.title }}</strong><small>{{ t.taskNo }}</small></td>
                <td>{{ moduleText(t) || '未分组' }}</td>
                <td><Badge :value="t.risk" /></td>
                <td><Badge :value="t.status" /></td>
                <td>{{ t.engineMode === 'llm' ? 'LLM' : '规则' }}</td>
                <td><Badge :value="t.reviewStatus" /></td>
                <td><Badge :value="t.knowledgeStatus || '未入库'" /></td>
                <td>{{ fmtDate(t.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Empty v-if="!tasksLoading && !tasks.length" title="暂无分析任务" desc="在「新建分析」发起第一个任务" />
      </div>
    </div>

    <!-- 已入库经验 -->
    <div v-else style="margin-top: 18px">
      <div v-if="kbItems.length" class="knowledge-cards">
        <button v-for="k in kbItems" :key="k.id" @click="router.push('/knowledge')">
          <div class="kb-badges">
            <Badge :value="k.category" />
            <Badge :value="k.risk" />
          </div>
          <h3>{{ k.title }}</h3>
          <p>{{ (k.body || '').slice(0, 90) }}</p>
          <footer>
            <span>{{ k.moduleName || k.sourceTask }}</span>
            <ChevronRight />
          </footer>
        </button>
      </div>
      <Empty v-else title="暂无入库经验" desc="专项任务复核通过后可申请入知识库" />
    </div>

    <!-- 任务详情抽屉 -->
    <TDrawer v-if="detailVisible && detail" eyebrow="TASK DETAIL" :title="detail.task.title"
             :subtitle="detail.task.taskNo" @close="detailVisible = false">
      <div class="detail-badges">
        <Badge :value="detail.task.risk" />
        <Badge :value="detail.task.status" />
        <Badge :value="detail.task.reviewStatus" />
        <Badge :value="detail.task.knowledgeStatus || '未入库'" />
        <span class="wb-id">{{ detail.task.engineMode === 'llm' ? 'LLM' : '规则引擎' }}</span>
      </div>
      <div class="detail-grid">
        <div><span>所属模块</span><strong>{{ moduleText(detail.task) || '未分组' }}</strong></div>
        <div><span>分析模型</span><strong>{{ detail.task.engineMode === 'llm' ? 'LLM 引擎' : '规则引擎' }}</strong></div>
        <div><span>创建时间</span><strong>{{ fmtTime(detail.task.createdAt) }}</strong></div>
        <div><span>知识状态</span><strong>{{ detail.task.knowledgeStatus || '未入库' }}</strong></div>
      </div>
      <div class="detail-section">
        <h3>问题上下文</h3>
        <pre>{{ detail.task.context }}</pre>
      </div>
      <div class="detail-section result-box">
        <h3>分析结果</h3>
        <pre v-if="detail.output">{{ JSON.stringify(detail.output, null, 2) }}</pre>
        <p v-else>暂无结构化输出</p>
      </div>
      <div class="drawer-actions">
        <template v-if="detail.task.reviewStatus === '待复核'">
          <button class="button" @click="reviewTask('reject')">驳回补充</button>
          <button class="button primary" @click="reviewTask('approve')">确认通过</button>
        </template>
        <span v-else class="wb-status" :class="detail.task.reviewStatus === '通过' ? 'success' : 'danger'">
          复核{{ detail.task.reviewStatus }}
        </span>
        <button v-if="canToKnowledge(detail.task)" class="button dark" @click="toKnowledge(detail.task)">
          <BookOpen /> 申请入知识库
        </button>
      </div>
      <div class="detail-section">
        <h3>操作记录</h3>
        <div v-if="taskAudits.length" class="timeline">
          <div v-for="a in taskAudits" :key="a.id">
            <i></i>
            <span>
              <strong>{{ a.action }}</strong>
              <small>{{ fmtTime(a.createdAt) }}{{ a.detail ? ' · ' + a.detail : '' }}</small>
            </span>
          </div>
        </div>
        <p v-else style="font-size: 12px; color: var(--muted)">暂无操作记录</p>
      </div>
    </TDrawer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus, Sparkles, Search, X, AlertTriangle, Check, Paperclip, ChevronRight, BookOpen
} from 'lucide-vue-next'
import {
  apiTaskCreate, apiTaskCompleteness, apiTasks, apiTaskDetail, apiTaskReview, apiTaskToKnowledge,
  apiKbTaskSourced, apiCaseModules, apiAudit
} from '../../api'
import { fmtTime, fmtDate } from '../../utils/format'
import Badge from '../../components/ui/Badge.vue'
import Empty from '../../components/ui/Empty.vue'
import TDrawer from '../../components/ui/TDrawer.vue'

const props = defineProps({
  cfg: { type: Object, required: true }
})

const router = useRouter()
const tab = ref('create')
const form = ref({ title: '', context: '', risk: 'P1' })
const modulePath = ref('')
const moduleOptions = ref([])
const completeness = ref(0)
const missingList = ref([])
const submitting = ref(false)

const taskQuery = ref({ keyword: '' })
const tasks = ref([])
const tasksLoading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const taskAudits = ref([])

const kbItems = ref([])

// 申请入知识库前置条件：三类专项任务 + 复核通过 + 尚未入库
const canToKnowledge = (task) => {
  if (!task) return false
  const type = task.taskType || props.cfg.taskType
  if (!['bug_analysis', 'log_triage', 'sql_analysis'].includes(type)) return false
  return task.reviewStatus === '通过' && (task.knowledgeStatus || '未入库') === '未入库'
}

const moduleText = (t) => [t.projectName, t.moduleName, t.submoduleName].filter(Boolean).join(' / ')

const toKnowledge = async (task) => {
  const saved = await apiTaskToKnowledge(task.id)
  ElMessage.success('已生成待审核知识，可在知识库中审核发布')
  if (detail.value && detail.value.task.id === task.id) {
    detail.value.task.knowledgeStatus = saved.knowledgeStatus
    taskAudits.value = await apiAudit({ entityType: 'task', entityNo: saved.taskNo })
  }
  loadTasks()
}

const onFilePicked = () => {
  ElMessage.info('演示环境：附件不参与分析，请将关键信息粘贴到上下文')
}

// 实时完整度检查（防抖）
let debounceTimer = null
watch(() => form.value.context, (val) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!val || !val.trim()) {
    completeness.value = 0
    missingList.value = []
    return
  }
  debounceTimer = setTimeout(async () => {
    try {
      const data = await apiTaskCompleteness({ taskType: props.cfg.taskType, context: val })
      completeness.value = data.completeness
      missingList.value = data.missing || []
    } catch (e) { /* 忽略实时检查错误 */ }
  }, 500)
})

const buildPayload = () => {
  const payload = {
    taskType: props.cfg.taskType,
    title: form.value.title,
    context: form.value.context,
    risk: form.value.risk
  }
  if (modulePath.value) {
    const [projectName, moduleName, submoduleName] = modulePath.value.split(' / ')
    payload.projectName = projectName
    payload.moduleName = moduleName || ''
    payload.submoduleName = submoduleName || ''
  }
  return payload
}

const submit = async () => {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写任务标题')
    return
  }
  if (!form.value.context.trim()) {
    ElMessage.warning('请填写问题上下文')
    return
  }
  submitting.value = true
  try {
    const task = await apiTaskCreate(buildPayload())
    ElMessage.success(`分析完成（${task.taskNo}）`)
    await loadTasks()
    tab.value = 'tasks'
    viewTask(task)
  } finally {
    submitting.value = false
  }
}

const reviewTask = async (action) => {
  const task = await apiTaskReview(detail.value.task.id, { action, note: '' })
  detail.value.task.reviewStatus = task.reviewStatus
  detail.value.task.status = task.status
  ElMessage.success(action === 'approve' ? '已确认通过' : '已驳回补充')
  taskAudits.value = await apiAudit({ entityType: 'task', entityNo: detail.value.task.taskNo })
  loadTasks()
}

const clearKeyword = () => {
  taskQuery.value.keyword = ''
  loadTasks()
}

const loadTasks = async () => {
  tasksLoading.value = true
  try {
    const data = await apiTasks({ type: props.cfg.taskType, keyword: taskQuery.value.keyword })
    tasks.value = data.list || []
  } finally {
    tasksLoading.value = false
  }
}

const viewTask = async (row) => {
  detail.value = await apiTaskDetail(row.id)
  detailVisible.value = true
  taskAudits.value = await apiAudit({ entityType: 'task', entityNo: detail.value.task.taskNo })
}

const loadKb = async () => {
  kbItems.value = await apiKbTaskSourced({})
}

watch(tab, (t) => {
  if (t === 'tasks' && !tasks.value.length) loadTasks()
  if (t === 'kb' && !kbItems.value.length) loadKb()
})

onMounted(async () => {
  loadTasks()
  const mods = await apiCaseModules()
  const seen = new Set()
  const options = []
  for (const m of mods || []) {
    const label = [m.project, m.module, m.submodule].filter(Boolean).join(' / ')
    if (!seen.has(label)) {
      seen.add(label)
      options.push({ value: label, label })
    }
  }
  moduleOptions.value = options
  if (options.length) modulePath.value = options[0].value
})
</script>
