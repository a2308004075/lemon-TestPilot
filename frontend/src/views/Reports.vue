<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-intro">
      <div>
        <h2>测试报告</h2>
        <p>勾选用例、Bug、分析任务和回归结果，输出统一质量结论。</p>
      </div>
      <div class="page-actions">
        <button class="button primary" @click="openGenerate"><Plus /> 生成报告</button>
      </div>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'list' }" @click="tab = 'list'">报告列表</button>
      <button :class="{ active: tab === 'generate' }" @click="tab = 'generate'">生成报告</button>
      <button :class="{ active: tab === 'template' }" @click="tab = 'template'">报告模板</button>
    </div>

    <!-- 报告列表 -->
    <div v-if="tab === 'list'" class="stack" style="margin-top: 18px">
      <div class="panel table-panel">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>报告</th><th>项目 / 版本</th><th>覆盖模块</th><th>结论</th><th>失败</th><th>阻塞</th><th>报告日期</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in reports" :key="r.id" @click="viewReport(r)">
                <td><strong>{{ r.title }}</strong><small>{{ r.reportNo }}</small></td>
                <td><strong>{{ r.projectName }}</strong><small>{{ r.version }}</small></td>
                <td>{{ r.modules || '—' }}</td>
                <td><Badge :value="r.conclusion" /></td>
                <td>{{ r.failCount }}</td>
                <td>{{ r.blockCount }}</td>
                <td>{{ r.reportDate }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Empty v-if="!loading && !reports.length" title="暂无测试报告" desc="在「生成报告」勾选来源后一键聚合" />
      </div>
    </div>

    <!-- 生成报告 -->
    <div v-else-if="tab === 'generate'" class="two-col" style="margin-top: 18px">
      <div class="panel">
        <div class="panel-title">
          <div>
            <h3>报告信息</h3>
            <p>统一模板 v1.0</p>
          </div>
        </div>
        <div class="field">
          <span>报告标题<em>*</em></span>
          <input v-model="genForm.title" placeholder="如：电商平台 v2.8.4 测试报告" />
        </div>
        <div class="inline-grid">
          <div class="field">
            <span>发布版本<em>*</em></span>
            <input v-model="genForm.version" placeholder="如 v2.8.4" />
          </div>
          <div class="field">
            <span>上线结论</span>
            <select v-model="genForm.conclusion">
              <option>有条件上线</option>
              <option>可上线</option>
              <option>不可上线</option>
            </select>
          </div>
        </div>
        <button class="button primary" style="margin-top: 16px" :disabled="generating" @click="generate">
          <FileBarChart /> {{ generating ? '生成中…' : '生成统一报告' }}
        </button>
      </div>

      <div class="panel source-picker">
        <div class="panel-title">
          <div>
            <h3>选择报告来源</h3>
            <p>已选 {{ selectedCount }} 项</p>
          </div>
        </div>
        <label v-for="t in allTasks" :key="'t' + t.id">
          <input type="checkbox" :checked="genForm.sourceTaskIds.includes(t.id)"
                 @change="toggleId(genForm.sourceTaskIds, t.id)" />
          <span class="task-icon"><ListChecks /></span>
          <span>
            {{ t.title }}
            <small>{{ taskName(t.taskType) }} · {{ [t.projectName, t.moduleName, t.submoduleName].filter(Boolean).slice(-2).join(' / ') || '未分组' }}</small>
          </span>
          <Badge :value="t.risk" />
        </label>
        <label v-for="l in regLists" :key="'l' + l.id">
          <input type="checkbox" :checked="genForm.regressionListIds.includes(l.id)"
                 @change="toggleId(genForm.regressionListIds, l.id)" />
          <span class="task-icon"><ClipboardCheck /></span>
          <span>
            {{ l.title }}
            <small>{{ l.total }} 项 · {{ l.passCount }} 通过 / {{ l.failCount }} 失败 / {{ l.blockCount }} 阻塞</small>
          </span>
          <Badge :value="l.status" />
        </label>
        <label v-for="c in cases" :key="'c' + c.id">
          <input type="checkbox" :checked="genForm.caseIds.includes(c.id)"
                 @change="toggleId(genForm.caseIds, c.id)" />
          <span class="task-icon"><FileText /></span>
          <span>
            {{ c.title }}
            <small>{{ c.caseNo }} · {{ c.status }}</small>
          </span>
          <Badge :value="c.priority" />
        </label>
        <p v-if="!allTasks.length && !regLists.length && !cases.length" class="tip">暂无可选来源</p>
      </div>
    </div>

    <!-- 报告模板 -->
    <div v-else style="margin-top: 18px">
      <div class="panel">
        <div class="panel-title">
          <div>
            <h3>测试报告统一模板</h3>
            <p>当前版本 v1.0</p>
          </div>
        </div>
        <div class="warning">
          <AlertTriangle />
          <p>模板修改统一在「配置中心 → 格式模板」完成。保存后历史记录会同步新结构，新增字段显示「待补齐」，原始快照不受影响。</p>
        </div>
      </div>
    </div>

    <!-- 报告详情抽屉 -->
    <TDrawer v-if="detailVisible && detail" eyebrow="REPORT DETAIL" :title="detail.report.title"
             :subtitle="detail.report.reportNo" width-class="wb-run-drawer" @close="detailVisible = false">
      <div class="report-verdict">
        <Badge :value="detail.report.conclusion" />
        <span>上线结论</span>
        <strong>{{ detail.report.version }}</strong>
      </div>
      <div class="detail-grid">
        <div><span>报告日期</span><strong>{{ detail.report.reportDate }}</strong></div>
        <div><span>项目</span><strong>{{ detail.report.projectName }}</strong></div>
        <div><span>覆盖模块</span><strong>{{ detail.report.modules || '—' }}</strong></div>
        <div><span>数据来源</span><strong>{{ (detail.content.sources || []).length }} 项</strong></div>
      </div>
      <div class="detail-section">
        <h3>执行结果</h3>
        <div class="report-numbers">
          <span>通过<b>{{ detail.report.passCount }}</b></span>
          <span>失败<b>{{ detail.report.failCount }}</b></span>
          <span>阻塞<b>{{ detail.report.blockCount }}</b></span>
        </div>
      </div>
      <div class="detail-section">
        <h3>结论摘要</h3>
        <p>{{ detail.content.summary }}</p>
      </div>
      <div class="detail-section">
        <h3>风险与遗留问题</h3>
        <ul v-if="(detail.content.riskNotes || []).length">
          <li v-for="(r, i) in detail.content.riskNotes" :key="i">{{ r }}</li>
        </ul>
        <p v-else>无风险遗留</p>
      </div>
      <div class="missing-box">
        <ShieldAlert />
        <p>{{ detail.content.manualReview }}</p>
      </div>
      <div class="drawer-actions" style="margin-top: 16px">
        <button class="button" @click="copyReport"><Copy /> 复制</button>
        <button class="button" @click="downloadMarkdown"><Download /> Markdown</button>
        <button class="button primary" @click="printReport"><Printer /> PDF</button>
      </div>
    </TDrawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus, FileBarChart, ListChecks, ClipboardCheck, FileText, AlertTriangle, ShieldAlert, Copy, Download, Printer
} from 'lucide-vue-next'
import {
  apiReports, apiReportGenerate, apiReportDetail,
  apiTasks, apiRegressionLists, apiCases
} from '../api'
import { taskName } from '../utils/format'
import Badge from '../components/ui/Badge.vue'
import Empty from '../components/ui/Empty.vue'
import TDrawer from '../components/ui/TDrawer.vue'

const tab = ref('list')
const reports = ref([])
const loading = ref(false)
const generating = ref(false)
const emptyForm = () => ({ title: '', version: '', conclusion: '有条件上线', projectName: '电商平台', sourceTaskIds: [], regressionListIds: [], caseIds: [] })
const genForm = ref(emptyForm())
const allTasks = ref([])
const regLists = ref([])
const cases = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const selectedCount = computed(() =>
  genForm.value.sourceTaskIds.length + genForm.value.regressionListIds.length + genForm.value.caseIds.length
)

const toggleId = (arr, id) => {
  const i = arr.indexOf(id)
  if (i >= 0) {
    arr.splice(i, 1)
  } else {
    arr.push(id)
  }
}

const load = async () => {
  loading.value = true
  try {
    reports.value = await apiReports()
  } finally {
    loading.value = false
  }
}

const loadSources = async () => {
  if (allTasks.value.length || regLists.value.length || cases.value.length) return
  const [t, l, c] = await Promise.all([apiTasks({}), apiRegressionLists(), apiCases({ page: 1, size: 200 })])
  allTasks.value = t.list || []
  regLists.value = l || []
  cases.value = c.list || []
}

const openGenerate = () => {
  tab.value = 'generate'
}

watch(tab, (t) => {
  if (t === 'generate') loadSources()
})

const generate = async () => {
  if (!genForm.value.title.trim() || !genForm.value.version.trim()) {
    ElMessage.warning('请填写报告标题与发布版本')
    return
  }
  generating.value = true
  try {
    await apiReportGenerate(genForm.value)
    ElMessage.success('报告已生成')
    genForm.value = emptyForm()
    tab.value = 'list'
    await load()
  } finally {
    generating.value = false
  }
}

const viewReport = async (row) => {
  detail.value = await apiReportDetail(row.id)
  detailVisible.value = true
}

const buildMarkdown = () => {
  const r = detail.value.report
  const c = detail.value.content
  const lines = [
    `# ${r.title}`,
    '',
    `- 项目：${r.projectName}`,
    `- 版本：${r.version}`,
    `- 报告日期：${r.reportDate}`,
    `- 结论：**${r.conclusion}**（仅建议，上线需人工确认）`,
    `- 执行结果：通过 ${r.passCount} / 失败 ${r.failCount} / 阻塞 ${r.blockCount}`,
    '',
    '## 结论摘要', '', c.summary, '',
    '## 来源', ''
  ]
  for (const s of c.sources || []) {
    lines.push(`- [${s.type}] ${s.title}（风险 ${s.risk || '-'}）`)
  }
  lines.push('', '## 风险与遗留', '')
  if ((c.riskNotes || []).length) {
    for (const n of c.riskNotes) {
      lines.push(`- ${n}`)
    }
  } else {
    lines.push('- 无风险遗留')
  }
  lines.push('', `> ${c.manualReview}`)
  return lines.join('\n')
}

const copyReport = async () => {
  await navigator.clipboard.writeText(buildMarkdown())
  ElMessage.success('报告已复制（Markdown 格式）')
}

const downloadMarkdown = () => {
  const blob = new Blob([buildMarkdown()], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${detail.value.report.reportNo}.md`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('Markdown 已下载')
}

const printReport = () => {
  window.print()
}

onMounted(load)
</script>
