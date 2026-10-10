<script setup>
import { FileText } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '暂无数据' },
  desc: { type: String, default: '' }
})
</script>

<template>
  <div class="empty">
    <FileText />
    <strong>{{ title }}</strong>
    <p v-if="desc">{{ desc }}</p>
  </div>
</template>
