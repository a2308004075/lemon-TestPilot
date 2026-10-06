<template>
  <div class="page">
    <!-- 黄色问候横幅 -->
    <div class="hero">
      <div class="hero-left">
        <div class="hero-hi">下午好，测试同学</div>
        <div class="hero-tip">一句话发起完整测试分析：识别任务 → 补充缺项 → 逐任务执行 → 人工复核</div>
      </div>
      <el-button type="warning" class="hero-btn" @click="$router.push('/assistant')">
        <el-icon style="margin-right: 4px"><MagicStick /></el-icon>
        开始智能分析
      </el-button>
    </div>

    <!-- 6 状态卡 -->
    <el-row :gutter="12" class="row-cards">
      <el-col v-for="c in stats.statusCards || []" :key="c.key" :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ c.value }}<span class="stat-unit">{{ c.unit }}</span></div>
          <div class="stat-name">{{ c.name }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 + 数据资产 -->
    <el-row :gutter="12" class="row-cards">
      <el-col :span="14">
        <el-card shadow="never">
          <div class="card-title"><el-icon color="#e6a23c"><Star /></el-icon>快捷入口</div>
          <div class="quick-grid">
            <div v-for="q in quicks" :key="q.path" class="quick-item" @click="$router.push(q.path)">
              <el-icon :size="20" color="#b17b00"><component :is="q.icon" /></el-icon>
              <span>{{ q.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <div class="card-title"><el-icon color="#e6a23c"><Coin /></el-icon>数据资产</div>
          <div class="asset-grid">
            <div v-for="a in stats.assets || []" :key="a.key" class="asset-item">
              <div class="asset-value">{{ a.value }}</div>
              <div class="asset-name">{{ a.name }}（{{ a.unit }}）</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近任务 -->
    <el-card shadow="never" class="row-cards">
      <div class="card-title"><el-icon color="#e6a23c"><Clock /></el-icon>最近任务</div>
      <el-table :data="recent" size="default" stripe>
        <el-table-column label="编号" width="150">
          <template #default="{ row }">{{ row.no }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip />
        <el-table-column label="类型" width="130">
          <template #default="{ row }">{{ row.kind === 'run' ? row.mainTaskName : row.typeName }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="tp-status" :class="row.status === '已完成' ? 'tp-status-ok' : 'tp-status-warn'">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="复核状态" width="110">
          <template #default="{ row }">
            <span class="tp-status" :class="reviewClass(row.reviewStatus)">{{ row.reviewStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small"
                       @click="$router.push(row.kind === 'run' ? '/runs' : (row.type === 'bug_analysis' ? '/bug-analysis' : row.type === 'log_triage' ? '/log-analysis' : row.type === 'sql_analysis' ? '/sql-analysis' : '/assistant'))">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiDashboardStats } from '../api'

const stats = ref({})
const recent = ref([])

const quicks = [
  { path: '/assistant', title: 'AI 测试助手', icon: 'MagicStick' },
  { path: '/bug-analysis', title: 'Bug 分析', icon: 'Warning' },
  { path: '/log-analysis', title: '日志分析', icon: 'Tickets' },
  { path: '/sql-analysis', title: 'SQL 分析', icon: 'Coin' },
  { path: '/regression', title: '生成回归清单', icon: 'RefreshRight' },
  { path: '/reports', title: '生成测试报告', icon: 'DataAnalysis' }
]

const reviewClass = (s) => s === '通过' ? 'tp-status-ok' : s === '驳回补充' ? 'tp-status-block' : 'tp-status-warn'

const fmtTime = (t) => (t || '').replace('T', ' ').slice(0, 19)

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

<style scoped>
.hero {
  background: linear-gradient(120deg, #f7c948 0%, #ffd966 100%);
  border-radius: 10px;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.hero-hi {
  color: #4a3800;
  font-size: 20px;
  font-weight: 700;
}

.hero-tip {
  color: #6b5410;
  font-size: 13px;
  margin-top: 6px;
}

.hero-btn {
  background: #4a3800;
  border-color: #4a3800;
  color: #ffe9a8;
  font-weight: 600;
}

.row-cards {
  margin-bottom: 12px;
}

.stat-card {
  text-align: center;
  border-top: 3px solid var(--tp-primary);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.stat-unit {
  font-size: 12px;
  color: #909399;
  margin-left: 2px;
}

.stat-name {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.quick-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 18px 12px;
  text-align: center;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  transition: all 0.15s;
}

.quick-item:hover {
  border-color: var(--tp-primary);
  background: var(--tp-primary-light);
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.asset-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}

.asset-value {
  font-size: 22px;
  font-weight: 700;
  color: #b17b00;
}

.asset-name {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
</style>
