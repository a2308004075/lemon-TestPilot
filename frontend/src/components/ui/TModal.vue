<script setup>
import { X } from 'lucide-vue-next'

defineProps({
  title: { type: String, default: '' },
  width: { type: Number, default: 590 }
})
const emit = defineEmits(['close'])
</script>

<template>
  <div class="overlay modal-layer" @mousedown.self="emit('close')">
    <div class="modal" :style="{ width: width + 'px' }">
      <header>
        <h2>{{ title }}</h2>
        <button class="icon-button" @click="emit('close')"><X /></button>
      </header>
      <slot />
      <footer v-if="$slots.footer">
        <slot name="footer" />
      </footer>
    </div>
  </div>
</template>
