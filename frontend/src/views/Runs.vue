<template>
  <div class="page">
    <h2 class="page-title">执行记录</h2>
    <p class="page-desc">AI 助手每次完整执行的记录：路由任务快照、步骤明细与人工复核。</p>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="mb12">
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="搜索标题 / 输入材料" clearable style="width: 280px"
                  @keyup.enter="load" @clear="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="load">
          <el-option label="已完成" value="已完成" />
          <el-option label="待补充" value="待补充" />
        </el-select>
        <el-button type="warning" class="tp-btn-primary" @click="load">查询</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="runNo" label="执行编号" width="140" />
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip />
        <el-table-column label="主任务" width="110">
          <template #default="{ row }">{{ taskName(row.mainTask) }}</template>
        </el-table-column>
        <el-table-column label="路由" width="70" align="center">
          <template #default="{ row }">{{ row.routeCount }}</template>
        </el-table-column>
        <el-table-column label="步骤" width="70" align="center">
          <template #default="{ row }">{{ row.stepCount }}</template>
        </el-table-column>
        <el-table-column label="风险" width="70" align="center">
          <template #default="{ row }">
            <span class="tp-risk" :class="'tp-risk-' + row.risk">{{ row.risk }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缺项" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.missingCount" size="small" type="warning">{{ row.missingCount }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tp-status" :class="row.status === '已完成' ? 'tp-status-ok' : 'tp-status-warn'">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="复核" width="100">
          <template #default="{ row }">
            <span class="tp-status" :class="reviewClass(row.reviewStatus)">{{ row.reviewStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="165">
          <template #default="{ row }">{{ fmtTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total"
                       layout="total, prev, pager, next" @current-change="load" />
      </div>
    </el-card>

    <!-- 详情弹层 -->
    <el-drawer v-model="detailVisible" size="72%" :title="detailTitle" destroy-on-close>
      <template v-if="detail">
        <el-descriptions :column="3" border size="small" class="mb12">
          <el-descriptions-item label="执行编号">{{ detail.run.runNo }}</el-descriptions-item>
          <el-descriptions-item label="主任务">{{ taskName(detail.run.mainTask) }}</el-descriptions-item>
          <el-descriptions-item label="风险">
            <span class="tp-risk" :class="'tp-risk-' + detail.run.risk">{{ detail.run.risk }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.run.status }}</el-descriptions-item>
          <el-descriptions-item label="复核状态">{{ detail.run.reviewStatus }}</el-descriptions-item>
          <el-descriptions-item label="引擎">{{ detail.run.engineMode === 'llm' ? 'LLM' : '规则引擎' }}</el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" class="mb12">
          <div class="card-title">
            <el-icon color="#e6a23c"><Collection /></el-icon>路由任务快照
            <el-select v-model="snapshotFilter" size="small" style="width: 170px; margin-left: 12px" clearable
                       placeholder="按任务筛选">
              <el-option v-for="s in detail.snapshots" :key="s.taskType" :value="s.taskType" :label="s.taskName" />
            </el-select>
          </div>
          <div v-for="s in filteredSnapshots" :key="s.seq" class="snap-block">
            <div class="snap-head">
              <span class="snap-name">{{ s.seq }}. {{ s.taskName }}</span>
              <el-tag size="small" :type="s.role === 'main' ? 'warning' : 'info'">{{ s.role === 'main' ? '主任务' : '辅助' }}</el-tag>
              <el-tag size="small" :type="s.completeness >= 80 ? 'success' : 'warning'">完整度 {{ s.completeness }}%</el-tag>
              <template v-if="(s.missing || []).length">
                <el-tag v-for="m in s.missing" :key="m" size="small" type="danger" effect="plain">缺：{{ m }}</el-tag>
              </template>
            </div>
            <pre class="tp-json">{{ JSON.stringify(s.output, null, 2) }}</pre>
          </div>
        </el-card>

        <el-card shadow="never" class="mb12">
          <div class="card-title"><el-icon color="#e6a23c"><List /></el-icon>步骤明细（共 {{ detail.steps.length }} 步）</div>
          <el-table :data="detail.steps" size="small" stripe max-height="360">
            <el-table-column prop="seq" label="#" width="60" />
            <el-table-column prop="taskType" label="任务" width="110">
              <template #default="{ row }">{{ row.taskType ? taskName(row.taskType) : '公共' }}</template>
            </el-table-column>
            <el-table-column prop="stepName" label="步骤" min-width="200" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <span class="tp-status tp-status-ok">{{ row.status === 'done' ? '完成' : row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="说明" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-card>

        <el-card v-if="detail.run.reviewStatus === '待复核'" shadow="never">
          <div class="card-title"><el-icon color="#e6a23c"><User /></el-icon>人工复核</div>
          <div class="review-bar">
            <el-input v-model="reviewNote" placeholder="复核备注" style="width: 380px" />
            <el-button @click="review('reject')">驳回补充</el-button>
            <el-button type="warning" class="tp-btn-primary" @click="review('approve')">确认通过</el-button>
          </div>
        </el-card>
        <el-alert v-else :title="'该记录已复核：' + detail.run.reviewStatus" :type="detail.run.reviewStatus === '通过' ? 'success' : 'warning'"
                  :closable="false" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiRuns, apiRunDetail, apiRunReview } from '../api'

const NAMES = {
  testcase_gen: '测试用例生成', bug_analysis: 'Bug 分析', log_triage: '日志排查',
  sql_analysis: 'SQL 分析', regression_list: '回归清单', test_report: '测试报告', prompt_test: 'Prompt 测试'
}

const query = ref({ keyword: '', status: '', page: 1, size: 10 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const detailTitle = ref('')
const snapshotFilter = ref('')
const reviewNote = ref('')

const taskName = (t) => NAMES[t] || t
const reviewClass = (s) => s === '通过' ? 'tp-status-ok' : s === '驳回补充' ? 'tp-status-block' : 'tp-status-warn'
const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 19)

const filteredSnapshots = computed(() => {
  if (!detail.value) return []
  if (!snapshotFilter.value) return detail.value.snapshots
  return detail.value.snapshots.filter((s) => s.taskType === snapshotFilter.value)
})

const load = async () => {
  loading.value = true
  try {
    const data = await apiRuns(query.value)
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  detail.value = await apiRunDetail(row.id)
  detailTitle.value = `执行详情 · ${row.runNo}`
  snapshotFilter.value = ''
  reviewNote.value = ''
  detailVisible.value = true
}

const review = async (action) => {
  const run = await apiRunReview(detail.value.run.id, { action, note: reviewNote.value })
  detail.value.run.reviewStatus = run.reviewStatus
  detail.value.run.status = run.status
  ElMessage.success(action === 'approve' ? '已确认通过' : '已驳回补充')
  await load()
}

onMounted(load)
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.search-bar {
  display: flex;
  gap: 8px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.snap-block {
  margin-bottom: 14px;
}

.snap-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.snap-name {
  font-weight: 600;
}

.review-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
