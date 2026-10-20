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

    <!-- 向量检索状态条 -->
    <div class="panel" style="margin-top: 14px; display: flex; align-items: center; gap: 14px; flex-wrap: wrap; padding: 12px 18px">
      <span class="badge" :class="embedStatus.enabled ? 'green' : 'gray'">
        {{ embedStatus.enabled ? '向量检索已启用' : '向量检索未配置' }}
      </span>
      <template v-if="embedStatus.enabled">
        <span style="font-size: 12px">模型 {{ embedStatus.currentModel || '—' }}</span>
        <span style="font-size: 12px">已嵌入 {{ embedStatus.ready || 0 }}/{{ embedStatus.total || 0 }}</span>
        <span v-if="embedStatus.pending" style="font-size: 12px; color: #b45309">待嵌入 {{ embedStatus.pending }}</span>
        <span v-if="embedStatus.staleVersion" style="font-size: 12px; color: #b45309">版本过期 {{ embedStatus.staleVersion }}</span>
        <span v-if="(embedStatus.staleModels || []).length" style="font-size: 12px; color: #b45309">
          旧模型向量：{{ embedStatus.staleModels.join('、') }}
        </span>
        <button class="button" :disabled="reembedding" @click="reembedAll">
          {{ reembedding ? '重嵌中…' : '全量重嵌' }}
        </button>
      </template>
      <span v-else style="font-size: 12px; color: var(--muted)">
        到「配置中心 → 大模型」新增用途为 embedding 的模型配置即启用；未配置时自动降级为关键词检索。
      </span>
    </div>

    <!-- 筛选 -->
    <div class="filterbar" style="margin-top: 18px">
      <div class="search">
        <Search />
        <input v-model="query.keyword" placeholder="语义检索：输入自然语言或故障描述后回车" @keyup.enter="search" />
      </div>
      <select v-model="searchMode">
        <option value="semantic">语义检索（三级降级）</option>
        <option value="keyword">关键词检索</option>
      </select>
      <select v-model="query.category" @change="filterList">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
      </select>
      <select v-model="query.status" @change="filterList">
        <option value="">全部状态</option>
        <option v-for="s in ['待审核', '已发布', '已驳回', '已撤销']" :key="s" :value="s">{{ s }}</option>
      </select>
      <select v-model="query.moduleName" @change="filterList">
        <option value="">全部模块</option>
        <option v-for="m in moduleNames" :key="m" :value="m">{{ m }}</option>
      </select>
    </div>

    <!-- 列表 -->
    <div class="panel table-panel" style="margin-top: 14px">
      <div v-if="searchActive" style="display: flex; align-items: center; gap: 10px; margin-bottom: 12px">
        <span class="badge green">{{ hitMethodLabel }}</span>
        <span style="font-size: 12px; color: var(--muted)">
          命中 {{ hitResult.list.length }} 条 · 仅检索已发布知识 · 语义命中阈值 0.30
        </span>
        <button class="text-button" @click="clearHits">清除检索</button>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>知识标题</th><th>分类</th>
              <th v-if="!searchActive">模块</th>
              <th>风险</th><th>版本</th>
              <th v-if="!searchActive">状态</th>
              <th v-if="searchActive">匹配方式</th>
              <th v-if="searchActive">相关度</th>
              <th v-if="!searchActive">更新时间</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="k in pagedRows" :key="k.id" @click="openDetail(k)">
              <td><strong>{{ k.title }}</strong><small>{{ k.kbNo }}</small></td>
              <td>{{ k.category }}</td>
              <td v-if="!searchActive">{{ [k.moduleName, k.submoduleName].filter(Boolean).join(' / ') || '通用' }}</td>
              <td><Badge :value="k.risk" /></td>
              <td><span class="wb-id">v{{ k.version }}</span></td>
              <td v-if="!searchActive"><Badge :value="k.status" /></td>
              <td v-if="searchActive"><span class="badge">{{ k.method === 'semantic' ? '语义检索' : '关键词检索' }}</span></td>
              <td v-if="searchActive">{{ k.score }}</td>
              <td v-if="!searchActive">{{ fmtDate(k.updatedAt) }}</td>
              <td><button class="text-button" @click.stop="openDetail(k)">查看 <ChevronRight /></button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <Empty
        v-if="!loading && !pagedRows.length"
        :title="searchActive ? '未检索到相关知识（未虚构）' : '暂无知识条目'"
        :desc="searchActive
          ? '换一种描述或切换关键词检索；任务执行时未命中同样会明确标记，不做虚构'
          : '新增知识或从分析任务申请入库'"
      />
      <TPagination v-if="!searchActive && allRows.length" v-model:page="page" v-model:page-size="pageSize" :total="allRows.length" />
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
          <strong>{{ detail.kb.refCount }}（硬引用 · 撤销将被阻止）</strong>
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
        <h3>检索命中（软引用）</h3>
        <template v-if="impact">
          <p style="font-size: 12px; color: var(--muted)">
            历史累计命中 {{ impact.softHitCount }} 次；软引用可追溯、不阻止撤销。
          </p>
          <div v-if="impact.recentHits && impact.recentHits.length" class="timeline">
            <div v-for="(h, i) in impact.recentHits" :key="i">
              <i></i>
              <span>
                <strong>{{ h.runNo }} · {{ h.method === 'semantic' ? '语义检索' : '关键词检索' }} {{ h.score }}</strong>
                <small>{{ h.runTitle }} · 命中版本 v{{ h.version }} · {{ fmtTime(h.createdAt) }}</small>
              </span>
            </div>
          </div>
          <p v-else style="font-size: 12px; color: var(--muted)">暂无检索命中记录</p>
        </template>
        <p v-else style="font-size: 12px; color: var(--muted)">加载中…</p>
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
  apiKbSearch, apiKbImpact, apiKbEmbeddingStatus, apiKbReembed,
  apiAudit, apiCaseModules
} from '../api'
import { fmtTime, fmtDate, fmtSlash } from '../utils/format'
import Badge from '../components/ui/Badge.vue'
import Empty from '../components/ui/Empty.vue'
import TDrawer from '../components/ui/TDrawer.vue'
import TModal from '../components/ui/TModal.vue'
import TPagination from '../components/ui/TPagination.vue'

const query = ref({ keyword: '', category: '', status: '', moduleName: '' })
const searchMode = ref('semantic')
const hitResult = ref(null)
const allRows = ref([])
const stats = ref({})
const embedStatus = ref({})
const reembedding = ref(false)
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
const impact = ref(null)

const searchActive = computed(() => !!hitResult.value)
const hitMethodLabel = computed(() => {
  const m = hitResult.value && hitResult.value.method
  return m === 'semantic' ? '语义检索' : m === 'keyword' ? '关键词检索' : '未命中'
})
const displayRows = computed(() => (searchActive.value ? (hitResult.value.list || []) : allRows.value))
const pagedRows = computed(() => displayRows.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const moduleText = (k) => [k.projectName, k.moduleName, k.submoduleName].filter(Boolean).join(' / ')

/** 搜索框回车：有关键字走三级降级检索，无关键字回到列表 */
const search = async () => {
  page.value = 1
  const kw = query.value.keyword.trim()
  if (!kw) {
    hitResult.value = null
    await load()
    return
  }
  loading.value = true
  try {
    hitResult.value = await apiKbSearch({ q: kw, mode: searchMode.value })
  } finally {
    loading.value = false
  }
}

/** 下拉筛选变化：退出检索视图，回到列表筛选 */
const filterList = async () => {
  hitResult.value = null
  page.value = 1
  await load()
}

/** 清除检索：清空关键字回到列表 */
const clearHits = async () => {
  hitResult.value = null
  query.value.keyword = ''
  page.value = 1
  await load()
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

const loadEmbedStatus = async () => {
  embedStatus.value = await apiKbEmbeddingStatus()
}

const reembedAll = async () => {
  reembedding.value = true
  try {
    const r = await apiKbReembed()
    ElMessage.success(`重嵌完成：成功 ${r.success} / 共 ${r.total}`)
    await loadEmbedStatus()
  } finally {
    reembedding.value = false
  }
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
  impact.value = null
  detail.value = await apiKbDetail(row.id)
  detailVisible.value = true
  audits.value = await apiAudit({ entityType: 'kb', entityNo: detail.value.kb.kbNo })
  try {
    impact.value = await apiKbImpact(row.id)
  } catch (e) {
    impact.value = null
  }
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
  await loadEmbedStatus()
})
</script>
