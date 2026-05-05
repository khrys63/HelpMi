<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-gray-100">{{ $t('projects.title') }}</h1>
      <button v-if="isAdmin" @click="showCreate = true"
        class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
        {{ $t('projects.new') }}
      </button>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-400">{{ $t('common.loading') }}</div>
    <div v-else-if="projects.length === 0" class="text-center py-12 text-gray-400">{{ $t('projects.no_projects') }}</div>
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="p in projects" :key="p.id" class="relative group">
        <router-link :to="`/projects/${p.id}`"
          class="block bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 hover:shadow-md transition-shadow">
          <div class="flex items-start gap-2 mb-1.5">
            <span class="bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 text-xs font-bold px-2 py-0.5 rounded shrink-0">{{ p.key }}</span>
            <span class="font-semibold text-gray-900 dark:text-gray-100 truncate flex-1">{{ p.name }}</span>
            <span v-if="p.userRole"
              :class="['text-xs font-medium px-2 py-0.5 rounded-full shrink-0',
                       p.userRole === 'MANAGER'
                         ? 'bg-violet-100 dark:bg-violet-900/40 text-violet-700 dark:text-violet-300'
                         : 'bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400']">
              {{ p.userRole === 'MANAGER' ? $t('projects.role_manager') : $t('projects.role_member') }}
            </span>
          </div>
          <p class="text-sm text-gray-500 dark:text-gray-400 line-clamp-2 mb-3">{{ p.description || $t('common.no_description') }}</p>
          <div class="flex items-center justify-between gap-2">
            <div class="flex flex-wrap gap-1 min-w-0">
              <span v-for="org in p.organizations" :key="org"
                class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 px-1.5 py-0.5 rounded truncate max-w-[120px]">
                {{ org }}
              </span>
            </div>
            <p class="text-xs text-gray-400 dark:text-gray-500 shrink-0">{{ $t('projects.ticket_count', { count: p.ticketCount }) }}</p>
          </div>
        </router-link>
        <button v-if="isAdmin" @click.prevent="openEdit(p)"
          class="absolute top-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity text-gray-400 hover:text-blue-600 text-xs font-medium bg-white dark:bg-gray-700 rounded px-2 py-1 shadow-sm border border-gray-200 dark:border-gray-600">
          {{ $t('common.edit') }}
        </button>
      </div>
    </div>

    <!-- Modal création projet -->
    <div v-if="showCreate" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-md">
        <h2 class="text-lg font-bold mb-4 dark:text-gray-100">{{ $t('projects.create_title') }}</h2>
        <div class="space-y-3">
          <input ref="nameInput" v-model="form.name" :placeholder="$t('projects.field_name')" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
          <input v-model="form.key" :placeholder="$t('projects.field_key')" maxlength="10"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm uppercase dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
          <textarea v-model="form.description" :placeholder="$t('projects.field_description')" rows="3"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm resize-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
        </div>
        <div class="flex justify-end gap-2 mt-4">
          <button @click="showCreate = false" class="px-4 py-2 text-sm text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100">{{ $t('common.cancel') }}</button>
          <button @click="createProject" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? $t('projects.creating') : $t('common.create') }}
          </button>
        </div>
        <p v-if="error" class="text-sm text-red-600 mt-2">{{ error }}</p>
      </div>
    </div>

    <!-- Modal édition projet -->
    <div v-if="editing" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-md">
        <h2 class="text-lg font-bold mb-1 dark:text-gray-100">{{ $t('projects.edit_title') }}</h2>
        <p class="text-xs text-gray-400 font-mono mb-4">{{ editing.key }}</p>
        <div class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('projects.field_name_label') }}</label>
            <input ref="editNameInput" v-model="editForm.name" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100" />
          </div>
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('projects.field_description_label') }}</label>
            <textarea v-model="editForm.description" rows="3"
              class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm resize-none dark:bg-gray-700 dark:text-gray-100" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-4">
          <button @click="editing = null" class="px-4 py-2 text-sm text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-gray-100">{{ $t('common.cancel') }}</button>
          <button @click="saveEdit" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? $t('common.saving') : $t('common.save') }}
          </button>
        </div>
        <p v-if="error" class="text-sm text-red-600 mt-2">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { projectsApi } from '../services/api.js'
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()
const projects = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')

const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const showCreate = ref(false)
const form = ref({ name: '', key: '', description: '' })
const nameInput = ref(null)

watch(showCreate, (val) => {
  if (val) nextTick(() => nameInput.value?.focus())
})

onMounted(async () => {
  const { data } = await projectsApi.list()
  projects.value = data
  loading.value = false
})

async function createProject() {
  error.value = ''
  saving.value = true
  try {
    const { data } = await projectsApi.create({ ...form.value, key: form.value.key.toUpperCase() })
    projects.value.unshift(data)
    showCreate.value = false
    form.value = { name: '', key: '', description: '' }
  } catch (e) {
    error.value = e.response?.data?.detail || e.message
  } finally {
    saving.value = false
  }
}

const editing = ref(null)
const editForm = ref({ name: '', description: '' })
const editNameInput = ref(null)

function openEdit(p) {
  editing.value = p
  editForm.value = { name: p.name, description: p.description || '' }
  error.value = ''
  nextTick(() => editNameInput.value?.focus())
}

async function saveEdit() {
  error.value = ''
  saving.value = true
  try {
    const { data } = await projectsApi.update(editing.value.id, editForm.value)
    const idx = projects.value.findIndex(p => p.id === editing.value.id)
    if (idx !== -1) projects.value.splice(idx, 1, data)
    editing.value = null
  } catch (e) {
    error.value = e.response?.data?.detail || e.message
  } finally {
    saving.value = false
  }
}
</script>
