<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Play, Check, AlertTriangle, ShieldCheck, Bot, Sparkles,
  ListChecks, SquareTerminal, Database, ClipboardCheck, FileBarChart, TestTube2, FlaskConical
} from 'lucide-vue-next'
import {
  apiAssistantEntries, apiAssistantIdentify, apiAssistantExecute, apiRunReview,
  apiCaseModules
} from '../api'
import { fmtTime } from '../utils/format'

const entries = ref([])
const selected = ref([])
const inputText = ref('')
const moduleName = ref('')
const moduleOptions = ref([])
const identifying = ref(false)
const executing = ref(false)
const reviewing = ref(false)
const identifyResult = ref(null)
const runResult = ref(null)
const reviewNote = ref('')

const securityRules = [
  '不跳步骤',
  '证据不足时明确标记',
  '不编造知识标题或 Bug 编号',
  'AI 不批准上线',
  'P0/P1 与 Prompt 通过需人工复核'
]

const example = '支付成功后订单仍为待支付，staging 环境 v2.8.3。复现步骤：完成支付后等待 30 秒查询订单，实际结果仍为待支付，预期订单应为已支付。traceId=demo-1024，日志出现 Lock wait timeout。请给出 sql 排查方向和回归范围。'

const routeIcon = (type) => {
  const map = {
    testcase_gen: ListChecks, bug_analysis: AlertTriangle, log_triage: SquareTerminal,
    sql_analysis: Database, regression_list: ClipboardCheck, test_report: FileBarChart,
    prompt_test: FlaskConical
  }
  return map[type] || TestTube2
}

const run = computed(() => runResult.value?.run || null)
const canReview = computed(() => run.value && run.value.reviewStatus === '待复核')

const moduleNameOf = (type) => {
  const e = (identifyResult.value?.checks || []).find((c) => c.taskType === type)
  return e ? (e.taskType === identifyResult.value.mainTask ? '主任务' : '辅助任务') : '辅助任务'
}

const toggle = (type) => {
  const i = selected.value.indexOf(type)
  if (i >= 0) selected.value.splice(i, 1)
  else selected.value.push(type)
}

const loadExample = () => {
  inputText.value = example
  selected.value = []
  identifyResult.value = null
  runResult.value = null
  ElMessage.success('已载入支付异常示例')
}

const buildPayload = () => {
  const payload = { inputText: inputText.value, selectedTypes: selected.value }
  if (moduleName.value) {
    const parts = moduleName.value.split(' / ')
    if (parts.length === 3) {
      payload.projectName = parts[0]
      payload.moduleName = parts[1]
      payload.submoduleName = parts[2]
    }
  }
  return payload
}

const doIdentify = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请先输入待分析内容')
    return
  }
  identifying.value = true
  try {
    identifyResult.value = await apiAssistantIdentify(buildPayload())
    ElMessage.success('识别完成')
  } finally {
    identifying.value = false
  }
}

const doExecute = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请先输入待分析内容')
    return
  }
  executing.value = true
  try {
    runResult.value = await apiAssistantExecute(buildPayload())
    const r = runResult.value.run
    ElMessage.success(`已按路由顺序完成分析：${r.routeCount} 个任务、${r.stepCount} 个步骤，等待人工复核`)
    if (!identifyResult.value) await doIdentify()
  } finally {
    executing.value = false
  }
}

const doReview = async (action) => {
  reviewing.value = true
  try {
    const r = await apiRunReview(run.value.id, { action, note: reviewNote.value })
    run.value.reviewStatus = r.reviewStatus
    run.value.status = r.status
    ElMessage.success(action === 'approve' ? '人工复核已完成' : '已驳回并标记待补充')
  } finally {
    reviewing.value = false
  }
}

const ENTRY_ORDER = ['testcase_gen', 'bug_analysis', 'log_triage', 'sql_analysis', 'regression_list', 'test_report', 'prompt_test']

onMounted(async () => {
  const list = await apiAssistantEntries()
  entries.value = [...list].sort((a, b) => ENTRY_ORDER.indexOf(a.taskType) - ENTRY_ORDER.indexOf(b.taskType))
  const mods = await apiCaseModules()
  moduleOptions.value = (mods || []).map((m) => `${m.project} / ${m.module} / ${m.submodule}`)
})
</script>

<template>
  <div class="page stack wb-page">
    <div class="page-intro">
      <div>
        <span class="wb-kicker">AI TEST ASSISTANT</span>
        <h2>一句话发起完整测试分析</h2>
        <p>自动识别主任务与辅助任务，按工作流顺序执行；证据不足时只提示补充，不猜测根因。</p>
      </div>
      <button class="button" @click="loadExample"><Sparkles />载入支付异常示例</button>
    </div>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>可选任务入口</h3>
          <p>可不勾选交给系统识别，也可勾选指定要同时执行的任务</p>
        </div>
        <span class="wb-id">sequential_when_matched</span>
      </div>
      <div class="wb-entry-grid">
        <button
          v-for="e in entries" :key="e.taskType"
          :class="{ selected: selected.includes(e.taskType) }"
          @click="toggle(e.taskType)"
        >
          <span class="wb-entry-icon"><component :is="routeIcon(e.taskType)" /></span>
          <span>
            <strong>{{ e.name }}</strong>
            <small>{{ e.taskType }}</small>
          </span>
          <i><Check v-if="selected.includes(e.taskType)" /><span v-else /></i>
        </button>
      </div>
    </section>

    <div class="two-col wb-input-grid">
      <section class="panel smart-panel">
        <div class="panel-title">
          <div>
            <h3>智能输入</h3>
            <p>支持整段材料；详细字段可在识别后逐项补充</p>
          </div>
          <span class="wb-count">{{ inputText.length }} 字</span>
        </div>
        <label class="field">
          <span>项目 / 模块 / 子模块</span>
          <select v-model="moduleName">
            <option value="">未指定（交给系统识别）</option>
            <option v-for="m in moduleOptions" :key="m" :value="m">{{ m }}</option>
          </select>
        </label>
        <label class="field">
          <span>问题、需求或发布材料</span>
          <textarea
            v-model="inputText" class="large wb-textarea" maxlength="2000"
            placeholder="粘贴需求、Bug 现象、复现步骤、日志、SQL、版本改动或测试结果…"
            @input="identifyResult = null; runResult = null"
          />
        </label>
        <div class="wb-upload-note">附件入口已预留；首版先保存文本与文件名，敏感日志请脱敏后粘贴。</div>
        <div class="smart-actions">
          <button class="button" :disabled="identifying" @click="doIdentify">
            <Search :class="{ spin: identifying }" />识别任务与缺项
          </button>
          <button class="button primary" :disabled="executing || !inputText.trim()" @click="doExecute">
            <Play :class="{ spin: executing }" />按流程执行
          </button>
        </div>
      </section>

      <aside class="panel wb-preflight">
        <div class="panel-title">
          <div>
            <h3>执行前检查</h3>
            <p>{{ identifyResult ? '识别结果已更新' : '等待输入材料' }}</p>
          </div>
          <span v-if="identifyResult" class="wb-score">{{ (identifyResult.missingAll || []).length ? '需补充' : '可执行' }}</span>
        </div>
        <div v-if="!identifyResult" class="wb-empty">
          <Bot />
          <strong>系统会在这里解释为什么匹配</strong>
          <p>主任务只保留一个，其他命中项作为辅助任务依次执行。</p>
        </div>
        <template v-else>
          <div class="wb-route-list">
            <div v-for="(c, i) in identifyResult.checks || []" :key="c.taskType">
              <b>{{ i + 1 }}</b>
              <span class="wb-entry-icon"><component :is="routeIcon(c.taskType)" /></span>
              <span>
                <strong>{{ c.taskName }}</strong>
                <small>{{ moduleNameOf(c.taskType) }} · {{ c.taskType }}</small>
              </span>
              <em>{{ c.completeness !== undefined && c.completeness !== null ? c.completeness + '%' : '已识别' }}</em>
            </div>
          </div>
          <div v-if="(identifyResult.missingAll || []).length" class="missing-box">
            <AlertTriangle />
            <div>
              <strong>缺少必要信息</strong>
              <p>{{ identifyResult.missingAll.join('、') }}。缺项不阻塞执行：引擎会按流程输出并明确标记待补充内容，补齐后可重新执行。</p>
            </div>
          </div>
          <div v-else class="wb-ok">
            <ShieldCheck />
            <span>
              <strong>必要信息检查通过</strong>
              <small>结论仍需按风险等级进行人工复核</small>
            </span>
          </div>
        </template>
      </aside>
    </div>

    <section v-if="run" class="panel wb-result">
      <div class="wb-result-head">
        <div>
          <span class="wb-id">{{ run.runNo }}</span>
          <h3>{{ run.title || '执行结果' }}</h3>
          <p>{{ run.routeCount }} 个路由 · {{ run.stepCount }} 个步骤 · {{ fmtTime(run.updatedAt) }} · 引擎 {{ run.engineMode === 'llm' ? 'LLM' : '规则' }}</p>
        </div>
        <div>
          <span class="wb-status" :class="run.risk === 'P0' ? 'danger' : ''">{{ run.risk || '—' }}</span>
          <span class="wb-status" :class="run.status === '已完成' ? 'success' : ''">{{ run.status }}</span>
        </div>
      </div>
      <div class="wb-result-tabs">
        <span v-for="s in runResult.snapshots || []" :key="s.seq">{{ s.role === 'main' ? '主任务' : '辅助任务' }} · {{ s.taskName }}</span>
      </div>
      <article v-for="s in runResult.snapshots || []" :key="'out' + s.seq" class="wb-output">
        <header>
          <span class="wb-entry-icon"><component :is="routeIcon(s.taskType)" /></span>
          <div>
            <strong>{{ s.taskName }}</strong>
            <small>引用范围：暂无知识事实引用 · 完整度 {{ s.completeness }}%</small>
          </div>
        </header>
        <pre>{{ JSON.stringify(s.output, null, 2) }}</pre>
      </article>
      <div class="wb-review">
        <div>
          <ShieldCheck />
          <span>
            <strong>人工复核</strong>
            <small>确认任务识别、证据、风险与结构化输出。AI 不做最终上线批准。</small>
          </span>
        </div>
        <div style="flex: 1; justify-content: flex-end">
          <input v-model="reviewNote" class="wb-review-note" placeholder="复核备注（驳回时请说明需补充的内容）" :disabled="!canReview" />
          <button class="button" :disabled="!canReview || reviewing" @click="doReview('reject')"><AlertTriangle />驳回补充</button>
          <button class="button primary" :disabled="!canReview || reviewing" @click="doReview('approve')"><Check />确认通过</button>
          <span v-if="!canReview" class="wb-status" :class="run.reviewStatus === '通过' ? 'success' : 'danger'">{{ run.reviewStatus }}</span>
        </div>
      </div>
    </section>

    <section class="panel wb-rule-strip">
      <strong>全局安全规则</strong>
      <span v-for="r in securityRules" :key="r"><Check />{{ r }}</span>
    </section>
  </div>
</template>
