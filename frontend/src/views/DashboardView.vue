<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <h1 class="text-xl font-bold text-gray-800 dark:text-gray-100 mb-6">{{ $t('dashboard.title') }}</h1>

    <div v-if="loading" class="text-center py-20 text-gray-400">{{ $t('common.loading') }}</div>

    <template v-else>
      <!-- 2×2 grid — 4 personal sections -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <section v-for="section in sections" :key="section.key"
          class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 flex flex-col">
          <div class="flex items-center gap-2 px-4 pt-4 pb-3 border-b border-gray-100 dark:border-gray-700">
            <span class="font-semibold text-sm text-gray-700 dark:text-gray-200">{{ section.label }}</span>
            <span class="text-xs bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full font-medium">
              {{ section.tickets.length }}
            </span>
          </div>
          <div class="divide-y divide-gray-50 dark:divide-gray-700/50 overflow-y-auto max-h-72">
            <p v-if="section.tickets.length === 0"
              class="px-4 py-6 text-sm text-gray-400 text-center">
              {{ $t('dashboard.empty') }}
            </p>
            <button v-for="t in section.tickets" :key="t.id"
              class="w-full text-left px-4 py-2.5 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors flex items-center gap-2"
              @click="openTicket(t)">
              <span class="text-xs font-mono text-blue-600 dark:text-blue-400 flex-shrink-0">{{ t.reference }}</span>
              <span :class="statusChip(t.status)"
                class="text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                {{ formatStatus(t.status) }}
              </span>
              <p class="text-sm text-gray-800 dark:text-gray-100 truncate">{{ t.title }}</p>
            </button>
          </div>
        </section>
      </div>

      <!-- Project stats — full width -->
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">
        <div class="px-4 pt-4 pb-3 border-b border-gray-100 dark:border-gray-700">
          <span class="font-semibold text-sm text-gray-700 dark:text-gray-200">{{ $t('dashboard.project_stats') }}</span>
        </div>
        <p v-if="data.projectStats.length === 0"
          class="py-10 text-center text-sm text-gray-400">
          {{ $t('dashboard.no_active_tickets') }}
        </p>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-700 text-xs font-medium text-gray-500 dark:text-gray-400">
                <th class="text-left px-4 py-2.5">{{ $t('dashboard.col_project') }}</th>
                <th class="text-center px-4 py-2.5 text-blue-600 dark:text-blue-400">{{ $t('dashboard.col_open') }}</th>
                <th class="text-center px-4 py-2.5 text-yellow-600 dark:text-yellow-400">{{ $t('dashboard.col_in_progress') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50 dark:divide-gray-700/50">
              <tr v-for="p in data.projectStats" :key="p.projectId"
                class="hover:bg-gray-50 dark:hover:bg-gray-700/50 cursor-pointer transition-colors"
                @click="goToProject(p.projectId)">
                <td class="px-4 py-3">
                  <span class="font-mono text-xs text-gray-400 mr-2">{{ p.projectKey }}</span>
                  <span class="text-gray-800 dark:text-gray-100">{{ p.projectName }}</span>
                </td>
                <td class="text-center px-4 py-3">
                  <span v-if="p.open > 0"
                    class="bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300 text-xs px-2 py-0.5 rounded-full font-medium">
                    {{ p.open }}
                  </span>
                  <span v-else class="text-gray-300 dark:text-gray-600">—</span>
                </td>
                <td class="text-center px-4 py-3">
                  <span v-if="p.inProgress > 0"
                    class="bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-300 text-xs px-2 py-0.5 rounded-full font-medium">
                    {{ p.inProgress }}
                  </span>
                  <span v-else class="text-gray-300 dark:text-gray-600">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { dashboardApi } from '../services/api.js'

const router = useRouter()
const { t } = useI18n()

const loading = ref(true)
const data = ref({ myOpenTickets: [], assignedToMe: [], watchedTickets: [], dueSoon: [], projectStats: [] })

const sections = computed(() => [
  { key: 'my_open',  label: t('dashboard.my_open'),  tickets: data.value.myOpenTickets },
  { key: 'assigned', label: t('dashboard.assigned'),  tickets: data.value.assignedToMe },
  { key: 'watched',  label: t('dashboard.watched'),   tickets: data.value.watchedTickets },
  { key: 'due_soon', label: t('dashboard.due_soon'),  tickets: data.value.dueSoon },
])

const STATUS_COLORS = {
  OPEN:        'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-300',
  STAND_BY:    'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
  RESOLVED:    'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300',
  CLOSED:      'bg-gray-200 text-gray-500 dark:bg-gray-600 dark:text-gray-400',
  CANCELLED:   'bg-red-100 text-red-600 dark:bg-red-900/50 dark:text-red-300',
}

function statusChip(status) {
  return STATUS_COLORS[status] ?? 'bg-gray-100 text-gray-600'
}

function formatStatus(s) {
  return s.replace(/_/g, ' ')
}

function openTicket(t) {
  router.push({ name: 'ticket-detail', params: { projectId: t.projectId, ticketId: t.id } })
}

function goToProject(projectId) {
  router.push({ name: 'project-detail', params: { projectId } })
}

onMounted(async () => {
  try {
    const { data: dash } = await dashboardApi.get()
    data.value = dash
  } finally {
    loading.value = false
  }
})
</script>
