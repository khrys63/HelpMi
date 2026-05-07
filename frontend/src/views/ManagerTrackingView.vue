<template>
  <div class="">
    <h1 class="text-2xl font-bold text-gray-800 dark:text-gray-100 mb-6">{{ $t('manager_tracking.title') }}</h1>

    <div v-if="loading" class="text-center py-20 text-gray-400">{{ $t('common.loading') }}</div>

    <template v-else>
      <!-- Empty state -->
      <div v-if="projects.length === 0" class="text-center py-20 text-gray-400">
        {{ $t('manager_tracking.no_projects') }}
      </div>

      <template v-else>
        <!-- ── Intro chart: open tickets per project ── -->
        <div v-if="hasActiveTickets"
          class="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 px-4 pt-4 pb-2 mb-6">
          <p class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase mb-1">
            {{ $t('manager_tracking.tickets_by_project') }}
          </p>
          <VueApexCharts
            type="bar"
            height="160"
            :options="introChartOptions"
            :series="introSeries"
          />
        </div>

        <!-- ── Project cards ── -->
        <div class="space-y-6">
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
            <div v-show="!collapsedProjects.has(project.projectId)">

              <!-- ── Assignees ── -->
              <div v-if="project.assignees.length > 0">
                <div class="px-4 pt-3 pb-1">
                  <h3 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase">
                    {{ $t('manager_tracking.assignee') }}
                  </h3>
                </div>

                <!-- Alternating rows -->
                <div class="divide-y divide-gray-100 dark:divide-gray-700/50">
                  <div v-for="(assignee, aIdx) in project.assignees" :key="assignee.id"
                    :class="aIdx % 2 === 1
                      ? 'bg-slate-50 dark:bg-white/[0.03]'
                      : 'bg-white dark:bg-gray-800'"
                    class="px-4 py-3">

                    <!-- Assignee header (collapsible) -->
                    <button @click="toggleAssignee(assignee.id)"
                      class="w-full flex items-center justify-between text-left group">
                      <div class="flex items-center gap-2 min-w-0">
                        <!-- Avatar initials -->
                        <span class="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300
                          flex items-center justify-center text-xs font-bold flex-shrink-0">
                          {{ initials(assignee) }}
                        </span>
                        <span class="text-sm font-medium text-gray-800 dark:text-gray-100
                          group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors truncate">
                          {{ assignee.firstName }} {{ assignee.lastName }}
                        </span>
                        <span class="text-xs text-gray-400 dark:text-gray-500 truncate hidden sm:block">
                          {{ assignee.email }}
                        </span>
                      </div>
                      <svg class="w-4 h-4 text-gray-400 flex-shrink-0 transition-transform"
                        :class="{ 'rotate-180': collapsedAssignees.has(assignee.id) }"
                        fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                      </svg>
                    </button>

                    <!-- Expanded: stats + tickets + donut chart -->
                    <div v-if="!collapsedAssignees.has(assignee.id)" class="mt-3 flex gap-4 items-start">

                      <!-- Left: stats pills + ticket list -->
                      <div class="flex-1 min-w-0">
                        <div class="flex flex-wrap gap-1.5 mb-3">
                          <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full">
                            {{ $t('manager_tracking.total') }}: {{ assignee.counts.total }}
                          </span>
                          <span class="text-xs bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded-full">
                            {{ $t('manager_tracking.open') }}: {{ assignee.counts.open }}
                          </span>
                          <span class="text-xs bg-amber-100 dark:bg-amber-900/50 text-amber-700 dark:text-amber-300 px-2 py-0.5 rounded-full">
                            {{ $t('manager_tracking.in_progress') }}: {{ assignee.counts.inProgress }}
                          </span>
                          <span class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 px-2 py-0.5 rounded-full">
                            {{ $t('manager_tracking.stand_by') }}: {{ assignee.counts.standBy }}
                          </span>
                        </div>

                        <div v-if="assignee.tickets.length > 0" class="space-y-1">
                          <button v-for="ticket in assignee.tickets" :key="ticket.id"
                            @click="openTicket(ticket)"
                            class="w-full text-left px-2 py-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700/50 transition-colors flex items-center gap-2">
                            <span class="text-xs font-mono text-blue-600 dark:text-blue-400 flex-shrink-0">{{ ticket.reference }}</span>
                            <span :class="statusChip(ticket.status)" class="text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                              {{ formatStatus(ticket.status) }}
                            </span>
                            <p class="text-xs text-gray-700 dark:text-gray-200 truncate">{{ ticket.title }}</p>
                          </button>
                        </div>
                        <p v-else class="text-xs text-gray-400 dark:text-gray-500 italic">
                          {{ $t('dashboard.empty') }}
                        </p>
                      </div>

                      <!-- Right: donut chart -->
                      <div v-if="assignee.counts.total > 0" class="flex-shrink-0 w-28 flex flex-col items-center">
                        <VueApexCharts
                          type="donut"
                          height="112"
                          width="112"
                          :options="donutChartOptions"
                          :series="donutSeries(assignee)"
                        />
                        <span class="text-xs text-gray-400 dark:text-gray-500 mt-1">
                          {{ assignee.counts.total }} {{ $t('manager_tracking.tickets') }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ── Unassigned ── -->
              <div v-if="project.unassignedTickets?.length > 0"
                class="px-4 py-3 border-t border-gray-100 dark:border-gray-700">
                <h3 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase mb-2">
                  {{ $t('manager_tracking.unassigned') }} ({{ project.unassignedTickets.length }})
                </h3>
                <div class="space-y-1">
                  <button v-for="ticket in project.unassignedTickets" :key="ticket.id"
                    @click="openTicket(ticket)"
                    class="w-full text-left px-2 py-1.5 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors flex items-center gap-2">
                    <span class="text-xs font-mono text-blue-600 dark:text-blue-400 flex-shrink-0">{{ ticket.reference }}</span>
                    <span :class="statusChip(ticket.status)" class="text-xs px-1.5 py-0.5 rounded font-medium flex-shrink-0">
                      {{ formatStatus(ticket.status) }}
                    </span>
                    <p class="text-xs text-gray-700 dark:text-gray-200 truncate">{{ ticket.title }}</p>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import VueApexCharts from 'vue3-apexcharts'
import { useThemeStore } from '../stores/theme.js'
import { managerTrackingApi } from '../services/api.js'

const router = useRouter()
const { t } = useI18n()
const themeStore = useThemeStore()

const loading = ref(true)
const projects = ref([])
const collapsedProjects = ref(new Set())
const collapsedAssignees = ref(new Set())

// ── Theme ────────────────────────────────────────────────────────────────────

const apexTheme = computed(() => themeStore.current === 'dark' ? 'dark' : 'light')

// ── Intro bar chart ───────────────────────────────────────────────────────────

function projectStatusCount(p, status) {
  const assigneeCount = p.assignees.reduce((s, a) => s + (a.counts?.[status] ?? 0), 0)
  const unassignedCount = p.unassignedTickets?.filter(t => {
    const map = { open: 'OPEN', inProgress: 'IN_PROGRESS', standBy: 'STAND_BY', resolved: 'RESOLVED' }
    return t.status === map[status]
  }).length ?? 0
  return assigneeCount + unassignedCount
}

const introSeries = computed(() => [
  { name: t('manager_tracking.open'),        data: projects.value.map(p => projectStatusCount(p, 'open')) },
  { name: t('manager_tracking.in_progress'), data: projects.value.map(p => projectStatusCount(p, 'inProgress')) },
  { name: t('manager_tracking.stand_by'),    data: projects.value.map(p => projectStatusCount(p, 'standBy')) },
])

const hasActiveTickets = computed(() =>
  introSeries.value.some(s => s.data.some(v => v > 0))
)

const introChartOptions = computed(() => ({
  chart: {
    type: 'bar',
    background: 'transparent',
    toolbar: { show: false },
    animations: { enabled: false },
    stacked: true
  },
  plotOptions: {
    bar: { borderRadius: 3, columnWidth: '45%' }
  },
  dataLabels: { enabled: false },
  xaxis: {
    categories: projects.value.map(p => p.key),
    labels: { style: { colors: apexTheme.value === 'dark' ? '#9ca3af' : '#6b7280', fontSize: '12px' } }
  },
  yaxis: { show: false },
  grid: { show: false },
  colors: ['#3B82F6', '#F59E0B', '#6B7280'],
  legend: {
    show: true,
    position: 'top',
    horizontalAlign: 'left',
    fontSize: '11px',
    labels: { colors: apexTheme.value === 'dark' ? '#d1d5db' : '#374151' },
    markers: { size: 6 }
  },
  theme: { mode: apexTheme.value },
  tooltip: {
    shared: true,
    intersect: false,
    x: {
      formatter: (_, { dataPointIndex }) => projects.value[dataPointIndex]?.name ?? ''
    }
  }
}))

// ── Per-user donut chart ──────────────────────────────────────────────────────

const donutChartOptions = computed(() => ({
  chart: {
    type: 'donut',
    background: 'transparent',
    sparkline: { enabled: true },
    animations: { enabled: false }
  },
  labels: [
    t('manager_tracking.open'),
    t('manager_tracking.in_progress'),
    t('manager_tracking.stand_by'),
  ],
  colors: ['#3B82F6', '#F59E0B', '#6B7280'],
  dataLabels: { enabled: false },
  legend: { show: false },
  tooltip: { y: { formatter: v => v } },
  plotOptions: { pie: { donut: { size: '62%' } } },
  theme: { mode: apexTheme.value }
}))

function donutSeries(assignee) {
  return [
    assignee.counts.open ?? 0,
    assignee.counts.inProgress ?? 0,
    assignee.counts.standBy ?? 0,
  ]
}

// ── Helpers ───────────────────────────────────────────────────────────────────

const STATUS_COLORS = {
  OPEN:        'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300',
  IN_PROGRESS: 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300',
  STAND_BY:    'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300',
  RESOLVED:    'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300',
  CLOSED:      'bg-gray-200 text-gray-500 dark:bg-gray-600 dark:text-gray-400',
  CANCELLED:   'bg-red-100 text-red-600 dark:bg-red-900/50 dark:text-red-300',
}

function statusChip(status) {
  return STATUS_COLORS[status] ?? 'bg-gray-100 text-gray-600'
}

const STATUS_LABELS = {
  OPEN:        'manager_tracking.open',
  IN_PROGRESS: 'manager_tracking.in_progress',
  STAND_BY:    'manager_tracking.stand_by',
  RESOLVED:    'manager_tracking.resolved',
  CLOSED:      'manager_tracking.closed',
  CANCELLED:   'manager_tracking.cancelled',
}

function formatStatus(s) { return t(STATUS_LABELS[s] ?? 'manager_tracking.open') }

function initials(assignee) {
  return ((assignee.firstName?.[0] ?? '') + (assignee.lastName?.[0] ?? '')).toUpperCase() || '?'
}

function openTicket(ticket) {
  router.push({ name: 'ticket-detail', params: { projectId: ticket.projectId, ticketId: ticket.id } })
}

function toggleProject(id) {
  const s = collapsedProjects.value
  s.has(id) ? s.delete(id) : s.add(id)
}

function toggleAssignee(id) {
  const s = collapsedAssignees.value
  s.has(id) ? s.delete(id) : s.add(id)
}

// ── Data ─────────────────────────────────────────────────────────────────────

onMounted(async () => {
  try {
    const { data } = await managerTrackingApi.get()
    projects.value = data.projects ?? []
  } finally {
    loading.value = false
  }
})
</script>
