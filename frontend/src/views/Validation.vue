<template>
  <div class="page">
    <h2 class="page-title">案例验证</h2>
    <p class="page-desc">用固定 walkthrough 材料验证路由识别与全局安全规则；发布工作台资产前先跑通验证案例。</p>

    <!-- 案例卡 -->
    <el-row :gutter="12">
      <el-col v-for="c in cases" :key="c.id" :span="12">
        <el-card shadow="never" class="case-card">
          <div class="case-head">
            <div class="case-title">{{ c.title }}</div>
            <el-tag size="small" :type="c.lastResult === '通过' ? 'success' : c.lastResult ? 'danger' : 'info'">
              {{ c.lastResult || '未运行' }}
            </el-tag>
          </div>
          <div class="case-no">{{ c.caseNo }}</div>

          <h4>验证材料</h4>
          <p class="case-material">{{ c.material }}</p>

          <h4>预期检查</h4>
          <div class="check-list">
            <span v-for="(chk, i) in splitLines(c.expectedChecks)" :key="i" class="check-item">{{ chk }}</span>
          </div>

          <div class="case-run-info">
            <span v-if="c.lastRunAt">最近验证：{{ fmtTime(c.lastRunAt) }}</span>
            <span v-if="c.lastRoutes">实际路由：{{ c.lastRoutes }}</span>
          </div>

          <div class="case-actions">
            <el-button type="warning" class="tp-btn-primary" size="small" :loading="runningId === c.id"
                       @click="run(c)">运行验证</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 验证结果弹层 -->
    <el-drawer v-model="resultVisible" size="62%" :title="result ? `验证结果 · ${result.caseNo} · ${result.passed ? '通过' : '未通过'}` : ''">
      <template v-if="result">
        <el-result :icon="result.passed ? 'success' : 'error'"
                   :title="result.passed ? '验证通过' : '验证未通过'"
                   :sub-title="`实际路由：${result.actualRoutes.join(' → ')}`" />

        <el-card shadow="never" class="mb12">
          <div class="card-title"><el-icon color="#e6a23c"><List /></el-icon>检查项</div>
          <el-table :data="result.checks" size="small" stripe>
            <el-table-column label="结果" width="80">
              <template #default="{ row }">
                <span class="tp-status" :class="row.passed ? 'tp-status-ok' : 'tp-status-block'">
                  {{ row.passed ? '通过' : '未通过' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="检查项" min-width="200" />
            <el-table-column prop="detail" label="说明" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-card>

        <el-card shadow="never">
          <div class="card-title"><el-icon color="#e6a23c"><Document /></el-icon>各任务输出</div>
          <div v-for="o in result.outputs" :key="o.taskType" class="out-block">
            <div class="out-head">
              <span class="out-name">{{ o.taskName }}</span>
              <el-tag size="small" :type="o.role === 'main' ? 'warning' : 'info'">
                {{ o.role === 'main' ? '主任务' : '辅助' }}
              </el-tag>
            </div>
            <pre class="tp-json">{{ JSON.stringify(o.output, null, 2) }}</pre>
          </div>
        </el-card>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiValidationCases, apiValidationRun } from '../api'

const cases = ref([])
const runningId = ref(null)
const resultVisible = ref(false)
const result = ref(null)

const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 19)

const splitLines = (text) => (text || '').split('\n').map((s) => s.trim()).filter(Boolean)

const load = async () => {
  cases.value = await apiValidationCases()
}

const run = async (c) => {
  runningId.value = c.id
  try {
    result.value = await apiValidationRun(c.id)
    resultVisible.value = true
    if (result.value.passed) {
      ElMessage.success('验证通过：路由与安全规则均符合预期')
    } else {
      ElMessage.warning('验证未通过，请查看检查项详情')
    }
    await load()
  } finally {
    runningId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.case-card {
  margin-bottom: 12px;
}

.case-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.case-title {
  font-size: 15px;
  font-weight: 600;
}

.case-no {
  color: #909399;
  font-size: 12px;
  margin: 4px 0 8px;
}

.case-material {
  background: #fafafa;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-line;
  max-height: 140px;
  overflow: auto;
}

.check-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.check-item {
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 2px 10px;
  font-size: 12px;
  color: #606266;
}

.case-run-info {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.case-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.mb12 {
  margin-bottom: 12px;
}

.out-block {
  margin-bottom: 12px;
}

.out-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.out-name {
  font-weight: 600;
}
</style>
