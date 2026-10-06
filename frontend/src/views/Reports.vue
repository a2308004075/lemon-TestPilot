<template>
  <div class="page">
    <h2 class="page-title">测试报告</h2>
    <p class="page-desc">勾选来源（分析任务 / 回归清单 / 用例）聚合统一报告：阻塞 &gt; 0 不可上线、有失败有条件上线、全通过可上线（仅建议，AI 不批准上线）。</p>

    <div class="toolbar">
      <div class="spacer" />
      <el-button type="warning" class="tp-btn-primary" @click="openGenerate">生成报告</el-button>
    </div>

    <!-- 报告列表 -->
    <el-card shadow="never">
      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="reportNo" label="编号" width="120" />
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="modules" label="覆盖模块" min-width="140" show-overflow-tooltip />
        <el-table-column label="结论" width="110">
          <template #default="{ row }">
            <span class="tp-status" :class="conclusionClass(row.conclusion)">{{ row.conclusion }}</span>
          </template>
        </el-table-column>
        <el-table-column label="通过/失败/阻塞" width="140" align="center">
          <template #default="{ row }">
            <span class="ok">{{ row.passCount }}</span> /
            <span class="fail">{{ row.failCount }}</span> /
            <span class="block">{{ row.blockCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reportDate" label="报告日期" width="110" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewReport(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 生成报告弹窗 -->
    <el-dialog v-model="genVisible" title="生成统一报告" width="640px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="报告标题" required>
          <el-input v-model="genForm.title" placeholder="如：电商平台 v2.8.4 测试报告" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="项目">
              <el-input v-model="genForm.projectName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布版本" required>
              <el-input v-model="genForm.version" placeholder="如 v2.8.4" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="来源任务">
          <el-select v-model="genForm.sourceTaskIds" multiple filterable placeholder="勾选分析任务" style="width: 100%">
            <el-option v-for="t in allTasks" :key="t.id" :value="t.id"
                       :label="`${t.taskNo} · ${t.title}（${t.risk}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="回归清单">
          <el-select v-model="genForm.regressionListIds" multiple placeholder="勾选回归清单" style="width: 100%">
            <el-option v-for="l in regLists" :key="l.id" :value="l.id"
                       :label="`${l.title}（${l.passCount}通过/${l.failCount}失败/${l.blockCount}阻塞）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充用例">
          <el-select v-model="genForm.caseIds" multiple filterable placeholder="勾选需计入的用例" style="width: 100%">
            <el-option v-for="c in cases" :key="c.id" :value="c.id"
                       :label="`${c.caseNo} · ${c.title}（${c.status}）`" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="warning" class="tp-btn-primary" :loading="generating" @click="generate">生成报告</el-button>
      </template>
    </el-dialog>

    <!-- 报告详情 -->
    <el-drawer v-model="detailVisible" size="56%" :title="detail ? detail.report.reportNo + ' · ' + detail.report.title : ''">
      <template v-if="detail">
        <el-descriptions :column="3" border size="small" class="mb12">
          <el-descriptions-item label="版本">{{ detail.report.version }}</el-descriptions-item>
          <el-descriptions-item label="报告日期">{{ detail.report.reportDate }}</el-descriptions-item>
          <el-descriptions-item label="结论">
            <span class="tp-status" :class="conclusionClass(detail.report.conclusion)">{{ detail.report.conclusion }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="count-row mb12">
          <div class="count-card ok"><div class="num">{{ detail.report.passCount }}</div><div>通过</div></div>
          <div class="count-card fail"><div class="num">{{ detail.report.failCount }}</div><div>失败</div></div>
          <div class="count-card block"><div class="num">{{ detail.report.blockCount }}</div><div>阻塞</div></div>
        </div>

        <h4>结论摘要</h4>
        <p class="detail-text">{{ detail.content.summary }}</p>

        <h4>来源</h4>
        <el-table :data="detail.content.sources || []" size="small" stripe class="mb12">
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="risk" label="风险" width="70" align="center" />
        </el-table>

        <h4>风险与遗留</h4>
        <ul v-if="(detail.content.riskNotes || []).length" class="risk-list">
          <li v-for="(r, i) in detail.content.riskNotes" :key="i">{{ r }}</li>
        </ul>
        <p v-else class="detail-text">无风险遗留</p>

        <el-alert type="warning" :closable="false" class="mb12"
                  :title="detail.content.manualReview" />

        <div class="detail-actions">
          <el-button @click="copyReport">复制</el-button>
          <el-button @click="downloadMarkdown">下载 Markdown</el-button>
          <el-button type="warning" plain @click="printReport">PDF 下载</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  apiReports, apiReportGenerate, apiReportDetail,
  apiTasks, apiRegressionLists, apiCases
} from '../api'

const reports = ref([])
const loading = ref(false)
const genVisible = ref(false)
const generating = ref(false)
const genForm = ref({ title: '', version: '', projectName: '电商平台', sourceTaskIds: [], regressionListIds: [], caseIds: [] })
const allTasks = ref([])
const regLists = ref([])
const cases = ref([])
const detailVisible = ref(false)
const detail = ref(null)

const conclusionClass = (c) => c === '可上线' ? 'tp-status-ok' : c === '不可上线' ? 'tp-status-block' : 'tp-status-warn'

const load = async () => {
  loading.value = true
  try {
    reports.value = await apiReports()
  } finally {
    loading.value = false
  }
}

const openGenerate = async () => {
  const [t, l, c] = await Promise.all([apiTasks({}), apiRegressionLists(), apiCases({ page: 1, size: 200 })])
  allTasks.value = t.list || []
  regLists.value = l || []
  cases.value = c.list || []
  genVisible.value = true
}

const generate = async () => {
  if (!genForm.value.title.trim() || !genForm.value.version.trim()) {
    ElMessage.warning('请填写报告标题与发布版本')
    return
  }
  generating.value = true
  try {
    await apiReportGenerate(genForm.value)
    ElMessage.success('报告已生成')
    genVisible.value = false
    genForm.value = { title: '', version: '', projectName: '电商平台', sourceTaskIds: [], regressionListIds: [], caseIds: [] }
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

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.toolbar {
  display: flex;
  margin-bottom: 12px;
}

.spacer {
  flex: 1;
}

.ok { color: var(--tp-pass); font-weight: 600; }
.fail { color: var(--tp-p0); font-weight: 600; }
.block { color: var(--tp-p1); font-weight: 600; }

.count-row {
  display: flex;
  gap: 12px;
}

.count-card {
  flex: 1;
  text-align: center;
  border-radius: 8px;
  padding: 14px;
  font-size: 13px;
  border: 1px solid #ebeef5;
}

.count-card .num {
  font-size: 26px;
  font-weight: 700;
}

.count-card.ok { background: #f0f9eb; color: var(--tp-pass); }
.count-card.fail { background: #fef0f0; color: var(--tp-p0); }
.count-card.block { background: var(--tp-primary-light); color: #b17b00; }

.detail-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  background: #fafafa;
  border-radius: 6px;
  padding: 10px 12px;
}

.risk-list {
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.9;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
</style>
