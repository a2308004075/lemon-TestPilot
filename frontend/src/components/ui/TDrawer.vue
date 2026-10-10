<script setup>
import { X } from 'lucide-vue-next'

defineProps({
  eyebrow: { type: String, default: 'DETAIL VIEW' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  widthClass: { type: String, default: '' }
})
const emit = defineEmits(['close'])
</script>

<template>
  <div class="overlay" @mousedown.self="emit('close')">
    <aside class="drawer" :class="widthClass">
      <header>
        <div>
          <span class="eyebrow">{{ eyebrow }}</span>
          <h2>{{ title }}</h2>
          <small v-if="subtitle">{{ subtitle }}</small>
        </div>
        <button class="icon-button" @click="emit('close')"><X /></button>
      </header>
      <div class="drawer-body">
        <slot />
      </div>
    </aside>
  </div>
</template>
