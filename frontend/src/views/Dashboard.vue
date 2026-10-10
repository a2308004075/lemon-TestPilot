<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Sparkles, ChevronRight, ListChecks, Bug, SquareTerminal, Database, ClipboardCheck, FileBarChart } from 'lucide-vue-next'
import Badge from '../components/ui/Badge.vue'
import { apiDashboardStats } from '../api'
import { fmtDate } from '../utils/format'

const router = useRouter()
const stats = ref({})
const recent = ref([])

// 后端 statusCards key → 左侧彩色竖条色类
const cardColor = {
  pendingSupplement: 'warn',
  pendingReview: 'yellow',
  runCount: 'blue',
  kbEffective: 'blue',
  p0Cases: 'red',
  taskCount: 'dark'
}

const quicks = [
  { key: 'cases', path: '/cases', title: '生成测试用例', desc: '将需求拆解为可执行用例', icon: ListChecks },
  { key: 'bug', path: '/bug-analysis', title: 'Bug 分析', desc: '定位现象、证据和回归范围', icon: Bug },
  { key: 'logs', path: '/log-analysis', title: '日志分析', desc: '还原调用链与异常节点', icon: SquareTerminal },
  { key: 'sql', path: '/sql-analysis', title: 'SQL 分析', desc: '检查正确性、性能和一致性', icon: Database },
  { key: 'regression', path: '/regression', title: '生成回归清单', desc: '从改动与历史风险生成范围', icon: ClipboardCheck },
  { key: 'report', path: '/reports', title: '生成测试报告', desc: '汇总多来源形成质量结论', icon: FileBarChart }
]

const assetPath = { cases: '/cases', tasks: '/assistant', kb: '/knowledge', reports: '/reports' }

const rowModule = (row) => {
  if (row.moduleName) return [row.projectName, row.moduleName, row.submoduleName].filter(Boolean).slice(-2).join(' / ')
  return '—'
}

const openRow = (row) => {
  if (row.kind === 'run') return router.push('/runs')
  const map = { bug_analysis: '/bug-analysis', log_triage: '/log-analysis', sql_analysis: '/sql-analysis' }
  router.push(map[row.type] || '/assistant')
}

onMounted(async () => {
  const data = await apiDashboardStats()
  stats.value = data
  const tasks = data.recentTasks || []
  const runs = data.recentRuns || []
  recent.value = [...tasks, ...runs]
    .sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
    .slice(0, 8)
})
</script>

<template>
  <div class="page stack">
    <section class="hero">
      <div>
        <span class="kicker">GOOD MORNING · QA</span>
        <h2>今天从哪里开始？</h2>
        <p>任务、用例、知识与质量结论都在一个工作台内完成。</p>
      </div>
      <button class="button dark" @click="router.push('/assistant')"><Sparkles />创建智能任务</button>
    </section>

    <div class="metric-grid six">
      <article v-for="c in stats.statusCards || []" :key="c.key" class="metric" :class="cardColor[c.key]">
        <span>{{ c.name }}</span>
        <strong>{{ c.value }}<small style="font-size: 12px; margin-left: 3px">{{ c.unit }}</small></strong>
        <small>实时统计</small>
      </article>
    </div>

    <div class="two-col wide-left">
      <section class="panel">
        <div class="panel-title">
          <div>
            <h3>快捷入口</h3>
            <p>每项任务都内置智能输入</p>
          </div>
          <button class="text-button" @click="router.push('/assistant')">AI 测试助手 <ChevronRight /></button>
        </div>
        <div class="quick-grid">
          <button v-for="q in quicks" :key="q.key" class="quick-card" @click="router.push(q.path)">
            <span class="task-icon"><component :is="q.icon" /></span>
            <div>
              <strong>{{ q.title }}</strong>
              <span>{{ q.desc }}</span>
            </div>
            <ChevronRight />
          </button>
        </div>
      </section>
      <section class="panel">
        <div class="panel-title">
          <div>
            <h3>数据资产</h3>
            <p>MySQL 实时统计</p>
          </div>
        </div>
        <div class="asset-list">
          <button v-for="a in stats.assets || []" :key="a.key" @click="router.push(assetPath[a.key] || '/dashboard')">
            <span>{{ a.name }}</span>
            <strong>{{ a.value }}</strong>
            <ChevronRight />
          </button>
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>最近任务</h3>
          <p>点击查看详情</p>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>任务</th>
              <th>类型</th>
              <th>模块</th>
              <th>风险</th>
              <th>审核</th>
              <th>更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in recent" :key="t.kind + t.id" @click="openRow(t)">
              <td>
                <strong>{{ t.title }}</strong>
                <small class="id">{{ t.no }}</small>
              </td>
              <td>{{ t.kind === 'run' ? t.mainTaskName : t.typeName }}</td>
              <td>{{ rowModule(t) }}</td>
              <td><Badge :value="t.risk || ''" /></td>
              <td><Badge :value="t.reviewStatus || ''" /></td>
              <td>{{ fmtDate(t.updatedAt) }}</td>
            </tr>
            <tr v-if="!recent.length">
              <td colspan="6" style="text-align: center; color: #9a9e94">暂无任务，从 AI 测试助手发起第一个分析吧</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
