<template>
  <div>
    <div v-if="loading" class="text-center py-12 text-gray-400">Chargement…</div>
    <template v-else>
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <div>
          <div class="flex items-center gap-2 mb-1">
            <router-link to="/projects" class="text-sm text-gray-400 hover:text-blue-600">Projets</router-link>
            <span class="text-gray-300">/</span>
            <span class="text-sm font-medium text-gray-700">{{ project.name }}</span>
          </div>
          <h1 class="text-2xl font-bold text-gray-900">
            <span class="text-gray-400 mr-2">{{ project.key }}</span>{{ project.name }}
          </h1>
          <p v-if="project.description" class="text-sm text-gray-500 mt-1">{{ project.description }}</p>
        </div>
        <router-link :to="`/projects/${project.id}/tickets/new`"
          class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
          + Nouveau ticket
        </router-link>
      </div>

      <!-- Filtres -->
      <div class="flex flex-wrap gap-3 mb-5">
        <select v-model="filters.status" @change="fetchTickets"
          class="border rounded-lg px-3 py-1.5 text-sm text-gray-700">
          <option value="">Tous les statuts</option>
          <option v-for="s in config.statuses" :key="s.code" :value="s.code">{{ s.label }}</option>
        </select>
        <select v-model="filters.priority" @change="fetchTickets"
          class="border rounded-lg px-3 py-1.5 text-sm text-gray-700">
          <option value="">Toutes priorités</option>
          <option v-for="p in config.priorities" :key="p.code" :value="p.code">{{ p.label }}</option>
        </select>
        <select v-model="filters.type" @change="fetchTickets"
          class="border rounded-lg px-3 py-1.5 text-sm text-gray-700">
          <option value="">Tous types</option>
          <option v-for="t in config.types" :key="t.code" :value="t.code">{{ t.label }}</option>
        </select>
      </div>

      <!-- Liste tickets -->
      <div v-if="tickets.length === 0" class="text-center py-12 text-gray-400">Aucun ticket.</div>
      <div v-else class="space-y-2">
        <router-link v-for="t in tickets" :key="t.id"
          :to="`/projects/${project.id}/tickets/${t.id}`"
          class="flex items-center gap-4 bg-white border border-gray-200 rounded-xl px-4 py-3 hover:shadow-sm transition-shadow">
          <span class="text-xs font-mono text-gray-400 w-20 shrink-0">{{ t.reference }}</span>
          <span class="flex-1 font-medium text-gray-900 truncate">{{ t.title }}</span>
          <div class="flex items-center gap-2 shrink-0">
            <TypeBadge :type="t.type" />
            <PriorityBadge :priority="t.priority" />
            <StatusBadge :status="t.status" />
            <span v-if="t.assignee" class="text-xs text-gray-500">
              {{ t.assignee.firstName }} {{ t.assignee.lastName }}
            </span>
          </div>
        </router-link>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-6">
        <button v-for="p in totalPages" :key="p" @click="page = p - 1; fetchTickets()"
          :class="['px-3 py-1 rounded text-sm', page === p - 1 ? 'bg-blue-600 text-white' : 'bg-white border text-gray-600']">
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
const filters = ref({ status: '', priority: '', type: '' })

onMounted(async () => {
  const [{ data: proj }] = await Promise.all([projectsApi.get(projectId)])
  project.value = proj
  await fetchTickets()
  loading.value = false
})

async function fetchTickets() {
  const params = { page: page.value, size: 20 }
  if (filters.value.status) params.status = filters.value.status
  if (filters.value.priority) params.priority = filters.value.priority
  if (filters.value.type) params.type = filters.value.type
  const { data } = await ticketsApi.list(projectId, params)
  tickets.value = data.content
  totalPages.value = data.totalPages
}
</script>
