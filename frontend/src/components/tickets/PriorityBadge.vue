<template>
  <span :class="['inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold', badgeClass]">
    {{ label }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { useConfigStore, COLORS } from '../../stores/config.js'

const props = defineProps({ priority: String })
const config = useConfigStore()

const entry = computed(() => config.getPriority(props.priority))
const label = computed(() => entry.value?.label ?? props.priority)
const badgeClass = computed(() => COLORS[entry.value?.color]?.badge ?? 'bg-gray-100 text-gray-700')
</script>
