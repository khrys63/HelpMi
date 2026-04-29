<template>
  <div>
    <div v-if="loading" class="text-center py-12 text-gray-400">{{ $t('common.loading') }}</div>
    <template v-else>
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <router-link to="/projects" class="text-sm text-gray-400 dark:text-gray-500 hover:text-blue-600 dark:hover:text-blue-400">{{ $t('projects.title') }}</router-link>
            <span class="text-gray-300 dark:text-gray-600">/</span>
            <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ project.name }}</span>
          </div>
          <h1 class="text-2xl font-bold text-gray-900 dark:text-gray-100">
            <span class="text-gray-400 dark:text-gray-500 mr-2">{{ project.key }}</span>{{ project.name }}
          </h1>
          <p v-if="project.description" class="text-sm text-gray-500 dark:text-gray-400 mt-1">{{ project.description }}</p>
        </div>
        <router-link :to="`/projects/${project.id}/tickets/new`"
          class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
          {{ $t('tickets.new') }}
        </router-link>
      </div>

      <!-- Filtres -->
      <div class="flex flex-wrap gap-3 mb-5">
        <!-- Statuts -->
        <div class="relative">
          <button @click="openFilter = openFilter === 'status' ? null : 'status'"
            :class="['border rounded-lg px-3 py-1.5 text-sm flex items-center gap-1.5 select-none',
              selectedStatuses.length < config.statuses.length
                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                : 'border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300']">
            {{ $t('tickets.filter_statuses') }}
            <span v-if="selectedStatuses.length < config.statuses.length"
              class="bg-blue-600 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center font-medium">
              {{ selectedStatuses.length }}
            </span>
            <span class="text-gray-400 text-xs">▾</span>
          </button>
          <div v-if="openFilter === 'status'" class="fixed inset-0 z-10" @click="openFilter = null"></div>
          <div v-if="openFilter === 'status'"
            class="absolute top-full mt-1 left-0 z-20 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg p-2 min-w-40">
            <label v-for="s in config.statuses" :key="s.code"
              class="flex items-center gap-2 px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 rounded cursor-pointer text-sm text-gray-700 dark:text-gray-300">
              <input type="checkbox" :value="s.code" v-model="selectedStatuses"
                @change="page = 0; fetchTickets()" class="rounded">
              {{ s.label }}
            </label>
          </div>
        </div>

        <!-- Priorités -->
        <div class="relative">
          <button @click="openFilter = openFilter === 'priority' ? null : 'priority'"
            :class="['border rounded-lg px-3 py-1.5 text-sm flex items-center gap-1.5 select-none',
              selectedPriorities.length < config.priorities.length
                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                : 'border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300']">
            {{ $t('tickets.filter_priorities') }}
            <span v-if="selectedPriorities.length < config.priorities.length"
              class="bg-blue-600 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center font-medium">
              {{ selectedPriorities.length }}
            </span>
            <span class="text-gray-400 text-xs">▾</span>
          </button>
          <div v-if="openFilter === 'priority'" class="fixed inset-0 z-10" @click="openFilter = null"></div>
          <div v-if="openFilter === 'priority'"
            class="absolute top-full mt-1 left-0 z-20 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg p-2 min-w-40">
            <label v-for="p in config.priorities" :key="p.code"
              class="flex items-center gap-2 px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 rounded cursor-pointer text-sm text-gray-700 dark:text-gray-300">
              <input type="checkbox" :value="p.code" v-model="selectedPriorities"
                @change="page = 0; fetchTickets()" class="rounded">
              {{ p.label }}
            </label>
          </div>
        </div>

        <!-- Types -->
        <div class="relative">
          <button @click="openFilter = openFilter === 'type' ? null : 'type'"
            :class="['border rounded-lg px-3 py-1.5 text-sm flex items-center gap-1.5 select-none',
              selectedTypes.length < config.types.length
                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300'
                : 'border-gray-200 dark:border-gray-700 text-gray-700 dark:text-gray-300']">
            {{ $t('tickets.filter_types') }}
            <span v-if="selectedTypes.length < config.types.length"
              class="bg-blue-600 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center font-medium">
              {{ selectedTypes.length }}
            </span>
            <span class="text-gray-400 text-xs">▾</span>
          </button>
          <div v-if="openFilter === 'type'" class="fixed inset-0 z-10" @click="openFilter = null"></div>
          <div v-if="openFilter === 'type'"
            class="absolute top-full mt-1 left-0 z-20 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl shadow-lg p-2 min-w-40">
            <label v-for="t in config.types" :key="t.code"
              class="flex items-center gap-2 px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-gray-700 rounded cursor-pointer text-sm text-gray-700 dark:text-gray-300">
              <input type="checkbox" :value="t.code" v-model="selectedTypes"
                @change="page = 0; fetchTickets()" class="rounded">
              {{ t.label }}
            </label>
          </div>
        </div>
      </div>

      <!-- Liste tickets -->
      <div v-if="tickets.length === 0" class="text-center py-12 text-gray-400 dark:text-gray-500">{{ $t('tickets.no_tickets') }}</div>
      <div v-else class="space-y-2">
        <router-link v-for="t in tickets" :key="t.id"
          :to="`/projects/${project.id}/tickets/${t.id}`"
          class="flex items-center gap-4 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl px-4 py-3 hover:shadow-sm transition-shadow">
          <span class="text-xs font-mono text-gray-400 dark:text-gray-500 w-20 shrink-0">{{ t.reference }}</span>
          <span class="flex-1 font-medium text-gray-900 dark:text-gray-100 truncate">{{ t.title }}</span>
          <div class="flex items-center gap-2 shrink-0">
            <TypeBadge :type="t.type" />
            <PriorityBadge :priority="t.priority" />
            <StatusBadge :status="t.status" />
            <span v-if="t.assignee"
              class="inline-flex items-center gap-1 text-xs text-gray-600 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-full px-2 py-0.5"
              :title="`${t.assignee.firstName} ${t.assignee.lastName}`">
              <span class="w-4 h-4 rounded-full bg-blue-500 text-white flex items-center justify-center font-medium text-[10px]">
                {{ (t.assignee.firstName[0] + t.assignee.lastName[0]).toUpperCase() }}
              </span>
              {{ t.assignee.firstName }} {{ t.assignee.lastName }}
            </span>
          </div>
        </router-link>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-6">
        <button v-for="p in totalPages" :key="p" @click="page = p - 1; fetchTickets()"
          :class="['px-3 py-1 rounded text-sm', page === p - 1 ? 'bg-blue-600 text-white' : 'bg-white dark:bg-gray-800 border dark:border-gray-700 text-gray-600 dark:text-gray-300']">
          {{ p }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { projectsApi, ticketsApi } from '../services/api.js'
import { useConfigStore } from '../stores/config.js'
import StatusBadge from '../components/tickets/StatusBadge.vue'
import PriorityBadge from '../components/tickets/PriorityBadge.vue'
import TypeBadge from '../components/tickets/TypeBadge.vue'

const route = useRoute()
const projectId = route.params.projectId
const config = useConfigStore()

const project = ref({})
const tickets = ref([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(1)
const openFilter = ref(null)

const EXCLUDED_BY_DEFAULT = ['CANCELLED', 'CLOSED', 'RESOLVED']
const selectedStatuses   = ref([])
const selectedPriorities = ref([])
const selectedTypes      = ref([])

onMounted(async () => {
  selectedStatuses.value   = config.statuses.filter(s => !EXCLUDED_BY_DEFAULT.includes(s.code)).map(s => s.code)
  selectedPriorities.value = config.priorities.map(p => p.code)
  selectedTypes.value      = config.types.map(t => t.code)

  const [{ data: proj }] = await Promise.all([projectsApi.get(projectId)])
  project.value = proj
  await fetchTickets()
  loading.value = false
})

async function fetchTickets() {
  const params = { page: page.value, size: 20 }
  if (selectedStatuses.value.length < config.statuses.length)
    params.status = selectedStatuses.value.join(',')
  if (selectedPriorities.value.length < config.priorities.length)
    params.priority = selectedPriorities.value.join(',')
  if (selectedTypes.value.length < config.types.length)
    params.type = selectedTypes.value.join(',')
  const { data } = await ticketsApi.list(projectId, params)
  tickets.value = data.content
  totalPages.value = data.page?.totalPages ?? data.totalPages ?? 1
}
</script>
