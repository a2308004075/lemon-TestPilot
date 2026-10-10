<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, RefreshCw, Check, AlertTriangle, ShieldCheck,
  ListChecks, SquareTerminal, Database, ClipboardCheck, FileBarChart, TestTube2
} from 'lucide-vue-next'
import Badge from '../components/ui/Badge.vue'
import TDrawer from '../components/ui/TDrawer.vue'
import {
  apiRuns, apiRunDetail, apiRunReview
} from '../api'
import { fmtSlash } from '../utils/format'

const SIZE = 20

const query = ref({ keyword: '', status: '', page: 1, size: SIZE })
const rows = ref([])
const total = ref(0)
const pages = computed(() => Math.max(1, Math.ceil(total.value / SIZE)))
const detailVisible = ref(false)
const detail = ref(null)
const reviewNote = ref('')

const routeIcon = (type) => {
  const map = {
    testcase_gen: ListChecks, bug_analysis: AlertTriangle, log_triage: SquareTerminal,
    sql_analysis: Database, regression_list: ClipboardCheck, test_report: FileBarChart
  }
  return map[type] || TestTube2
}

const run = computed(() => detail.value?.run || null)
const canReview = computed(() => run.value && run.value.reviewStatus === '待复核')

const load = async () => {
  const data = await apiRuns(query.value)
  rows.value = data.list || []
  total.value = data.total || 0
  if (query.value.page > pages.value) query.value.page = 1
}

const setPage = (p) => {
  if (p < 1 || p > pages.value) return
  query.value.page = p
  load()
}

const openDetail = async (row) => {
  detail.value = await apiRunDetail(row.id)
  reviewNote.value = ''
  detailVisible.value = true
}

const review = async (action) => {
  const r = await apiRunReview(run.value.id, { action, note: reviewNote.value })
  run.value.reviewStatus = r.reviewStatus
  run.value.status = r.status
  ElMessage.success(action === 'approve' ? '人工复核已完成' : '已驳回并标记待补充')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page stack wb-page">
    <div class="page-intro">
      <div>
        <span class="wb-kicker">TRACEABLE RUNS</span>
        <h2>执行记录</h2>
        <p>每次识别、路由、步骤、输入、证据与人工复核都可追溯。</p>
      </div>
      <button class="button" @click="load"><RefreshCw />刷新</button>
    </div>

    <div class="filterbar">
      <label class="search">
        <Search />
        <input
          v-model="query.keyword" placeholder="搜索标题或原始输入"
          @keyup.enter="query.page = 1; load()"
        >
      </label>
      <select v-model="query.status" @change="query.page = 1; load()">
        <option value="">全部状态</option>
        <option>待补充</option>
        <option>执行中</option>
        <option>已完成</option>
      </select>
      <span class="summary">共 {{ total }} 条</span>
    </div>

    <section class="panel table-panel">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>执行记录</th>
              <th>主任务</th>
              <th>路由数</th>
              <th>风险</th>
              <th>状态</th>
              <th>缺项</th>
              <th>复核</th>
              <th>更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in rows" :key="r.id" @click="openDetail(r)">
              <td>
                <strong>{{ r.title }}</strong>
                <small class="id">{{ r.runNo }}</small>
              </td>
              <td>{{ r.mainTask }}</td>
              <td>{{ r.routeCount }}</td>
              <td><Badge :value="r.risk" /></td>
              <td><Badge :value="r.status" /></td>
              <td>{{ r.missingCount || 0 }}</td>
              <td>{{ r.reviewStatus }}</td>
              <td>{{ fmtSlash(r.updatedAt) }}</td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="8" style="text-align: center; color: #9a9e94; cursor: default">没有匹配的执行记录</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination">
        <span>第 {{ query.page }} / {{ pages }} 页</span>
        <button :disabled="query.page <= 1" @click="setPage(query.page - 1)">‹</button>
        <button :disabled="query.page >= pages" @click="setPage(query.page + 1)">›</button>
      </div>
    </section>

    <TDrawer
      v-if="detailVisible && detail" eyebrow="RUN DETAIL" width-class="wb-run-drawer"
      :title="run.title || run.runNo" :subtitle="run.runNo" @close="detailVisible = false"
    >
      <section class="panel wb-result">
        <div class="wb-result-head">
          <div>
            <span class="wb-id">{{ run.runNo }}</span>
            <h3>{{ run.title }}</h3>
            <p>{{ run.routeCount }} 个路由 · {{ run.stepCount }} 个步骤 · {{ fmtSlash(run.updatedAt) }} · 引擎 {{ run.engineMode === 'llm' ? 'LLM' : '规则' }}</p>
          </div>
          <div>
            <span class="wb-status" :class="run.risk === 'P0' ? 'danger' : ''">{{ run.risk || '—' }}</span>
            <span class="wb-status" :class="run.status === '已完成' ? 'success' : ''">{{ run.status }}</span>
          </div>
        </div>
        <div class="wb-result-tabs">
          <span v-for="s in detail.snapshots || []" :key="s.seq">{{ s.role === 'main' ? '主任务' : '辅助任务' }} · {{ s.taskName }}</span>
        </div>
        <article v-for="s in detail.snapshots || []" :key="'out' + s.seq" class="wb-output">
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
            <input v-model="reviewNote" class="wb-review-note" placeholder="复核备注（驳回时请说明需补充的内容）" :disabled="!canReview">
            <button class="button" :disabled="!canReview" @click="review('reject')"><AlertTriangle />驳回补充</button>
            <button class="button primary" :disabled="!canReview" @click="review('approve')"><Check />确认通过</button>
            <span v-if="!canReview" class="wb-status" :class="run.reviewStatus === '通过' ? 'success' : 'danger'">{{ run.reviewStatus }}</span>
          </div>
        </div>
      </section>

      <section class="detail-section">
        <h3>逐步执行轨迹</h3>
        <div class="vertical-flow">
          <div v-for="s in detail.steps || []" :key="s.id || s.seq">
            <span>{{ s.seq }}</span>
            <span class="wb-mini-icon"><component :is="routeIcon(s.taskType)" /></span>
            <div>
              <strong>{{ s.stepName }}</strong>
              <small>{{ s.taskType || '公共' }} · {{ fmtSlash(s.completedAt || s.createdAt) }}</small>
            </div>
            <Badge :value="s.status === 'done' ? '已完成' : s.status" />
          </div>
        </div>
      </section>
    </TDrawer>
  </div>
</template>
