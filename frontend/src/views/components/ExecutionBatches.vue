<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Trash2, Download } from 'lucide-vue-next'
import * as XLSX from 'xlsx'

import Badge from '../../components/ui/Badge.vue'
import Empty from '../../components/ui/Empty.vue'
import TPagination from '../../components/ui/TPagination.vue'
import TDrawer from '../../components/ui/TDrawer.vue'
import { apiCaseBatches, apiCaseBatchDetail, apiCaseBatchUpdate, apiCaseBatchDelete } from '../../api'
import { fmtTime } from '../../utils/format'

const ITEM_STATUSES = ['待执行', '通过', '失败', '阻塞', '跳过']

const query = ref({ page: 1, size: 20 })
const rows = ref([])
const total = ref(0)
const loading = ref(false)

const detailVisible = ref(false)
const detail = ref(null)
const savingId = ref(null)

const readonly = computed(() => detail.value && detail.value.batch.status === '已完成')

const load = async () => {
  loading.value = true
  try {
    const data = await apiCaseBatches(query.value)
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const progressText = (b) => {
  const parts = []
  if (b.passed) parts.push(`通过 ${b.passed}`)
  if (b.failed) parts.push(`失败 ${b.failed}`)
  if (b.blocked) parts.push(`阻塞 ${b.blocked}`)
  if (b.skipped) parts.push(`跳过 ${b.skipped}`)
  const pending = b.total - b.passed - b.failed - b.blocked - b.skipped
  if (pending) parts.push(`待执行 ${pending}`)
  return parts.length ? parts.join(' · ') : `共 ${b.total} 条`
}

const openDetail = async (row) => {
  detail.value = await apiCaseBatchDetail(row.id)
  detailVisible.value = true
}

/** 逐条登记：状态下拉 + 备注 + 保存（批次完成后只读，6.11 W3） */
const saveItem = async (item) => {
  savingId.value = item.id
  try {
    detail.value = await apiCaseBatchUpdate(detail.value.batch.id, {
      itemId: item.id,
      status: item.status,
      note: item.note || ''
    })
    const idx = rows.value.findIndex((r) => r.id === detail.value.batch.id)
    if (idx >= 0) rows.value[idx] = detail.value.batch
    ElMessage.success(`${item.caseNoSnapshot} 已登记：${item.status}`)
  } finally {
    savingId.value = null
  }
}

/** 删除执行批次（二次确认后先删子项再删批次） */
const deleteBatch = async (row) => {
  if (!confirm(`确定删除执行批次「${row.title}」？\n删除后不可恢复。`)) return
  await apiCaseBatchDelete(row.id)
  ElMessage.success('执行批次已删除')
  await load()
}

/** 导出本批次用例执行结果为 Excel（列表行用，需先拉详情） */
const exportBatchFromRow = async (row, e) => {
  e.stopPropagation()
  try {
    const data = await apiCaseBatchDetail(row.id)
    exportBatchXlsx(data)
  } catch (err) {
    // api 拦截器已弹错误提示
  }
}

/** 导出当前详情批次用例执行结果为 Excel */
const exportDetailBatch = () => {
  if (!detail.value) return
  exportBatchXlsx(detail.value)
}

const exportBatchXlsx = (batchData) => {
  const batch = batchData.batch || {}
  const items = batchData.items || []
  if (!items.length) {
    ElMessage.info('该批次没有可导出的执行条目')
    return
  }
  const data = items.map((item) => ({
    批次号: batch.batchNo || '',
    批次标题: batch.title || '',
    创建时间: fmtTime(batch.createdAt),
    批次状态: batch.status || '',
    用例编号: item.caseNoSnapshot || '',
    用例标题: item.titleSnapshot || '',
    优先级: item.prioritySnapshot || '',
    执行结果: item.status || '',
    备注: item.note || '',
    登记时间: fmtTime(item.executedAt)
  }))
  const sheet = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, sheet, '执行记录')
  const filename = `${batch.title || '执行批次'}_执行记录`.replace(/[\\/:*?"<>|]/g, '_')
  XLSX.writeFile(wb, `${filename}.xlsx`)
  ElMessage.success(`已导出 ${items.length} 条执行记录（Excel）`)
}

onMounted(load)
</script>

<template>
  <div class="stack" style="margin-top: 18px">
    <template v-if="rows.length">
      <div class="panel table-panel">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>批次号</th>
                <th>执行批次</th>
                <th>进度</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="b in rows" :key="b.id" @click="openDetail(b)">
                <td>{{ b.batchNo }}</td>
                <td>
                  <strong>{{ b.title }}</strong>
                  <small class="id">{{ fmtTime(b.createdAt) }}</small>
                </td>
                <td>{{ progressText(b) }}</td>
                <td><Badge :value="b.status" /></td>
                <td style="cursor: default" @click.stop>
                  <div style="display: flex; gap: 8px; align-items: center">
                    <button class="button" style="min-height: 34px" @click="openDetail(b)">
                      {{ b.status === '已完成' ? '查看结果' : '登记结果' }}
                    </button>
                    <button class="icon-button" title="导出 Excel" @click="exportBatchFromRow(b, $event)">
                      <Download />
                    </button>
                    <button class="icon-button danger" title="删除" @click="deleteBatch(b)">
                      <Trash2 />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <TPagination v-model:page="query.page" v-model:page-size="query.size" :total="total"
                     @update:page="load" @update:page-size="load" />
      </div>
    </template>

    <!-- 空态与手册 UI 参考一致（6.11 W3） -->
    <Empty v-else-if="!loading" title="暂无独立执行批次" desc="勾选用例后点击“创建执行”，执行结果会在这里按批次归档。" />

    <TDrawer
      v-if="detailVisible && detail" eyebrow="EXECUTION BATCH"
      :title="detail.batch.title"
      :subtitle="detail.batch.createdAt ? fmtTime(detail.batch.createdAt) : ''"
      @close="detailVisible = false"
    >
      <div style="display: flex; gap: 8px; margin-bottom: 12px">
        <button class="button" @click="exportDetailBatch">
          <Download /> 导出本批次 Excel
        </button>
      </div>

      <div class="detail-badges">
        <span class="wb-id">批次号 {{ detail.batch.batchNo }}</span>
        <Badge :value="detail.batch.status" />
        <span class="wb-id">共 {{ detail.batch.total }} 条</span>
        <span v-if="detail.batch.passed" class="wb-id">通过 {{ detail.batch.passed }}</span>
        <span v-if="detail.batch.failed" class="wb-id" style="color: var(--red)">失败 {{ detail.batch.failed }}</span>
        <span v-if="detail.batch.blocked" class="wb-id" style="color: var(--red)">阻塞 {{ detail.batch.blocked }}</span>
        <span v-if="detail.batch.skipped" class="wb-id">跳过 {{ detail.batch.skipped }}</span>
      </div>

      <div v-for="item in detail.items" :key="item.id" class="batch-item">
        <div class="batch-item-head">
          <Badge :value="item.prioritySnapshot" />
          <strong>{{ item.titleSnapshot }}</strong>
          <small class="id">{{ item.caseNoSnapshot }}</small>
        </div>
        <div class="batch-item-form">
          <select v-model="item.status" :disabled="readonly">
            <option v-for="s in ITEM_STATUSES" :key="s">{{ s }}</option>
          </select>
          <input v-model="item.note" placeholder="执行备注（可选）" :disabled="readonly">
          <button v-if="!readonly" class="button primary" style="min-height: 36px"
                  :disabled="savingId === item.id" @click="saveItem(item)">
            {{ savingId === item.id ? '保存中…' : '保存' }}
          </button>
        </div>
        <small v-if="item.executedAt" class="batch-item-time">登记于 {{ fmtTime(item.executedAt) }}</small>
      </div>

      <p v-if="readonly" class="tip">批次已完成，结果只读。如需补登请新建执行批次。</p>
    </TDrawer>
  </div>
</template>
