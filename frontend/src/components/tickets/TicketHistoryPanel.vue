<template>
  <!-- Overlay -->
  <div v-if="open" class="fixed inset-0 z-40 flex justify-end" @click.self="$emit('close')">
    <!-- Backdrop -->
    <div class="absolute inset-0 bg-black/30 dark:bg-black/50" @click="$emit('close')"></div>

    <!-- Panel -->
    <div class="relative z-50 flex flex-col w-full max-w-lg bg-white dark:bg-gray-900 shadow-2xl h-full overflow-hidden">
      <!-- Header -->
      <div class="flex items-center justify-between px-5 py-4 border-b dark:border-gray-700 shrink-0">
        <h2 class="font-semibold text-gray-900 dark:text-gray-100 text-base">
          {{ $t('tickets.history_title') }}
        </h2>
        <button @click="$emit('close')"
          class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg p-1 transition-colors">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-y-auto px-5 py-4">
        <div v-if="loading" class="text-center py-12 text-gray-400 text-sm">{{ $t('common.loading') }}</div>

        <div v-else-if="!entries.length" class="text-center py-12 text-gray-400 text-sm">
          {{ $t('tickets.history_empty') }}
        </div>

        <ol v-else class="relative border-l border-gray-200 dark:border-gray-700 ml-3 space-y-6">
          <li v-for="entry in entries" :key="entry.id" class="ml-5">
            <!-- Dot -->
            <span class="absolute -left-2.5 mt-1 flex h-5 w-5 items-center justify-center rounded-full"
              :class="dotClass(entry.field)">
              <component :is="fieldIcon(entry.field)" class="w-3 h-3 text-white" />
            </span>

            <div class="space-y-1">
              <!-- Who + when -->
              <div class="flex items-center gap-2 text-xs text-gray-400 dark:text-gray-500">
                <span class="font-medium text-gray-600 dark:text-gray-300">
                  {{ fullName(entry.changedBy) }}
                </span>
                <span>·</span>
                <time :title="entry.changedAt">{{ formatRelative(entry.changedAt) }}</time>
              </div>

              <!-- Field label -->
              <p class="text-sm font-medium text-gray-800 dark:text-gray-200">
                {{ fieldLabel(entry.field) }}
              </p>

              <!-- Values -->
              <div v-if="entry.field !== 'created'" class="flex flex-col gap-1 text-xs">
                <div v-if="entry.oldValue" class="flex items-start gap-1.5">
                  <span class="shrink-0 mt-0.5 text-red-400">–</span>
                  <span class="text-gray-500 dark:text-gray-400 line-through break-words">{{ truncate(entry.oldValue) }}</span>
                </div>
                <div v-if="entry.newValue" class="flex items-start gap-1.5">
                  <span class="shrink-0 mt-0.5 text-green-500">+</span>
                  <span class="text-gray-700 dark:text-gray-200 break-words">{{ truncate(entry.newValue) }}</span>
                </div>
                <div v-if="!entry.oldValue && !entry.newValue" class="text-gray-400 italic">—</div>
              </div>
              <div v-else class="text-xs text-gray-500 dark:text-gray-400">
                {{ entry.newValue }}
              </div>
            </div>
          </li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { ticketsApi } from '../../services/api.js'

const props = defineProps({
  open: Boolean,
  projectId: String,
  ticketId: String
})
defineEmits(['close'])

const { t, locale } = useI18n()
const entries = ref([])
const loading = ref(false)

watch(() => props.open, async (val) => {
  if (!val) return
  loading.value = true
  try {
    const { data } = await ticketsApi.history(props.projectId, props.ticketId)
    entries.value = data
  } finally {
    loading.value = false
  }
})

function fullName(user) {
  if (!user) return '?'
  return `${user.firstName} ${user.lastName}`.trim() || user.email
}

function truncate(str, max = 300) {
  if (!str || str.length <= max) return str
  return str.slice(0, max) + '…'
}

function formatRelative(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return t('tickets.history_just_now')
  if (diff < 3600) return t('tickets.history_minutes_ago', { n: Math.floor(diff / 60) })
  if (diff < 86400) return t('tickets.history_hours_ago', { n: Math.floor(diff / 3600) })
  return d.toLocaleDateString(locale.value, { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function fieldLabel(field) {
  return t(`tickets.history_field_${field}`, field)
}

const FIELD_COLORS = {
  created:     'bg-blue-500',
  status:      'bg-purple-500',
  assignee:    'bg-orange-400',
  priority:    'bg-yellow-500',
  type:        'bg-cyan-500',
  title:       'bg-gray-500',
  description: 'bg-gray-400',
  dueDate:     'bg-red-400',
  clients:     'bg-emerald-500',
  labels:      'bg-indigo-400',
  project:     'bg-pink-500',
}

function dotClass(field) {
  return FIELD_COLORS[field] || 'bg-gray-400'
}

// Inline SVG icons as render functions (no extra deps)
const icons = {
  created:     () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M12 4v16m8-8H4' })]),
  status:      () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z' })]),
  assignee:    () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' })]),
  priority:    () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M3 4h13M3 8h9m-9 4h9m5-4v12m0 0l-4-4m4 4l4-4' })]),
  dueDate:     () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' })]),
  default:     () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', fill: 'none', viewBox: '0 0 24 24', stroke: 'currentColor', 'stroke-width': 2 }, [h('path', { 'stroke-linecap': 'round', 'stroke-linejoin': 'round', d: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z' })]),
}

function fieldIcon(field) {
  return icons[field] || icons.default
}
</script>
