<script setup>
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 20 },
  total: { type: Number, default: 0 }
})
const emit = defineEmits(['update:page', 'update:pageSize'])

const pages = computed(() => Math.max(1, Math.ceil(props.total / Math.max(1, props.pageSize))))

const setPage = (p) => {
  if (p < 1 || p > pages.value || p === props.page) return
  emit('update:page', p)
}
const onSize = (e) => {
  emit('update:pageSize', Number(e.target.value))
  emit('update:page', 1)
}
</script>

<template>
  <div class="pagination">
    <span>共 {{ total }} 条</span>
    <select :value="pageSize" @change="onSize">
      <option :value="20">20</option>
      <option :value="50">50</option>
      <option :value="100">100</option>
    </select>
    <button :disabled="page <= 1" @click="setPage(page - 1)"><ChevronLeft /></button>
    <b>{{ page }} / {{ pages }}</b>
    <button :disabled="page >= pages" @click="setPage(page + 1)"><ChevronRight /></button>
  </div>
</template>
