<template>
  <div class="page">
    <h2 class="page-title">{{ cfg.title }}</h2>
    <p class="page-desc">{{ cfg.desc }}</p>

    <el-tabs v-model="tab">
      <!-- Tab 1: 新建分析 -->
      <el-tab-pane label="新建分析" name="create">
        <el-row :gutter="12">
          <el-col :span="16">
            <el-card shadow="never">
              <div class="card-title"><el-icon color="#e6a23c"><EditPen /></el-icon>{{ cfg.formTitle }}</div>
              <el-form label-width="90px">
                <el-form-item label="任务标题" required>
                  <el-input v-model="form.title" placeholder="一句话概括问题" />
                </el-form-item>
                <el-row :gutter="10">
                  <el-col :span="8">
                    <el-form-item label="所属模块">
                      <el-cascader v-model="modulePath" :options="moduleOptions" placeholder="项目/模块/子模块"
                                   clearable style="width: 100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="风险等级">
                      <el-select v-model="form.risk" style="width: 100%">
                        <el-option label="P0（资损/主流程）" value="P0" />
                        <el-option label="P1（功能异常）" value="P1" />
                        <el-option label="P2（体验问题）" value="P2" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-form-item label="问题上下文" required>
                  <el-input v-model="form.context" type="textarea" :rows="9"
                            :placeholder="cfg.placeholder" />
                </el-form-item>
                <el-form-item>
                  <el-button size="small" @click="loadExample">载入示例</el-button>
                  <el-button type="warning" class="tp-btn-primary" :loading="submitting" @click="submit">
                    发起{{ cfg.shortName }}
                  </el-button>
                </el-form-item>
              </el-form>

              <!-- 分析结果 -->
              <template v-if="result">
                <el-divider content-position="left">分析结果</el-divider>
                <div class="result-meta">
                  <el-tag size="small" :type="result.status === '已完成' ? 'success' : 'warning'">{{ result.status }}</el-tag>
                  <el-tag size="small" type="info">完整度 {{ result.completeness }}%</el-tag>
                  <el-tag size="small" type="info">引擎：{{ result.engineMode === 'llm' ? 'LLM' : '规则' }}</el-tag>
                  <span class="result-no">{{ result.taskNo }}</span>
                </div>
                <div v-if="missingList.length" class="result-missing">
                  缺项提示：
                  <el-tag v-for="m in missingList" :key="m" size="small" type="danger" effect="plain">{{ m }}</el-tag>
                </div>
                <pre class="tp-json">{{ JSON.stringify(resultOutput, null, 2) }}</pre>
                <div class="review-bar">
                  <el-button @click="review('reject')">驳回补充</el-button>
                  <el-button type="warning" class="tp-btn-primary" @click="review('approve')">确认通过</el-button>
                  <el-tag v-if="result.reviewStatus !== '待复核'" size="small"
                          :type="result.reviewStatus === '通过' ? 'success' : 'danger'">
                    {{ result.reviewStatus }}
                  </el-tag>
                </div>
              </template>
            </el-card>
          </el-col>

          <!-- 右侧：输入完整度 + 输出结构说明 -->
          <el-col :span="8">
            <el-card shadow="never" class="mb12">
              <div class="card-title"><el-icon color="#e6a23c"><DataLine /></el-icon>输入完整度</div>
              <el-progress :percentage="completeness" :stroke-width="14"
                           :color="completeness >= 80 ? '#67c23a' : '#e6a23c'" />
              <div class="check-missing">
                <template v-if="missingList.length">
                  <div class="missing-title">建议补充：</div>
                  <div v-for="m in missingList" :key="m" class="missing-item">
                    <el-icon color="#f56c6c"><Warning /></el-icon>{{ m }}
                  </div>
                </template>
                <div v-else-if="form.context" class="ok-text">输入已完整</div>
                <div v-else class="tip-text">输入上下文后实时检查</div>
              </div>
            </el-card>
            <el-card shadow="never">
              <div class="card-title"><el-icon color="#e6a23c"><Document /></el-icon>输出结构说明</div>
              <div v-for="f in cfg.outputFields" :key="f.name" class="field-item">
                <code class="field-name">{{ f.name }}</code>
                <span class="field-label">{{ f.label }}</span>
              </div>
              <el-alert type="info" :closable="false" class="mt8"
                        title="安全规则：证据不足时明确标记；不编造知识标题或 Bug 编号；结论需人工复核。" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- Tab 2: 任务列表 -->
      <el-tab-pane :label="cfg.shortName + '任务'" name="tasks">
        <el-card shadow="never">
          <div class="search-bar">
            <el-input v-model="taskQuery.keyword" placeholder="搜索标题 / 上下文" clearable style="width: 260px"
                      @keyup.enter="loadTasks" @clear="loadTasks" />
            <el-button type="warning" class="tp-btn-primary" @click="loadTasks">查询</el-button>
          </div>
          <el-table :data="tasks" stripe class="mt8" v-loading="tasksLoading">
            <el-table-column prop="taskNo" label="编号" width="130" />
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column label="风险" width="70" align="center">
              <template #default="{ row }">
                <span class="tp-risk" :class="'tp-risk-' + row.risk">{{ row.risk }}</span>
              </template>
            </el-table-column>
            <el-table-column label="完整度" width="150">
              <template #default="{ row }">
                <el-progress :percentage="row.completeness" :stroke-width="10"
                             :color="row.completeness >= 80 ? '#67c23a' : '#e6a23c'" />
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="tp-status" :class="row.status === '已完成' ? 'tp-status-ok' : 'tp-status-warn'">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column label="复核" width="100">
              <template #default="{ row }">
                <span class="tp-status" :class="row.reviewStatus === '通过' ? 'tp-status-ok' : row.reviewStatus === '驳回补充' ? 'tp-status-block' : 'tp-status-warn'">{{ row.reviewStatus }}</span>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="165">
              <template #default="{ row }">{{ (row.updatedAt || '').replace('T', ' ').slice(0, 19) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewTask(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- Tab 3: 已入库经验 -->
      <el-tab-pane label="已入库经验" name="kb">
        <el-card shadow="never">
          <div class="card-title"><el-icon color="#e6a23c"><Collection /></el-icon>{{ cfg.shortName }}相关已入库经验</div>
          <el-table :data="kbItems" stripe v-loading="kbLoading">
            <el-table-column prop="kbNo" label="编号" width="100" />
            <el-table-column prop="title" label="知识标题" min-width="220" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="110" />
            <el-table-column label="风险" width="70" align="center">
              <template #default="{ row }">
                <span class="tp-risk" :class="'tp-risk-' + row.risk">{{ row.risk }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" />
            <el-table-column label="被引用" width="80" align="center">
              <template #default="{ row }">{{ row.refCount }} 次</template>
            </el-table-column>
            <el-table-column prop="sourceTask" label="来源" min-width="130" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="!kbLoading && !kbItems.length" description="暂无入库经验" :image-size="80" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 任务详情弹层 -->
    <el-drawer v-model="taskDetailVisible" :title="taskDetail ? taskDetail.task.taskNo + ' · ' + taskDetail.task.title : ''" size="52%">
      <template v-if="taskDetail">
        <el-descriptions :column="3" border size="small" class="mb12">
          <el-descriptions-item label="风险">
            <span class="tp-risk" :class="'tp-risk-' + taskDetail.task.risk">{{ taskDetail.task.risk }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="完整度">{{ taskDetail.task.completeness }}%</el-descriptions-item>
          <el-descriptions-item label="状态">{{ taskDetail.task.status }}</el-descriptions-item>
          <el-descriptions-item label="复核状态">{{ taskDetail.task.reviewStatus }}</el-descriptions-item>
          <el-descriptions-item label="引擎">{{ taskDetail.task.engineMode === 'llm' ? 'LLM' : '规则' }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ taskDetail.task.source === 'manual' ? '页面发起' : 'AI 助手' }}</el-descriptions-item>
        </el-descriptions>
        <h4>问题上下文</h4>
        <p class="detail-text pre">{{ taskDetail.task.context }}</p>
        <h4>分析输出</h4>
        <pre class="tp-json">{{ JSON.stringify(taskDetail.output, null, 2) }}</pre>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  apiTaskCreate, apiTaskCompleteness, apiTasks, apiTaskDetail, apiTaskReview,
  apiKbTaskSourced, apiCaseModules
} from '../../api'

const props = defineProps({
  cfg: { type: Object, required: true }
})

const tab = ref('create')
const form = ref({ title: '', context: '', risk: 'P1' })
const modulePath = ref([])
const moduleOptions = ref([])
const completeness = ref(0)
const missingList = ref([])
const submitting = ref(false)
const result = ref(null)
const resultOutput = ref(null)

const taskQuery = ref({ keyword: '' })
const tasks = ref([])
const tasksLoading = ref(false)
const taskDetailVisible = ref(false)
const taskDetail = ref(null)

const kbItems = ref([])
const kbLoading = ref(false)

const loadExample = () => {
  form.value.title = cfg2().exampleTitle
  form.value.context = cfg2().example
  ElMessage.success('已载入示例')
}

const cfg2 = () => props.cfg

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
  if (modulePath.value && modulePath.value.length === 3) {
    payload.projectName = modulePath.value[0]
    payload.moduleName = modulePath.value[1]
    payload.submoduleName = modulePath.value[2]
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
    result.value = task
    const detail = await apiTaskDetail(task.id)
    resultOutput.value = detail.output
    missingList.value = detail.task.status === '待补充' ? missingList.value : []
    ElMessage.success(`分析完成（${task.taskNo}）`)
    loadTasks()
  } finally {
    submitting.value = false
  }
}

const review = async (action) => {
  const task = await apiTaskReview(result.value.id, { action, note: '' })
  result.value.reviewStatus = task.reviewStatus
  result.value.status = task.status
  ElMessage.success(action === 'approve' ? '已确认通过' : '已驳回补充')
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
  taskDetail.value = await apiTaskDetail(row.id)
  taskDetailVisible.value = true
}

const loadKb = async () => {
  kbLoading.value = true
  try {
    kbItems.value = await apiKbTaskSourced({})
  } finally {
    kbLoading.value = false
  }
}

watch(tab, (t) => {
  if (t === 'tasks' && !tasks.value.length) loadTasks()
  if (t === 'kb' && !kbItems.value.length) loadKb()
})

onMounted(async () => {
  loadTasks()
  const mods = await apiCaseModules()
  const projectMap = {}
  for (const m of mods || []) {
    projectMap[m.project] = projectMap[m.project] || {}
    projectMap[m.project][m.module] = projectMap[m.project][m.module] || []
    projectMap[m.project][m.module].push({ value: m.submodule, label: m.submodule })
  }
  moduleOptions.value = Object.entries(projectMap).map(([p, modsOf]) => ({
    value: p, label: p,
    children: Object.entries(modsOf).map(([m, subs]) => ({ value: m, label: m, children: subs }))
  }))
})
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.mt8 {
  margin-top: 8px;
}

.search-bar {
  display: flex;
  gap: 8px;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.result-no {
  color: #909399;
  font-size: 12px;
}

.result-missing {
  margin-bottom: 10px;
  font-size: 13px;
  color: #f56c6c;
}

.review-bar {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.check-missing {
  margin-top: 10px;
}

.missing-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 6px;
}

.missing-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #f56c6c;
  padding: 4px 8px;
  background: #fef0f0;
  border-radius: 4px;
  margin-bottom: 6px;
}

.ok-text {
  color: var(--tp-pass);
  font-size: 13px;
}

.tip-text {
  color: #909399;
  font-size: 12px;
}

.field-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: baseline;
}

.field-name {
  background: #2d3239;
  color: #ffd04b;
  border-radius: 4px;
  padding: 1px 6px;
  font-size: 12px;
}

.field-label {
  color: #606266;
  font-size: 13px;
}

.detail-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  background: #fafafa;
  border-radius: 6px;
  padding: 10px 12px;
}

.detail-text.pre {
  white-space: pre-line;
}
</style>
