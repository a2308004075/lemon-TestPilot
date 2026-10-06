<template>
  <div class="page">
    <h2 class="page-title">回归测试</h2>
    <p class="page-desc">从高风险用例（P0/失败/阻塞）与历史 Bug 自动生成回归清单，至少 6 个必回归项；逐项流转状态并自动计算进度。</p>

    <div class="toolbar">
      <div class="spacer" />
      <el-button type="warning" class="tp-btn-primary" @click="genVisible = true">生成回归清单</el-button>
    </div>

    <!-- 清单卡 -->
    <el-row :gutter="12">
      <el-col v-for="list in lists" :key="list.id" :span="12">
        <el-card shadow="never" class="list-card">
          <div class="list-head">
            <div class="list-title-area">
              <div class="list-title">{{ list.title }}</div>
              <div class="list-meta">
                <el-tag size="small" effect="plain">{{ list.version || '未指定版本' }}</el-tag>
                <el-tag size="small" :type="list.status === '已完成' ? 'success' : 'warning'">{{ list.status }}</el-tag>
                <span class="list-date">{{ list.createdDate }}</span>
              </div>
            </div>
            <el-progress type="circle" :percentage="list.progress" :width="72" :stroke-width="8"
                         :color="list.progress === 100 ? '#67c23a' : '#f7c948'">
              <template #default>
                <span class="ring-num">{{ list.progress }}%</span>
              </template>
            </el-progress>
          </div>
          <div class="list-summary">
            <span>共 {{ list.total }} 项</span>
            <span class="ok">{{ list.passCount }} 通过</span>
            <span class="fail">{{ list.failCount }} 失败</span>
            <span class="block">{{ list.blockCount }} 阻塞</span>
          </div>
          <el-table :data="list.items" size="small" stripe max-height="300">
            <el-table-column prop="seq" label="#" width="46" />
            <el-table-column prop="title" label="回归项" min-width="200" show-overflow-tooltip />
            <el-table-column label="优先级" width="70" align="center">
              <template #default="{ row }">
                <span class="tp-risk" :class="'tp-risk-' + row.priority">{{ row.priority }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="moduleLabel" label="来源" min-width="150" show-overflow-tooltip />
            <el-table-column label="状态" width="170">
              <template #default="{ row }">
                <el-select :model-value="row.status" size="small" @change="(v) => updateStatus(row, v)">
                  <el-option v-for="s in ['未执行', '通过', '失败', '阻塞']" :key="s" :label="s" :value="s" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
          <div class="list-actions">
            <el-button size="small" @click="copyList(list)">复制清单</el-button>
            <el-button size="small" type="warning" plain @click="downloadList(list)">下载 Excel</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !lists.length" description="暂无回归清单，点击右上角生成" />

    <!-- 生成弹窗 -->
    <el-dialog v-model="genVisible" title="生成回归清单" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="清单标题" required>
          <el-input v-model="genForm.title" placeholder="如：v2.8.4 支付链路回归" />
        </el-form-item>
        <el-form-item label="目标版本">
          <el-input v-model="genForm.version" placeholder="如 v2.8.4" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" title="来源：P0 用例 + 失败/阻塞用例 + 历史Bug库已发布条目；不足 6 项时以通用回归规则补足。" />
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="warning" class="tp-btn-primary" :loading="generating" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiRegressionLists, apiRegressionGenerate, apiRegressionItemStatus } from '../api'

const lists = ref([])
const loading = ref(false)
const genVisible = ref(false)
const generating = ref(false)
const genForm = ref({ title: '', version: '' })

const load = async () => {
  loading.value = true
  try {
    lists.value = await apiRegressionLists()
  } finally {
    loading.value = false
  }
}

const generate = async () => {
  if (!genForm.value.title.trim()) {
    ElMessage.warning('请填写清单标题')
    return
  }
  generating.value = true
  try {
    const created = await apiRegressionGenerate(genForm.value)
    ElMessage.success(`已生成「${created.title}」，共 ${created.total} 个回归项`)
    genVisible.value = false
    genForm.value = { title: '', version: '' }
    await load()
  } finally {
    generating.value = false
  }
}

const updateStatus = async (item, status) => {
  const updated = await apiRegressionItemStatus(item.id, status)
  const idx = lists.value.findIndex((l) => l.id === updated.id)
  if (idx >= 0) {
    lists.value[idx] = updated
  }
  ElMessage.success(`「${item.title}」→ ${status}`)
}

const copyList = async (list) => {
  const lines = [`回归清单：${list.title}（${list.version || '未指定版本'}）`, '']
  for (const it of list.items) {
    lines.push(`${it.seq}. [${it.priority}] ${it.title}（${it.moduleLabel}） —— ${it.status}`)
  }
  await navigator.clipboard.writeText(lines.join('\n'))
  ElMessage.success('清单已复制到剪贴板')
}

const downloadList = (list) => {
  const rows = [
    ['序号', '回归项', '优先级', '来源', '状态'],
    ...list.items.map((it) => [it.seq, it.title, it.priority, it.moduleLabel, it.status])
  ]
  const csv = '\ufeff' + rows.map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `回归清单_${list.title}_${list.version || 'v'}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已下载 Excel（CSV）文件')
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  margin-bottom: 12px;
}

.spacer {
  flex: 1;
}

.list-card {
  margin-bottom: 12px;
}

.list-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.list-title {
  font-size: 16px;
  font-weight: 600;
}

.list-meta {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 6px;
}

.list-date {
  color: #909399;
  font-size: 12px;
}

.ring-num {
  font-size: 13px;
  font-weight: 600;
}

.list-summary {
  display: flex;
  gap: 14px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.list-summary .ok { color: var(--tp-pass); }
.list-summary .fail { color: var(--tp-p0); }
.list-summary .block { color: var(--tp-p1); }

.list-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
