<template>
  <div class="max-w-7xl mx-auto px-4 py-6">
    <h1 class="text-xl font-bold text-gray-800 dark:text-gray-100 mb-6">{{ $t('manager_tracking.title') }}</h1>

    <div v-if="loading" class="text-center py-20 text-gray-400">{{ $t('common.loading') }}</div>

    <template v-else>
      <!-- Empty state -->
      <div v-if="projects.length === 0" class="text-center py-20 text-gray-400">
        {{ $t('manager_tracking.no_projects') }}
      </div>

      <!-- Project cards -->
      <div v-else class="space-y-6">
        <div v-for="project in projects" :key="project.projectId"
          class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700">

          <!-- Project header (collapsible) -->
          <button @click="toggleProject(project.projectId)"
            class="w-full flex items-center justify-between px-4 pt-4 pb-3 border-b border-gray-100 dark:border-gray-700 text-left">
            <div class="flex items-center gap-2">
              <span class="text-xs font-mono text-gray-400">{{ project.key }}</span>
              <span class="font-semibold text-sm text-gray-700 dark:text-gray-200">{{ project.name }}</span>
            </div>
            <svg class="w-4 h-4 text-gray-400 transition-transform"
              :class="{ 'rotate-180': collapsedProjects.has(project.projectId) }"
              fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </button>

          <!-- Collapsed body -->
          <div v-show="!collapsedProjects.has(project.projectId)" class="divide-y divide-gray-50 dark:divide-gray-700/50">

            <!-- Assignees section -->
            <div v-if="project.assignees.length > 0" class="px-4 py-4">
              <h3 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase mb-3">
                {{ $t('manager_tracking.assignee') }}
              </h3>
              <div v-for="assignee in project.assignees" :key="assignee.id"
                class="mb-4 last:mb-0">
                <!-- Assignee header (collapsible) -->
                <button @click="toggleAssignee(assignee.id)"
                  class="w-full flex items-center justify-between text-left group">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-gray-800 dark:text-gray-100 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                      {{ assignee.firstName }} {{ assignee.lastName }}
                    </span>
                    <span class="text-xs text-gray-400 dark:text-gray-500">({{ assignee.email }})</span>
                  </div>
                  <svg class="w-4 h-4 text-gray-400 transition-transform"
                    :class="{ 'rotate-180': collapsedAssignees.has(assignee.id) }"
                    fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                  </svg>
                </button>

                <!-- Assignee stats -->
                <div v-show="!collapsedAssignees.has(assignee.id)" class="mt-2">
                  <div class="flex flex-wrap gap-2 mb-3">
                    <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full">
                      {{ $t('manager_tracking.total') }}: {{ assignee.counts.total }}
                    </span>
                    <span class="text-xs bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full">
                      {{ $t('manager_tracking.open') }}: {{ assignee.counts.open }}
                    </span>
                    <span class="text-xs bg-yellow-100 dark:bg-yellow-900/50 text-yellow-700 dark:text-yellow-300 px-2 py-0.5 rounded-full">
                      {{ $t('manager_tracking.in_progress') }}: {{ assignee.counts.inProgress }}
                    </span>
                    <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full">
                      {{ $t('manager_tracking.stand_by') }}: {{ assignee.counts.standBy }}
                    </span>
                    <span class="text-xs bg-green-100 dark:bg-green-900/50 text-green-700 dark:text-green-300 px-2 py-0.5 rounded-full">
                      {{ $t('manager_tracking.resolved') }}: {{ assignee.counts.resolved }}
                    </span>
                  </div>

                  <!-- Tickets for this assignee -->
                  <div v-if="assignee.tickets.length > 0" class="space-y-2">
                    <button v-for="ticket in assignee.tickets" :key="ticket.id"
                      @click="openTicket(ticket)"
                      class="w-full text-left px-3 py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors flex items-start justify-between gap-2">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-2 mb-1">
                          <span class="text-xs font-mono text-blue-600 dark:text-blue-400 flex-shrink-0">{{ ticket.reference }}</span>
                          <span :class="statusChip(ticket.status)" class="text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                            {{ formatStatus(ticket.status) }}
                          </span>
                        </div>
                        <p class="text-sm text-gray-800 dark:text-gray-100 truncate">{{ ticket.title }}</p>
                      </div>
                      <span v-if="ticket.dueDate" :class="dueDateClass(ticket.dueDate)" class="text-xs flex-shrink-0">
                        {{ formatDate(ticket.dueDate) }}
                      </span>
                    </button>
                  </div>
                  <p v-else class="text-xs text-gray-400 dark:text-gray-500 italic pl-1">
                    {{ $t('dashboard.empty') }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Unassigned section -->
            <div v-if="project.unassignedTickets && project.unassignedTickets.length > 0" class="px-4 py-4">
              <h3 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase mb-3">
                {{ $t('manager_tracking.unassigned') }} ({{ project.unassignedTickets.length }})
              </h3>
              <div class="space-y-2">
                <button v-for="ticket in project.unassignedTickets" :key="ticket.id"
                  @click="openTicket(ticket)"
                  class="w-full text-left px-3 py-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors flex items-start justify-between gap-2">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <span class="text-xs font-mono text-blue-600 dark:text-blue-400 flex-shrink-0">{{ ticket.reference }}</span>
                      <span :class="statusChip(ticket.status)" class="text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                        {{ formatStatus(ticket.status) }}
                      </span>
                    </div>
                    <p class="text-sm text-gray-800 dark:text-gray-100 truncate">{{ ticket.title }}</p>
                  </div>
                  <span v-if="ticket.dueDate" :class="dueDateClass(ticket.dueDate)" class="text-xs flex-shrink-0">
                    {{ formatDate(ticket.dueDate) }}
                  </span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { managerTrackingApi } from '../services/api.js'

const router = useRouter()

const loading = ref(true)
const projects = ref([])
const collapsedProjects = ref(new Set())
const collapsedAssignees = ref(new Set())

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

function formatDate(d) {
  return new Date(d).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

function dueDateClass(d) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return new Date(d) < today
    ? 'text-red-500 font-medium'
    : 'text-gray-400 dark:text-gray-500'
}

function openTicket(ticket) {
  router.push({ name: 'ticket-detail', params: { projectId: ticket.projectId, ticketId: ticket.id } })
}

function toggleProject(projectId) {
  const set = collapsedProjects.value
  if (set.has(projectId)) set.delete(projectId)
  else set.add(projectId)
}

function toggleAssignee(assigneeId) {
  const set = collapsedAssignees.value
  if (set.has(assigneeId)) set.delete(assigneeId)
  else set.add(assigneeId)
}

onMounted(async () => {
  try {
    const { data: result } = await managerTrackingApi.get()
    projects.value = result.projects || []
  } finally {
    loading.value = false
  }
})
</script>
