<template>
  <div class="fixed bottom-5 right-5 z-50 flex flex-col gap-2 pointer-events-none">
    <transition-group name="toast">
      <div
        v-for="t in toast.toasts"
        :key="t.id"
        class="pointer-events-auto flex items-start gap-3 bg-white dark:bg-gray-800 rounded-xl shadow-lg border-l-4 px-4 py-3 w-80"
        :class="borderClass(t.type)"
      >
        <div class="flex-1 min-w-0">
          <p class="text-sm text-gray-800 dark:text-gray-200 leading-snug">{{ t.message }}</p>
        </div>
        <button @click="toast.remove(t.id)" class="text-gray-300 dark:text-gray-600 hover:text-gray-600 dark:hover:text-gray-300 shrink-0 text-base leading-none mt-0.5">✕</button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToastStore } from '../../stores/toast.js'

const toast = useToastStore()

function borderClass(type) {
  return {
    success: 'border-green-500',
    info:    'border-blue-500',
    warning: 'border-yellow-500',
    error:   'border-red-500',
  }[type] ?? 'border-blue-500'
}
</script>

<style scoped>
.toast-enter-active {
  transition: all 0.25s ease-out;
}
.toast-leave-active {
  transition: all 0.2s ease-in;
}
.toast-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
