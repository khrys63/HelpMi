<template>
  <div class="max-w-2xl mx-auto">
    <div class="flex items-center gap-2 mb-6 text-sm text-gray-500 dark:text-gray-400">
      <router-link :to="`/projects/${projectId}`" class="hover:text-blue-600 dark:hover:text-blue-400">{{ $t('common.back') }}</router-link>
      <span>/</span>
      <span class="text-gray-700 dark:text-gray-300 font-medium">{{ $t('tickets.new_ticket') }}</span>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
      <h1 class="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">{{ $t('tickets.create_title') }}</h1>

      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('tickets.field_title') }}</label>
          <input ref="titleInput" v-model="form.title" :placeholder="$t('tickets.field_title_placeholder')"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('tickets.field_type') }}</label>
            <select v-model="form.type" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option v-for="t in config.types" :key="t.code" :value="t.code">{{ t.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('tickets.field_priority') }}</label>
            <select v-model="form.priority" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option v-for="p in config.priorities" :key="p.code" :value="p.code">{{ p.label }}</option>
            </select>
          </div>
        </div>

        <div v-if="canAssign">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('tickets.field_assignee') }}</label>
          <select v-model="form.assigneeId" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
            <option value="">{{ $t('tickets.field_assignee_none') }}</option>
            <option v-for="u in users" :key="u.id" :value="u.id">
              {{ u.firstName }} {{ u.lastName }} ({{ u.role }})
            </option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('tickets.field_description') }}</label>
          <textarea v-model="form.description" rows="6" :placeholder="$t('tickets.field_description_placeholder')"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm resize-none focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('tickets.field_due_date') }} <span class="text-gray-400 dark:text-gray-500 font-normal">({{ $t('common.optional') }})</span></label>
          <input type="date" v-model="form.dueDate"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100" />
        </div>
      </div>

      <p v-if="error" class="text-sm text-red-600 mt-3">{{ error }}</p>

      <div class="flex justify-end gap-3 mt-6">
        <router-link :to="`/projects/${projectId}`"
          class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200">{{ $t('common.cancel') }}</router-link>
        <button @click="submit" :disabled="saving || !form.title.trim()"
          class="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
          {{ saving ? $t('tickets.creating') : $t('tickets.new_ticket') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ticketsApi, usersApi, projectsApi } from '../services/api.js'
import { useConfigStore } from '../stores/config.js'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId
const config = useConfigStore()

const project = ref(null)
const canAssign = computed(() => project.value?.canAssign ?? false)
const users = ref([])
const saving = ref(false)
const error = ref('')
const form = ref({ title: '', description: '', type: '', priority: '', assigneeId: '', dueDate: '' })
const titleInput = ref(null)

onMounted(async () => {
  nextTick(() => titleInput.value?.focus())
  const { data } = await projectsApi.get(projectId)
  project.value = data
  if (canAssign.value) {
    const { data: usersData } = await usersApi.assignable(projectId)
    users.value = usersData
  }
  const firstType = config.types.find(t => t.active)
  const firstPriority = config.priorities.find(p => p.active)
  if (firstType) form.value.type = firstType.code
  if (firstPriority) form.value.priority = firstPriority.code
})

async function submit() {
  error.value = ''
  saving.value = true
  try {
    const payload = { ...form.value }
    if (!payload.assigneeId) delete payload.assigneeId
    if (!payload.dueDate) delete payload.dueDate
    const { data } = await ticketsApi.create(projectId, payload)
    router.push(`/projects/${projectId}/tickets/${data.id}`)
  } catch (e) {
    error.value = e.response?.data?.detail || 'Erreur lors de la création'
  } finally {
    saving.value = false
  }
}
</script>
