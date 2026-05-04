<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-gray-100">{{ $t('admin.orgs.title') }}</h1>
      <button @click="openCreate"
        class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
        {{ $t('admin.orgs.new') }}
      </button>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-400">{{ $t('common.loading') }}</div>
    <div v-else-if="orgs.length === 0" class="text-center py-12 text-gray-400">{{ $t('admin.orgs.no_orgs') }}</div>

    <div v-else class="space-y-4">
      <div v-for="org in orgs" :key="org.id"
        class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
        <div class="flex items-start justify-between">
          <div>
            <div class="flex items-center gap-2 mb-1">
              <span class="font-semibold text-gray-900 dark:text-gray-100">{{ org.name }}</span>
              <span v-if="!org.active"
                class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-500 dark:text-gray-400 px-2 py-0.5 rounded">{{ $t('admin.orgs.inactive') }}</span>
            </div>
            <p class="text-xs text-gray-400 dark:text-gray-500">
              {{ $t('admin.orgs.users_projects', { users: org.users.length, projects: org.projects.length }) }}
            </p>
          </div>
          <div class="flex gap-2">
            <button @click="openManage(org)"
              class="text-sm text-blue-600 hover:text-blue-800 font-medium">{{ $t('admin.orgs.manage') }}</button>
            <button @click="openEdit(org)"
              class="text-sm text-gray-500 hover:text-gray-800">{{ $t('common.edit') }}</button>
            <button @click="confirmDelete(org)"
              class="text-sm text-red-400 hover:text-red-600">{{ $t('common.delete') }}</button>
          </div>
        </div>

        <!-- Inline summary -->
        <div v-if="org.users.length > 0 || org.projects.length > 0" class="mt-3 flex flex-wrap gap-3">
          <div v-if="org.projects.length > 0" class="flex flex-wrap gap-1">
            <span v-for="p in org.projects" :key="p.id"
              class="bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 text-xs px-2 py-0.5 rounded font-mono">{{ p.key }}</span>
          </div>
          <div v-if="org.users.length > 0" class="flex flex-wrap gap-1">
            <span v-for="u in org.users" :key="u.id"
              class="bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 text-xs px-2 py-0.5 rounded">
              {{ u.firstName }} {{ u.lastName }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Create / Edit modal -->
    <div v-if="modal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-sm">
        <h2 class="text-lg font-bold mb-4 dark:text-gray-100">{{ editingOrg ? $t('admin.orgs.edit_title') : $t('admin.orgs.create_title') }}</h2>
        <div class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">{{ $t('admin.orgs.field_name') }}</label>
            <input ref="nameInput" v-model="form.name" :placeholder="$t('admin.orgs.field_name_placeholder')"
              class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
          </div>
          <label v-if="editingOrg" class="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" v-model="form.active" class="rounded" /> {{ $t('admin.orgs.field_active') }}
          </label>
        </div>
        <p v-if="modalError" class="text-sm text-red-600 mt-3">{{ modalError }}</p>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="modal = false" class="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 px-4 py-2">{{ $t('common.cancel') }}</button>
          <button @click="saveOrg" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? $t('common.saving') : (editingOrg ? $t('common.save') : $t('common.create')) }}
          </button>
        </div>
      </div>
    </div>

    <!-- Manage access modal -->
    <div v-if="managing" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-lg max-h-[80vh] overflow-y-auto">
        <h2 class="text-lg font-bold mb-4 dark:text-gray-100">{{ $t('admin.orgs.manage_title', { name: managing.name }) }}</h2>

        <!-- Projects -->
        <div class="mb-5">
          <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">{{ $t('admin.orgs.projects_section') }}</h3>
          <div class="flex flex-wrap gap-2 mb-2">
            <span v-for="p in managing.projects" :key="p.id"
              class="inline-flex items-center gap-1 bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 text-xs px-2 py-1 rounded">
              {{ p.key }} — {{ p.name }}
              <button @click="removeProject(p.id)" class="hover:text-red-500 ml-1">×</button>
            </span>
            <span v-if="managing.projects.length === 0" class="text-xs text-gray-400 dark:text-gray-500">{{ $t('admin.orgs.no_projects') }}</span>
          </div>
          <div class="flex gap-2">
            <select v-model="selectedProjectId"
              class="flex-1 border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option value="">{{ $t('admin.orgs.add_project') }}</option>
              <option v-for="p in availableProjects" :key="p.id" :value="p.id">
                {{ p.key }} — {{ p.name }}
              </option>
            </select>
            <button @click="addProject" :disabled="!selectedProjectId"
              class="bg-blue-600 text-white px-3 py-2 rounded-lg text-sm disabled:opacity-40">{{ $t('common.add') }}</button>
          </div>
        </div>

        <!-- Users -->
        <div>
          <h3 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">{{ $t('admin.orgs.users_section') }}</h3>
          <div class="flex flex-wrap gap-2 mb-2">
            <span v-for="u in managing.users" :key="u.id"
              class="inline-flex items-center gap-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 text-xs px-2 py-1 rounded">
              {{ u.firstName }} {{ u.lastName }}
              <span class="text-gray-400 dark:text-gray-500">({{ u.role }})</span>
              <button @click="removeUser(u.id)" class="hover:text-red-500 ml-1">×</button>
            </span>
            <span v-if="managing.users.length === 0" class="text-xs text-gray-400 dark:text-gray-500">{{ $t('admin.orgs.no_users') }}</span>
          </div>
          <div class="flex gap-2">
            <select v-model="selectedUserId"
              class="flex-1 border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option value="">{{ $t('admin.orgs.add_user') }}</option>
              <option v-for="u in availableUsers" :key="u.id" :value="u.id">
                {{ u.firstName }} {{ u.lastName }} ({{ u.role }})
              </option>
            </select>
            <button @click="addUser" :disabled="!selectedUserId"
              class="bg-blue-600 text-white px-3 py-2 rounded-lg text-sm disabled:opacity-40">{{ $t('common.add') }}</button>
          </div>
        </div>

        <p v-if="manageError" class="text-sm text-red-600 mt-3">{{ manageError }}</p>
        <div class="flex justify-end mt-5">
          <button @click="managing = null"
            class="text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 px-4 py-2">{{ $t('common.close') }}</button>
        </div>
      </div>
    </div>

    <!-- Delete confirmation -->
    <div v-if="deleteTarget" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 w-full max-w-sm">
        <h2 class="text-lg font-bold text-red-700 dark:text-red-400 mb-2">{{ $t('admin.orgs.delete_title') }}</h2>
        <p class="text-sm text-gray-700 dark:text-gray-300 mb-4">
          {{ $t('admin.orgs.delete_body', { name: deleteTarget.name }) }}
        </p>
        <p v-if="deleteError" class="text-sm text-red-600 mb-3">{{ deleteError }}</p>
        <div class="flex justify-end gap-2">
          <button @click="deleteTarget = null; deleteError = ''"
            class="text-sm text-gray-500 dark:text-gray-400 px-4 py-2">{{ $t('common.cancel') }}</button>
          <button @click="doDelete" :disabled="deleting"
            class="bg-red-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50">
            {{ deleting ? $t('common.saving') : $t('common.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { organizationsApi, projectsApi, usersApi } from '../services/api.js'

const orgs = ref([])
const loading = ref(true)
const allProjects = ref([])
const allUsers = ref([])

onMounted(async () => {
  await reload()
  const [{ data: projects }, { data: users }] = await Promise.all([
    projectsApi.list(),
    usersApi.list()
  ])
  allProjects.value = projects
  allUsers.value = users
  loading.value = false
})

async function reload() {
  const { data } = await organizationsApi.list()
  orgs.value = data
}

// --- Create / Edit ---
const modal = ref(false)
const editingOrg = ref(null)
const form = ref({ name: '', active: true })
const modalError = ref('')
const saving = ref(false)
const nameInput = ref(null)

watch(modal, val => { if (val) nextTick(() => nameInput.value?.focus()) })

function openCreate() {
  editingOrg.value = null
  form.value = { name: '', active: true }
  modalError.value = ''
  modal.value = true
}
function openEdit(org) {
  editingOrg.value = org
  form.value = { name: org.name, active: org.active }
  modalError.value = ''
  modal.value = true
}
async function saveOrg() {
  modalError.value = ''
  saving.value = true
  try {
    if (editingOrg.value) {
      const { data } = await organizationsApi.update(editingOrg.value.id, form.value)
      replaceOrg(data)
    } else {
      const { data } = await organizationsApi.create({ name: form.value.name })
      orgs.value.push(data)
    }
    modal.value = false
  } catch (e) {
    modalError.value = e.response?.data?.detail || 'Erreur'
  } finally {
    saving.value = false
  }
}

// --- Manage access ---
const managing = ref(null)
const selectedProjectId = ref('')
const selectedUserId = ref('')
const manageError = ref('')

const availableProjects = computed(() => {
  if (!managing.value) return []
  const assignedIds = new Set(managing.value.projects.map(p => p.id))
  return allProjects.value.filter(p => !assignedIds.has(p.id))
})
const availableUsers = computed(() => {
  if (!managing.value) return []
  const assignedIds = new Set(managing.value.users.map(u => u.id))
  return allUsers.value.filter(u => !assignedIds.has(u.id))
})

function openManage(org) {
  managing.value = { ...org, projects: [...org.projects], users: [...org.users] }
  selectedProjectId.value = ''
  selectedUserId.value = ''
  manageError.value = ''
}
async function addProject() {
  if (!selectedProjectId.value) return
  try {
    const { data } = await organizationsApi.addProject(managing.value.id, selectedProjectId.value)
    managing.value = data
    replaceOrg(data)
    selectedProjectId.value = ''
  } catch (e) { manageError.value = e.response?.data?.detail || 'Erreur' }
}
async function removeProject(projectId) {
  try {
    const { data } = await organizationsApi.removeProject(managing.value.id, projectId)
    managing.value = data
    replaceOrg(data)
  } catch (e) { manageError.value = e.response?.data?.detail || 'Erreur' }
}
async function addUser() {
  if (!selectedUserId.value) return
  try {
    const { data } = await organizationsApi.addUser(managing.value.id, selectedUserId.value)
    managing.value = data
    replaceOrg(data)
    selectedUserId.value = ''
  } catch (e) { manageError.value = e.response?.data?.detail || 'Erreur' }
}
async function removeUser(userId) {
  try {
    const { data } = await organizationsApi.removeUser(managing.value.id, userId)
    managing.value = data
    replaceOrg(data)
  } catch (e) { manageError.value = e.response?.data?.detail || 'Erreur' }
}

// --- Delete ---
const deleteTarget = ref(null)
const deleteError = ref('')
const deleting = ref(false)

function confirmDelete(org) { deleteTarget.value = org; deleteError.value = '' }
async function doDelete() {
  deleting.value = true
  deleteError.value = ''
  try {
    await organizationsApi.remove(deleteTarget.value.id)
    orgs.value = orgs.value.filter(o => o.id !== deleteTarget.value.id)
    deleteTarget.value = null
  } catch (e) {
    deleteError.value = e.response?.data?.detail || 'Impossible de supprimer'
  } finally {
    deleting.value = false
  }
}

function replaceOrg(updated) {
  const idx = orgs.value.findIndex(o => o.id === updated.id)
  if (idx !== -1) orgs.value.splice(idx, 1, updated)
}
</script>
