<script setup>
import { computed } from 'vue'

const props = defineProps({
  value: { type: [String, Number], default: '' }
})

const cls = computed(() => {
  const v = String(props.value || '')
  if (!v) return 'gray'
  if (['P0', '失败', '阻塞', '不能上线', '已驳回', '驳回补充'].includes(v)) return 'red'
  if (['P1', '待审核', '待补充', '待复核', '有条件上线', '未执行', '跳过'].includes(v)) return 'yellow'
  if (['通过', '已通过', '已完成', '已发布', '审核通过', '已入库', '可上线', '启用', '可创建'].includes(v)) return 'green'
  if (['执行中', '项目', '生成中'].includes(v)) return 'blue'
  return 'gray'
})
</script>

<template>
  <span class="badge" :class="cls">{{ value || '—' }}</span>
</template>
