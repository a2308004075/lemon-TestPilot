<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Save, ChevronRight, AlertTriangle, ListChecks, SquareTerminal, Database, ClipboardCheck, FileBarChart, TestTube2 } from 'lucide-vue-next'
import Badge from '../components/ui/Badge.vue'
import { apiWorkbenchEntries, apiWorkbenchPublish } from '../api'

const entries = ref([])
const current = ref(null)
const edit = ref({})
const publishing = ref(false)

const routeIcon = (type) => {
  const map = {
    testcase_gen: ListChecks, bug_analysis: AlertTriangle, log_triage: SquareTerminal,
    sql_analysis: Database, regression_list: ClipboardCheck, test_report: FileBarChart
  }
  return map[type] || TestTube2
}

const select = (e) => {
  current.value = e
  edit.value = {
    triggerWords: (e.triggerWords || []).join('\n'),
    requiredFields: (e.requiredFields || []).join('\n'),
    kbMappings: (e.kbMappings || []).join('\n'),
    workflowSteps: (e.workflowSteps || []).join('\n')
  }
}

const publish = async () => {
  publishing.value = true
  try {
    const payload = {
      triggerWords: edit.value.triggerWords.split('\n').map((s) => s.trim()).filter(Boolean),
      requiredFields: edit.value.requiredFields.split('\n').map((s) => s.trim()).filter(Boolean),
      kbMappings: edit.value.kbMappings.split('\n').map((s) => s.trim()).filter(Boolean),
      workflowSteps: edit.value.workflowSteps.split('\n').map((s) => s.trim()).filter(Boolean)
    }
    const res = await apiWorkbenchPublish(current.value.taskType, payload)
    ElMessage.success(`已发布 ${current.value.taskType} v${res.version}`)
    entries.value = await apiWorkbenchEntries()
    select(entries.value.find((e) => e.taskType === current.value.taskType))
  } finally {
    publishing.value = false
  }
}

onMounted(async () => {
  entries.value = await apiWorkbenchEntries()
  if (entries.value.length) select(entries.value[0])
})
</script>

<template>
  <div class="page stack wb-page">
    <div class="page-intro">
      <div>
        <span class="wb-kicker">WORKBENCH ASSETS</span>
        <h2>工作台编排</h2>
        <p>事实留在知识库；工作台只维护入口、触发词、流程、引用映射与输出规则。</p>
      </div>
      <div class="page-actions">
        <button class="button primary" :disabled="!current || publishing" @click="publish"><Save />保存并发布新版本</button>
      </div>
    </div>

    <div class="wb-orchestration">
      <aside class="panel wb-asset-nav">
        <div class="panel-title">
          <div>
            <h3>{{ entries.length }} 个标准入口</h3>
            <p>点击编辑当前发布资产</p>
          </div>
        </div>
        <button v-for="e in entries" :key="e.taskType" :class="{ active: current && current.taskType === e.taskType }" @click="select(e)">
          <span class="wb-entry-icon"><component :is="routeIcon(e.taskType)" /></span>
          <span>
            <strong>{{ e.name }}</strong>
            <small>{{ e.taskType }} · v{{ e.version }}</small>
          </span>
          <ChevronRight />
        </button>
      </aside>

      <section v-if="current" class="panel wb-asset-editor">
        <div class="wb-asset-title">
          <span class="wb-entry-icon"><component :is="routeIcon(current.taskType)" /></span>
          <div>
            <span class="wb-id">{{ current.taskType }}</span>
            <h3>{{ current.name }}</h3>
          </div>
          <Badge :value="current.published ? '已发布' : '未发布'" />
        </div>
        <div class="wb-edit-grid">
          <label class="field">
            <span>触发词（每行一项）</span>
            <textarea v-model="edit.triggerWords" placeholder="每行一个触发词；路由按命中数量与优先级决定主任务与辅助任务" />
          </label>
          <label class="field">
            <span>必填信息（每行一项）</span>
            <textarea v-model="edit.requiredFields" placeholder="每行一项；用于执行前完整度检查与缺项提示" />
          </label>
          <label class="field">
            <span>知识库引用映射（每行一项）</span>
            <textarea v-model="edit.kbMappings" placeholder="每行一个知识分类；引擎只引用这些分类下已发布的真实条目" />
          </label>
          <label class="field">
            <span>工作流步骤（严格顺序）</span>
            <textarea v-model="edit.workflowSteps" class="wb-tall" placeholder="每行一步，严格顺序执行；建议以「校验输入」开始、以「标记人工复核」结束" />
          </label>
        </div>
        <div class="warning">
          <AlertTriangle />
          <p>
            <strong>发布规则</strong><br>
            知识库事实不复制到工作台；修改后版本号递增，历史执行记录保留当时快照。删除步骤前请确认不会破坏输出 Schema。
          </p>
        </div>
      </section>
    </div>
  </div>
</template>
