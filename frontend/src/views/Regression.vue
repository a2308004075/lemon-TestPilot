<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-intro">
      <div>
        <h2>回归测试</h2>
        <p>从高风险用例、历史 Bug 和变更模块生成可执行清单。</p>
      </div>
      <div class="page-actions">
        <button class="button" @click="downloadAll"><Download /> 下载 XLSX</button>
        <button class="button primary" @click="tab = 'generate'"><Plus /> 生成清单</button>
      </div>
    </div>

    <div class="tabs">
      <button :class="{ active: tab === 'list' }" @click="tab = 'list'">回归清单</button>
      <button :class="{ active: tab === 'generate' }" @click="tab = 'generate'">生成回归</button>
      <button :class="{ active: tab === 'done' }" @click="tab = 'done'">完成记录</button>
    </div>

    <!-- 生成回归 -->
    <div v-if="tab === 'generate'" style="margin-top: 18px; max-width: 660px">
      <div class="panel">
        <div class="panel-title">
          <h3>生成回归清单</h3>
          <p>自动汇集高风险用例与已发布 Bug 条目，一键成单</p>
        </div>
        <div class="field">
          <span>目标版本</span>
          <input v-model="genForm.version" placeholder="如 v2.8.4" />
        </div>
        <div class="field">
          <span>清单标题<em>*</em></span>
          <input v-model="genForm.title" placeholder="如：v2.8.4 支付链路回归" />
        </div>
        <div class="warning">
          <AlertTriangle />
          <p>生成来源：P0 用例 + 失败 / 阻塞用例 + 知识库 Bug 条目；不足 6 项时以通用回归规则补足。生成后逐项流转状态，进度自动计算。</p>
        </div>
        <button class="button primary full" style="margin-top: 16px" :disabled="generating" @click="generate">
          <ClipboardCheck /> {{ generating ? '生成中…' : '分析并生成' }}
        </button>
      </div>
    </div>

    <!-- 回归清单 / 完成记录 -->
    <div v-else class="stack" style="margin-top: 18px">
      <div v-for="list in currentLists" :key="list.id" class="panel regression-card">
        <div class="reg-head">
          <div>
            <div class="title-line">
              <h3>{{ list.title }}</h3>
              <Badge :value="list.status" />
            </div>
            <p>{{ list.version || '未指定版本' }} · {{ list.createdDate }} · 共 {{ list.total }} 项</p>
          </div>
          <div class="progress-ring" :style="{ '--progress': (list.progress || 0) * 3.6 + 'deg' }">
            <strong>{{ list.progress || 0 }}%</strong>
          </div>
        </div>
        <div class="progress"><i :style="{ width: (list.progress || 0) + '%' }"></i></div>
        <div class="reg-items">
          <div v-for="it in list.items" :key="it.id" class="reg-item">
            <Badge :value="it.priority" />
            <div>
              {{ it.title }}
              <small>{{ it.moduleLabel || '未分组' }} · {{ it.sourceRef || sourceLabel(it.sourceType) }}</small>
            </div>
            <select :value="it.status" @change="updateStatus(it, $event.target.value)">
              <option v-for="s in ['未执行', '通过', '失败', '阻塞']" :key="s" :value="s">{{ s }}</option>
            </select>
            <button class="text-button" @click="viewSource(it)">查看 <ChevronRight /></button>
          </div>
        </div>
        <div class="reg-footer">
          <span>{{ list.passCount }} 通过 · {{ list.failCount }} 失败 · {{ list.blockCount }} 阻塞</span>
          <button class="button" @click="copyList(list)"><Copy /> 复制清单</button>
        </div>
      </div>
      <Empty v-if="!loading && !currentLists.length"
             :title="tab === 'done' ? '暂无已完成回归' : '暂无回归清单'"
             :desc="tab === 'done' ? '所有必选项完成后，清单会自动归档到这里。' : '点击右上角「生成清单」自动汇集高风险用例与历史 Bug'" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { Download, Plus, ClipboardCheck, Copy, AlertTriangle, ChevronRight } from 'lucide-vue-next'
import { apiRegressionLists, apiRegressionGenerate, apiRegressionItemStatus } from '../api'
import Badge from '../components/ui/Badge.vue'
import Empty from '../components/ui/Empty.vue'

const tab = ref('list')
const lists = ref([])
const loading = ref(false)
const generating = ref(false)
const genForm = ref({ title: '', version: '' })

const doneLists = computed(() => lists.value.filter((l) => l.status === '已完成'))
const currentLists = computed(() => (tab.value === 'done' ? doneLists.value : lists.value))

const sourceLabel = (t) => ({ case: '用例', bug: 'Bug', rule: '规则' }[t] || '来源')

const viewSource = (it) => {
  ElMessage.info(`来源：${sourceLabel(it.sourceType)}${it.sourceRef ? ' · ' + it.sourceRef : ''}`)
}

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
    genForm.value = { title: '', version: '' }
    tab.value = 'list'
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

const downloadAll = () => {
  if (!lists.value.length) {
    ElMessage.info('暂无清单可导出')
    return
  }
  const wb = XLSX.utils.book_new()
  lists.value.forEach((list, i) => {
    const data = list.items.map((it) => ({
      序号: it.seq, 回归项: it.title, 优先级: it.priority, 来源: it.moduleLabel, 状态: it.status
    }))
    const name = `${i + 1}_${(list.title || '清单').slice(0, 24)}`
    XLSX.utils.book_append_sheet(wb, XLSX.utils.json_to_sheet(data), name)
  })
  XLSX.writeFile(wb, `回归清单_共${lists.value.length}份.xlsx`)
  ElMessage.success(`已导出 ${lists.value.length} 份清单`)
}

onMounted(load)
</script>
