<template>
  <div class="page">
    <h2 class="page-title">工作台编排</h2>
    <p class="page-desc">维护 7 个标准任务入口的触发词 / 必填信息 / 知识库映射 / 工作流步骤；保存即发布新版本，历史执行保留当时快照。</p>

    <el-row :gutter="12">
      <!-- 左侧入口列表 -->
      <el-col :span="7">
        <el-card shadow="never" class="entry-list">
          <div class="card-title"><el-icon color="#e6a23c"><SetUp /></el-icon>任务入口</div>
          <div v-for="e in entries" :key="e.taskType" class="entry-item"
               :class="{ active: current && current.taskType === e.taskType }" @click="select(e)">
            <div class="entry-name">{{ e.name }}</div>
            <div class="entry-meta">
              <el-tag size="small" :type="e.published ? 'success' : 'info'" effect="plain">
                {{ e.published ? '已发布' : '未发布' }}
              </el-tag>
              <span class="entry-ver">v{{ e.version }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧配置编辑 -->
      <el-col :span="17">
        <el-card v-if="current" shadow="never">
          <div class="card-title">
            <el-icon color="#e6a23c"><Edit /></el-icon>
            {{ current.name }} · 当前 v{{ current.version }}
            <el-tag size="small" type="info">发布于 {{ fmtTime(current.publishedAt) }}</el-tag>
          </div>

          <el-form label-width="110px">
            <el-form-item label="触发词">
              <el-input v-model="edit.triggerWords" type="textarea" :rows="4"
                        placeholder="每行一个触发词；路由按命中数量与优先级决定主任务与辅助任务" />
            </el-form-item>
            <el-form-item label="必填信息">
              <el-input v-model="edit.requiredFields" type="textarea" :rows="3"
                        placeholder="每行一项；用于执行前完整度检查与缺项提示" />
            </el-form-item>
            <el-form-item label="知识库映射">
              <el-input v-model="edit.kbMappings" type="textarea" :rows="3"
                        placeholder="每行一个知识分类；引擎只引用这些分类下已发布的真实条目" />
            </el-form-item>
            <el-form-item label="工作流步骤">
              <el-input v-model="edit.workflowSteps" type="textarea" :rows="5"
                        placeholder="每行一步，严格顺序执行；建议以「校验输入」开始、以「标记人工复核」结束" />
            </el-form-item>
          </el-form>

          <el-alert type="warning" :closable="false" class="mb12"
                    title="发布规则：保存后版本号 +1 立即生效；已产生的执行记录保留发布当时的快照，不受影响。" />

          <div class="actions">
            <el-button @click="select(current)">重置</el-button>
            <el-button type="warning" class="tp-btn-primary" :loading="publishing" @click="publish">
              保存并发布新版本
            </el-button>
          </div>
        </el-card>
        <el-empty v-else description="选择左侧入口" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiWorkbenchEntries, apiWorkbenchPublish } from '../api'

const entries = ref([])
const current = ref(null)
const edit = ref({})
const publishing = ref(false)

const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 19)

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
    ElMessage.success(`已发布 v${res.version}，立即生效`)
    entries.value = await apiWorkbenchEntries()
    const updated = entries.value.find((e) => e.taskType === current.value.taskType)
    select(updated)
  } finally {
    publishing.value = false
  }
}

onMounted(async () => {
  entries.value = await apiWorkbenchEntries()
  if (entries.value.length) {
    select(entries.value[0])
  }
})
</script>

<style scoped>
.entry-list {
  min-height: 480px;
}

.entry-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.entry-item.active {
  border-color: var(--tp-primary);
  background: var(--tp-primary-light);
}

.entry-name {
  font-weight: 600;
  font-size: 14px;
}

.entry-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.entry-ver {
  color: #909399;
  font-size: 12px;
}

.mb12 {
  margin-bottom: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
