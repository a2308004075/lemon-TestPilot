<template>
  <div class="page">
    <h2 class="page-title">AI 测试助手</h2>
    <p class="page-desc">粘贴一段问题材料，自动识别主任务与辅助任务，检查输入完整度，按流程逐步执行并等待人工复核。</p>

    <!-- 任务入口卡 -->
    <el-card shadow="never" class="mb12">
      <div class="card-title"><el-icon color="#e6a23c"><Grid /></el-icon>任务入口（可手动勾选）</div>
      <el-row :gutter="10">
        <el-col v-for="e in entries" :key="e.taskType" :span="6" class="entry-col">
          <div class="entry-card" :class="{ active: selected.includes(e.taskType) }" @click="toggle(e.taskType)">
            <div class="entry-head">
              <el-checkbox :model-value="selected.includes(e.taskType)" @change="toggle(e.taskType)" @click.stop />
              <span class="entry-name">{{ e.name }}</span>
            </div>
            <div class="entry-words">{{ (e.triggerWords || []).slice(0, 5).join(' / ') }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 智能输入 -->
    <el-card shadow="never" class="mb12">
      <div class="card-title"><el-icon color="#e6a23c"><EditPen /></el-icon>输入材料</div>
      <el-form label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="所属模块">
              <el-cascader v-model="modulePath" :options="moduleOptions" placeholder="选择项目/模块/子模块"
                           clearable style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-input v-model="inputText" type="textarea" :rows="6" maxlength="2000" show-word-limit
                placeholder="粘贴问题材料：现象、复现步骤、预期结果、traceId、日志片段、SQL、版本信息……（识别依赖关键词，越完整越好）" />
      <div class="input-actions">
        <el-button size="small" @click="loadExample">载入示例</el-button>
        <div class="spacer" />
        <el-button :loading="identifying" @click="doIdentify">识别任务与缺项</el-button>
        <el-button type="warning" class="tp-btn-primary" :loading="executing" @click="doExecute">
          按流程执行
        </el-button>
      </div>
    </el-card>

    <!-- 执行前检查面板 -->
    <el-card v-if="identifyResult" shadow="never" class="mb12">
      <div class="card-title">
        <el-icon color="#e6a23c"><CircleCheck /></el-icon>执行前检查
        <el-tag size="small" type="warning">总体完整度 {{ identifyResult.overallCompleteness }}%</el-tag>
      </div>
      <el-alert v-if="(identifyResult.missingAll || []).length" type="warning" :closable="false" class="mb12"
                :title="'存在缺项：' + identifyResult.missingAll.join('、')" description="缺项不阻塞执行：引擎会按流程输出并明确标记待补充内容。" />
      <el-table :data="identifyResult.checks || []" size="small" stripe>
        <el-table-column prop="taskName" label="任务" width="120" />
        <el-table-column label="角色" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.taskType === identifyResult.mainTask ? 'warning' : 'info'">
              {{ row.taskType === identifyResult.mainTask ? '主任务' : '辅助' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完整度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.completeness" :stroke-width="12"
                         :color="row.completeness >= 80 ? '#67c23a' : '#e6a23c'" />
          </template>
        </el-table-column>
        <el-table-column label="缺项">
          <template #default="{ row }">
            <template v-if="(row.missing || []).length">
              <el-tag v-for="m in row.missing" :key="m" size="small" type="danger" effect="plain" class="mr4">{{ m }}</el-tag>
            </template>
            <span v-else class="ok-text">无缺项</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行结果 -->
    <el-card v-if="runResult" shadow="never" class="mb12">
      <div class="card-title">
        <el-icon color="#e6a23c"><Document /></el-icon>执行结果
        <el-tag size="small">{{ runResult.run.runNo }}</el-tag>
        <el-tag size="small" :type="runResult.run.status === '已完成' ? 'success' : 'warning'">{{ runResult.run.status }}</el-tag>
        <el-tag size="small" type="info">引擎：{{ runResult.run.engineMode === 'llm' ? 'LLM' : '规则' }}</el-tag>
      </div>
      <el-row :gutter="12">
        <el-col v-for="s in runResult.snapshots || []" :key="s.seq" :span="12" class="snap-col">
          <div class="snap-card">
            <div class="snap-head">
              <span class="snap-name">{{ s.seq }}. {{ s.taskName }}</span>
              <el-tag size="small" :type="s.role === 'main' ? 'warning' : 'info'">{{ s.role === 'main' ? '主任务' : '辅助' }}</el-tag>
              <el-tag size="small" :type="s.completeness >= 80 ? 'success' : 'warning'">完整度 {{ s.completeness }}%</el-tag>
            </div>
            <div v-if="(s.missing || []).length" class="snap-missing">
              缺项：<el-tag v-for="m in s.missing" :key="m" size="small" type="danger" effect="plain">{{ m }}</el-tag>
            </div>
            <pre class="tp-json">{{ JSON.stringify(s.output, null, 2) }}</pre>
          </div>
        </el-col>
      </el-row>

      <!-- 人工复核 -->
      <el-divider content-position="left">人工复核</el-divider>
      <div class="review-bar">
        <el-input v-model="reviewNote" placeholder="复核备注（驳回时请说明需补充的内容）" style="width: 420px" />
        <el-button @click="doReview('reject')" :loading="reviewing">驳回补充</el-button>
        <el-button type="warning" class="tp-btn-primary" @click="doReview('approve')" :loading="reviewing">确认通过</el-button>
        <el-tag v-if="runResult.run.reviewStatus !== '待复核'" size="small"
                :type="runResult.run.reviewStatus === '通过' ? 'success' : 'danger'">
          当前：{{ runResult.run.reviewStatus }}
        </el-tag>
      </div>
    </el-card>

    <!-- 全局安全规则 -->
    <el-card shadow="never">
      <div class="card-title"><el-icon color="#e6a23c"><Lock /></el-icon>全局安全规则</div>
      <div class="rule-list">
        <span v-for="r in securityRules" :key="r" class="rule-item">{{ r }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  apiAssistantEntries, apiAssistantIdentify, apiAssistantExecute, apiRunReview,
  apiCaseModules
} from '../api'

const entries = ref([])
const selected = ref([])
const inputText = ref('')
const modulePath = ref([])
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

const toggle = (type) => {
  const i = selected.value.indexOf(type)
  if (i >= 0) {
    selected.value.splice(i, 1)
  } else {
    selected.value.push(type)
  }
}

const loadExample = () => {
  inputText.value = example
  ElMessage.success('已载入示例材料')
}

const buildPayload = () => {
  const payload = { inputText: inputText.value, selectedTypes: selected.value }
  if (modulePath.value && modulePath.value.length === 3) {
    payload.projectName = modulePath.value[0]
    payload.moduleName = modulePath.value[1]
    payload.submoduleName = modulePath.value[2]
  }
  return payload
}

const doIdentify = async () => {
  identifying.value = true
  try {
    identifyResult.value = await apiAssistantIdentify(buildPayload())
    ElMessage.success('识别完成')
  } finally {
    identifying.value = false
  }
}

const doExecute = async () => {
  executing.value = true
  try {
    runResult.value = await apiAssistantExecute(buildPayload())
    const run = runResult.value.run
    ElMessage.success(`执行完成：${run.routeCount} 个任务、${run.stepCount} 个步骤（${run.runNo}）`)
    if (!identifyResult.value) {
      await doIdentify()
    }
  } finally {
    executing.value = false
  }
}

const doReview = async (action) => {
  reviewing.value = true
  try {
    const run = await apiRunReview(runResult.value.run.id, { action, note: reviewNote.value })
    runResult.value.run.reviewStatus = run.reviewStatus
    runResult.value.run.status = run.status
    ElMessage.success(action === 'approve' ? '已确认通过' : '已驳回补充')
  } finally {
    reviewing.value = false
  }
}

onMounted(async () => {
  entries.value = await apiAssistantEntries()
  const mods = await apiCaseModules()
  const projectMap = {}
  for (const m of mods || []) {
    projectMap[m.project] = projectMap[m.project] || {}
    projectMap[m.project][m.module] = projectMap[m.project][m.module] || []
    projectMap[m.project][m.module].push({ value: m.submodule, label: m.submodule })
  }
  moduleOptions.value = Object.entries(projectMap).map(([p, modsOf]) => ({
    value: p,
    label: p,
    children: Object.entries(modsOf).map(([m, subs]) => ({ value: m, label: m, children: subs }))
  }))
})
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.mr4 {
  margin-right: 4px;
}

.entry-col {
  margin-bottom: 10px;
}

.entry-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px 14px;
  cursor: pointer;
  transition: all 0.15s;
  height: 100%;
}

.entry-card.active {
  border-color: var(--tp-primary);
  background: var(--tp-primary-light);
}

.entry-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.entry-name {
  font-weight: 600;
  font-size: 13px;
}

.entry-words {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
  line-height: 1.5;
}

.input-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

.spacer {
  flex: 1;
}

.ok-text {
  color: var(--tp-pass);
  font-size: 12px;
}

.snap-col {
  margin-bottom: 12px;
}

.snap-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.snap-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.snap-name {
  font-weight: 600;
}

.snap-missing {
  margin-bottom: 8px;
  font-size: 12px;
  color: #f56c6c;
}

.review-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rule-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rule-item {
  border: 1px solid var(--tp-primary);
  color: #b17b00;
  background: var(--tp-primary-light);
  border-radius: 14px;
  padding: 3px 14px;
  font-size: 12px;
}
</style>
