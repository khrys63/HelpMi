<template>
  <div class="max-w-7xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-6">{{ $t('audit.title') }}</h1>

    <!-- Filters -->
    <div class="flex flex-wrap gap-3 mb-6">
      <select v-model="selectedAction" @change="load(0)"
        class="px-3 py-2 rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-sm text-gray-700 dark:text-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500">
        <option value="">{{ $t('audit.filter_all') }}</option>
        <option v-for="action in actions" :key="action" :value="action">
          {{ $t(`audit.actions.${action}`, action) }}
        </option>
      </select>
    </div>

    <!-- Table -->
    <div class="bg-white dark:bg-gray-800 rounded-xl shadow overflow-x-auto">
      <div v-if="loading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!entries.length" class="p-8 text-center text-gray-400">{{ $t('audit.no_entries') }}</div>
      <table v-else class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
        <thead>
          <tr class="bg-gray-50 dark:bg-gray-700 text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
            <th class="px-4 py-3 text-left">{{ $t('audit.col_date') }}</th>
            <th class="px-4 py-3 text-left">{{ $t('audit.col_action') }}</th>
            <th class="px-4 py-3 text-left">{{ $t('audit.col_actor') }}</th>
            <th class="px-4 py-3 text-left">{{ $t('audit.col_target') }}</th>
            <th class="px-4 py-3 text-left">{{ $t('audit.col_details') }}</th>
            <th class="px-4 py-3 text-left">{{ $t('audit.col_ip') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 dark:divide-gray-700">
          <tr v-for="entry in entries" :key="entry.id"
            class="hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors text-sm">
            <td class="px-4 py-3 whitespace-nowrap text-gray-500 dark:text-gray-400 font-mono text-xs">
              {{ formatDate(entry.createdAt) }}
            </td>
            <td class="px-4 py-3 whitespace-nowrap">
              <span :class="actionBadgeClass(entry.action)"
                class="px-2 py-0.5 rounded-full text-xs font-medium">
                {{ $t(`audit.actions.${entry.action}`, entry.action) }}
              </span>
            </td>
            <td class="px-4 py-3 text-gray-700 dark:text-gray-300 text-xs">
              {{ entry.actorEmail || '—' }}
            </td>
            <td class="px-4 py-3 text-gray-700 dark:text-gray-300 text-xs">
              <span v-if="entry.targetType" class="text-gray-400 dark:text-gray-500 mr-1">{{ entry.targetType }}</span>
              {{ entry.targetId || '—' }}
            </td>
            <td class="px-4 py-3 text-gray-500 dark:text-gray-400 text-xs font-mono">
              {{ entry.details || '—' }}
            </td>
            <td class="px-4 py-3 text-gray-400 dark:text-gray-500 text-xs font-mono">
              {{ entry.ipAddress || '—' }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex items-center justify-between mt-4">
      <button @click="load(page - 1)" :disabled="page === 0"
        class="px-3 py-1.5 text-sm rounded-lg border border-gray-300 dark:border-gray-600 disabled:opacity-40 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-gray-700 dark:text-gray-300">
        {{ $t('audit.prev') }}
      </button>
      <span class="text-sm text-gray-500 dark:text-gray-400">{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="load(page + 1)" :disabled="page >= totalPages - 1"
        class="px-3 py-1.5 text-sm rounded-lg border border-gray-300 dark:border-gray-600 disabled:opacity-40 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-gray-700 dark:text-gray-300">
        {{ $t('audit.next') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { auditApi } from '../services/api.js'

const loading = ref(false)
const entries = ref([])
const page = ref(0)
const totalPages = ref(1)
const selectedAction = ref('')

const actions = [
  'PAT_CREATED', 'PAT_REVOKED', 'PAT_AUTH_SUCCESS', 'PAT_AUTH_FAILURE',
  'USER_ROLE_CHANGED', 'USER_DEACTIVATED', 'USER_ACTIVATED',
  'USER_ORG_ADDED', 'USER_ORG_REMOVED', 'USER_PROJECTS_UPDATED',
  'TICKET_DELETED', 'ACCESS_DENIED'
]

async function load(p = 0) {
  loading.value = true
  page.value = p
  try {
    const params = { page: p, size: 50 }
    if (selectedAction.value) params.action = selectedAction.value
    const res = await auditApi.list(params)
    entries.value = res.data.content
    totalPages.value = res.data.totalPages
  } finally {
    loading.value = false
  }
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const badgeClasses = {
  PAT_AUTH_FAILURE: 'bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300',
  ACCESS_DENIED:    'bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300',
  USER_DEACTIVATED: 'bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300',
  TICKET_DELETED:   'bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300',
  PAT_AUTH_SUCCESS: 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300',
  USER_ACTIVATED:   'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300',
}
function actionBadgeClass(action) {
  return badgeClasses[action] ?? 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
}

onMounted(() => load(0))
</script>
