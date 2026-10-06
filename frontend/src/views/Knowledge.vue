<template>
  <div class="page">
    <h2 class="page-title">知识库</h2>
    <p class="page-desc">七分类经验资产：分析结论入库需审核后发布；引用计数公开可见，被引用的知识撤销入库将被阻止。</p>

    <!-- 4 统计卡 -->
    <el-row :gutter="12" class="mb12">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.effective || 0 }}</div>
          <div class="stat-name">有效知识（已发布）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.pendingReview || 0 }}</div>
          <div class="stat-name">待审核</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.categoryCoverage || '0/7' }}</div>
          <div class="stat-name">分类覆盖</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-name">知识总量</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card shadow="never" class="mb12">
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="搜索标题 / 正文" clearable style="width: 220px"
                  @keyup.enter="load" @clear="load" />
        <el-select v-model="query.category" placeholder="分类" clearable style="width: 150px" @change="load">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
          <el-option v-for="s in ['待审核', '已发布', '已驳回', '已撤销']" :key="s" :label="s" :value="s" />
        </el-select>
        <div class="spacer" />
        <el-button type="warning" class="tp-btn-primary" @click="load">查询</el-button>
        <el-button type="warning" plain @click="openCreate">新增知识</el-button>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="kbNo" label="编号" width="95" />
        <el-table-column prop="title" label="知识标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="模块" min-width="150">
          <template #default="{ row }">
            {{ [row.moduleName, row.submoduleName].filter(Boolean).join(' / ') || '通用' }}
          </template>
        </el-table-column>
        <el-table-column label="风险" width="70" align="center">
          <template #default="{ row }">
            <span class="tp-risk" :class="'tp-risk-' + row.risk">{{ row.risk }}</span>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">v{{ row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <span class="tp-status" :class="statusClass(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="被引用" width="85" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.refCount" size="small" type="warning">{{ row.refCount }} 次</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceTask" label="来源" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === '待审核'" link type="success" size="small" @click="review(row, 'approve')">通过</el-button>
            <el-button v-if="row.status === '待审核'" link type="danger" size="small" @click="review(row, 'reject')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增弹窗 -->
    <el-dialog v-model="createVisible" title="新增知识（提交审核）" width="620px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="知识标题" required>
          <el-input v-model="form.title" placeholder="如：支付状态同步链路排查手册" />
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险">
              <el-select v-model="form.risk" style="width: 100%">
                <el-option v-for="p in ['P0', 'P1', 'P2']" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="8">
            <el-form-item label="项目">
              <el-input v-model="form.projectName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模块">
              <el-input v-model="form.moduleName" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="子模块">
              <el-input v-model="form.submoduleName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="知识正文" required>
          <el-input v-model="form.body" type="textarea" :rows="8"
                    placeholder="背景 / 现象 / 排查过程 / 根因 / 解法 / 预防措施。事实留在知识库，请勿编造编号与引用。" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="warning" class="tp-btn-primary" @click="save">提交审核</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="52%" :title="detail ? detail.kb.kbNo + ' · ' + detail.kb.title : ''">
      <template v-if="detail">
        <el-descriptions :column="3" border size="small" class="mb12">
          <el-descriptions-item label="分类">{{ detail.kb.category }}</el-descriptions-item>
          <el-descriptions-item label="风险">
            <span class="tp-risk" :class="'tp-risk-' + detail.kb.risk">{{ detail.kb.risk }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="版本">v{{ detail.kb.version }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="tp-status" :class="statusClass(detail.kb.status)">{{ detail.kb.status }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="被引用">{{ detail.kb.refCount }} 次</el-descriptions-item>
          <el-descriptions-item label="来源">{{ detail.kb.sourceTask }}</el-descriptions-item>
          <el-descriptions-item label="模块" :span="3">
            {{ [detail.kb.projectName, detail.kb.moduleName, detail.kb.submoduleName].filter(Boolean).join(' / ') || '通用' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4>知识正文</h4>
        <p class="detail-text pre">{{ detail.kb.body }}</p>

        <h4>引用关系</h4>
        <ul v-if="detail.related.length" class="related-list">
          <li v-for="(r, i) in detail.related" :key="i">{{ r }}</li>
        </ul>
        <p v-else class="detail-text">暂无引用</p>

        <div class="detail-actions">
          <el-button v-if="detail.kb.status === '待审核'" type="success" plain @click="review(detail.kb, 'approve')">审核通过</el-button>
          <el-button v-if="detail.kb.status === '待审核'" type="danger" plain @click="review(detail.kb, 'reject')">驳回</el-button>
          <el-button v-if="detail.kb.status === '已发布'" @click="revoke(detail.kb)">撤销入库</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { apiKbList, apiKbStats, apiKbCategories, apiKbDetail, apiKbCreate, apiKbReview, apiKbRevoke } from '../api'

const query = ref({ keyword: '', category: '', status: '', moduleName: '' })
const rows = ref([])
const stats = ref({})
const categories = ref([])
const loading = ref(false)
const createVisible = ref(false)
const form = ref({})
const detailVisible = ref(false)
const detail = ref(null)

const statusClass = (s) => s === '已发布' ? 'tp-status-ok' : s === '已撤销' || s === '已驳回' ? 'tp-status-block' : 'tp-status-warn'

const load = async () => {
  loading.value = true
  try {
    const data = await apiKbList(query.value)
    rows.value = data.list || []
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  stats.value = await apiKbStats()
}

const openCreate = () => {
  form.value = {
    title: '', category: '业务规则库', risk: 'P1',
    projectName: '电商平台', moduleName: '', submoduleName: '', body: ''
  }
  createVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim() || !form.value.body.trim()) {
    ElMessage.warning('请填写标题与正文')
    return
  }
  await apiKbCreate(form.value)
  ElMessage.success('已提交审核')
  createVisible.value = false
  await load()
  await loadStats()
}

const openDetail = async (row) => {
  detail.value = await apiKbDetail(row.id)
  detailVisible.value = true
}

const review = async (kb, action) => {
  await apiKbReview(kb.id, { action, note: '' })
  ElMessage.success(action === 'approve' ? '已审核通过并发布' : '已驳回')
  detailVisible.value = false
  await load()
  await loadStats()
}

const revoke = async (kb) => {
  try {
    await apiKbRevoke(kb.id)
    ElMessage.success('已撤销入库')
    detailVisible.value = false
    await load()
    await loadStats()
  } catch (e) {
    // 拦截器已提示：被引用时阻止
  }
}

onMounted(async () => {
  categories.value = await apiKbCategories()
  await load()
  await loadStats()
})
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}

.stat-card {
  text-align: center;
  border-top: 3px solid var(--tp-primary);
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
}

.stat-name {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.search-bar {
  display: flex;
  gap: 8px;
  align-items: center;
}

.spacer {
  flex: 1;
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

.related-list {
  padding-left: 18px;
  color: #606266;
  font-size: 13px;
  line-height: 1.9;
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
