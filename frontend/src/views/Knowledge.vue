<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-intro">
      <div>
        <h2>知识库</h2>
        <p>点击任意记录查看正文、版本、来源、审核和引用关系。</p>
      </div>
      <div class="page-actions">
        <button class="button" @click="importPlaceholder"><Upload /> 批量导入</button>
        <button class="button primary" @click="openCreate"><Plus /> 新增知识</button>
      </div>
    </div>

    <!-- 统计卡 -->
    <div class="metric-grid four" style="margin-top: 4px">
      <div class="metric yellow"><span>有效知识</span><strong>{{ stats.effective || 0 }}</strong></div>
      <div class="metric warn"><span>待审核</span><strong>{{ stats.pendingReview || 0 }}</strong></div>
      <div class="metric blue"><span>分类覆盖</span><strong>{{ stats.categoryCoverage || '0/7' }}</strong></div>
      <div class="metric"><span>知识总量</span><strong>{{ stats.total || 0 }}</strong></div>
    </div>

    <!-- 筛选 -->
    <div class="filterbar" style="margin-top: 18px">
      <div class="search">
        <Search />
        <input v-model="query.keyword" placeholder="搜索标题、正文和分类" @keyup.enter="search" />
      </div>
      <select v-model="query.category" @change="search">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="query.status" @change="search">
        <option value="">全部状态</option>
        <option v-for="s in ['待审核', '已发布', '已驳回', '已撤销']" :key="s" :value="s">{{ s }}</option>
      </select>
      <select v-model="query.moduleName" @change="search">
        <option value="">全部模块</option>
        <option v-for="m in moduleNames" :key="m" :value="m">{{ m }}</option>
      </select>
    </div>

    <!-- 列表 -->
    <div class="panel table-panel" style="margin-top: 14px">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>知识标题</th><th>分类</th><th>模块</th><th>风险</th><th>版本</th><th>状态</th><th>更新时间</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="k in pagedRows" :key="k.id" @click="openDetail(k)">
              <td><strong>{{ k.title }}</strong><small>{{ k.kbNo }}</small></td>
              <td>{{ k.category }}</td>
              <td>{{ [k.moduleName, k.submoduleName].filter(Boolean).join(' / ') || '通用' }}</td>
              <td><Badge :value="k.risk" /></td>
              <td><span class="wb-id">v{{ k.version }}</span></td>
              <td><Badge :value="k.status" /></td>
              <td>{{ fmtDate(k.updatedAt) }}</td>
              <td><button class="text-button" @click.stop="openDetail(k)">查看 <ChevronRight /></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <Empty v-if="!loading && !pagedRows.length" title="暂无知识条目" desc="新增知识或从分析任务申请入库" />
      <TPagination v-if="allRows.length" v-model:page="page" v-model:page-size="pageSize" :total="allRows.length" />
    </div>

    <!-- 新增弹窗 -->
    <TModal v-if="createVisible" title="新增知识" :width="620" @close="createVisible = false">
      <div class="field">
        <span>知识标题<em>*</em></span>
        <input v-model="form.title" placeholder="如：支付状态同步链路排查手册" />
      </div>
      <div class="inline-grid">
        <div class="field">
          <span>知识分类</span>
          <select v-model="form.category">
            <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
          </select>
        </div>
        <div class="field">
          <span>风险</span>
          <select v-model="form.risk">
            <option v-for="p in ['P0', 'P1', 'P2']" :key="p" :value="p">{{ p }}</option>
          </select>
        </div>
      </div>
      <div class="field">
        <span>所属模块</span>
        <select v-model="form.modulePath">
          <option value="">通用（不挂模块）</option>
          <option v-for="o in moduleOptions" :key="o.value" :value="o.value">{{ o.value }}</option>
        </select>
      </div>
      <div class="field">
        <span>知识正文<em>*</em></span>
        <textarea v-model="form.body" style="min-height: 180px"
                  placeholder="背景 / 现象 / 排查过程 / 根因 / 解法 / 预防措施。事实留在知识库，请勿编造编号与引用。"></textarea>
      </div>
      <template #footer>
        <button class="button" @click="createVisible = false">取消</button>
        <button class="button primary" :disabled="saving" @click="save">保存并提交审核</button>
      </template>
    </TModal>

    <!-- 详情抽屉 -->
    <TDrawer v-if="detailVisible && detail" eyebrow="DETAIL VIEW" :title="detail.kb.title"
             :subtitle="detail.kb.kbNo" @close="detailVisible = false">
      <div class="detail-badges">
        <span class="badge">{{ detail.kb.category }}</span>
        <Badge :value="detail.kb.risk" />
        <Badge :value="detail.kb.status" />
        <span class="wb-id">v{{ detail.kb.version }}</span>
      </div>
      <div class="detail-grid">
        <div><span>所属模块</span><strong>{{ moduleText(detail.kb) || '通用' }}</strong></div>
        <div><span>来源任务</span><strong>{{ detail.kb.sourceTask || '—' }}</strong></div>
        <div><span>更新时间</span><strong>{{ fmtSlash(detail.kb.updatedAt) }}</strong></div>
        <div>
          <span>引用次数</span>
          <strong>{{ detail.kb.refCount }}（撤销将被阻止）</strong>
        </div>
      </div>
      <div class="detail-section">
        <h3>关联影响</h3>
        <ul v-if="detail.related.length">
          <li v-for="(r, i) in detail.related" :key="i">{{ r }}</li>
        </ul>
        <p v-else>暂无引用</p>
      </div>
      <div class="detail-section">
        <h3>知识正文</h3>
        <pre>{{ detail.kb.body }}</pre>
      </div>
      <div class="detail-section">
        <h3>审核信息</h3>
        <p>{{ detail.kb.reviewNote || '暂无审核备注' }}</p>
      </div>
      <div class="drawer-actions">
        <template v-if="detail.kb.status === '待审核'">
          <button class="button" @click="review(detail.kb, 'reject')">驳回</button>
          <button class="button primary" @click="review(detail.kb, 'approve')">审核通过并发布</button>
        </template>
        <template v-if="detail.kb.status === '已发布'">
          <button class="button" @click="editKb(detail.kb)">编辑内容</button>
          <button class="button danger" @click="revoke(detail.kb)"><RotateCcw />撤销入库</button>
        </template>
      </div>
      <div class="detail-section">
        <h3>操作记录</h3>
        <div v-if="audits.length" class="timeline">
          <div v-for="a in audits" :key="a.id">
            <i></i>
            <span>
              <strong>{{ a.action }}</strong>
              <small>{{ fmtTime(a.createdAt) }}{{ a.detail ? ' · ' + a.detail : '' }}</small>
            </span>
          </div>
        </div>
        <p v-else style="font-size: 12px; color: var(--muted)">暂无操作记录</p>
      </div>
    </TDrawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload, Search, ChevronRight, RotateCcw } from 'lucide-vue-next'
import {
  apiKbList, apiKbStats, apiKbCategories, apiKbDetail, apiKbCreate, apiKbReview, apiKbRevoke,
  apiAudit, apiCaseModules
} from '../api'
import { fmtTime, fmtDate, fmtSlash } from '../utils/format'
import Badge from '../components/ui/Badge.vue'
import Empty from '../components/ui/Empty.vue'
import TDrawer from '../components/ui/TDrawer.vue'
import TModal from '../components/ui/TModal.vue'
import TPagination from '../components/ui/TPagination.vue'

const query = ref({ keyword: '', category: '', status: '', moduleName: '' })
const allRows = ref([])
const stats = ref({})
const categories = ref([])
const moduleNames = ref([])
const moduleOptions = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)

const createVisible = ref(false)
const saving = ref(false)
const form = ref({})
const detailVisible = ref(false)
const detail = ref(null)
const audits = ref([])

const pagedRows = computed(() => allRows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const moduleText = (k) => [k.projectName, k.moduleName, k.submoduleName].filter(Boolean).join(' / ')

const search = () => {
  page.value = 1
  load()
}

const load = async () => {
  loading.value = true
  try {
    const data = await apiKbList(query.value)
    allRows.value = data.list || []
    if ((page.value - 1) * pageSize.value >= allRows.value.length) {
      page.value = 1
    }
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  stats.value = await apiKbStats()
}

const importPlaceholder = () => {
  ElMessage.info('批量导入将在后续版本提供')
}

const openCreate = () => {
  form.value = {
    title: '', category: categories.value[0] || '业务规则库', risk: 'P1',
    modulePath: moduleOptions.value[0]?.value || '', body: ''
  }
  createVisible.value = true
}

const save = async () => {
  if (!form.value.title.trim() || !form.value.body.trim()) {
    ElMessage.warning('请填写标题与正文')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: form.value.title,
      category: form.value.category,
      risk: form.value.risk,
      body: form.value.body
    }
    if (form.value.modulePath) {
      const [projectName, moduleName, submoduleName] = form.value.modulePath.split(' / ')
      payload.projectName = projectName
      payload.moduleName = moduleName || ''
      payload.submoduleName = submoduleName || ''
    }
    await apiKbCreate(payload)
    ElMessage.success('已提交审核')
    createVisible.value = false
    await load()
    await loadStats()
  } finally {
    saving.value = false
  }
}

const openDetail = async (row) => {
  detail.value = await apiKbDetail(row.id)
  detailVisible.value = true
  audits.value = await apiAudit({ entityType: 'kb', entityNo: detail.value.kb.kbNo })
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

const editKb = (kb) => {
  ElMessage.info('知识编辑将在后续版本提供，可撤销后重新入库')
}

onMounted(async () => {
  categories.value = await apiKbCategories()
  const mods = await apiCaseModules()
  const names = new Set()
  const options = []
  const seen = new Set()
  for (const m of mods || []) {
    if (m.module) names.add(m.module)
    const label = [m.project, m.module, m.submodule].filter(Boolean).join(' / ')
    if (!seen.has(label)) {
      seen.add(label)
      options.push({ value: label, label })
    }
  }
  moduleNames.value = [...names].sort()
  moduleOptions.value = options
  await load()
  await loadStats()
})
</script>
