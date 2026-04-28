<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Utilisateurs</h1>
      <span v-if="pendingCount > 0"
        class="bg-amber-100 text-amber-700 text-xs font-semibold px-3 py-1 rounded-full">
        {{ pendingCount }} en attente d'affectation
      </span>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-400">Chargement…</div>
    <div v-else class="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="text-left text-xs text-gray-500 uppercase border-b bg-gray-50">
            <th class="px-4 py-3">Nom</th>
            <th class="px-4 py-3">Email</th>
            <th class="px-4 py-3">Rôle</th>
            <th class="px-4 py-3">Organisation</th>
            <th class="px-4 py-3">Statut</th>
            <th class="px-4 py-3"></th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="u in users" :key="u.id"
            :class="['hover:bg-gray-50', isPending(u) ? 'bg-amber-50' : '']">
            <td class="px-4 py-3 font-medium text-gray-900">
              {{ u.firstName }} {{ u.lastName }}
              <span v-if="u.id === currentUserId" class="ml-1 text-xs text-gray-400">(vous)</span>
            </td>
            <td class="px-4 py-3 text-gray-500">{{ u.email }}</td>
            <td class="px-4 py-3">
              <span :class="roleBadge(u.role)">{{ u.role }}</span>
            </td>
            <td class="px-4 py-3">
              <span v-if="u.organizationName" class="text-gray-700">{{ u.organizationName }}</span>
              <span v-else-if="u.role !== 'ADMIN'"
                class="text-amber-600 text-xs font-medium">Sans organisation</span>
              <span v-else class="text-gray-400 text-xs">—</span>
            </td>
            <td class="px-4 py-3">
              <span :class="u.active ? 'text-green-600' : 'text-gray-400'">
                {{ u.active ? 'Actif' : 'Inactif' }}
              </span>
            </td>
            <td class="px-4 py-3 text-right">
              <button v-if="u.id !== currentUserId" @click="openEdit(u)"
                class="text-blue-500 hover:text-blue-700 text-xs font-medium">Modifier</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" class="px-4 py-8 text-center text-gray-400">Aucun utilisateur.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Edit modal -->
    <div v-if="editing" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg font-bold mb-1">Modifier l'utilisateur</h2>
        <p class="text-sm text-gray-500 mb-5">{{ editing.firstName }} {{ editing.lastName }}</p>

        <div class="space-y-5">
          <!-- Compte -->
          <div class="space-y-3">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Compte</p>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">Rôle</label>
              <select v-model="form.role" class="w-full border rounded-lg px-3 py-2 text-sm">
                <option value="ADMIN">ADMIN</option>
                <option value="USER">USER</option>
              </select>
            </div>
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" v-model="form.active" class="rounded" />
              Compte actif
            </label>
          </div>

          <!-- Organisation -->
          <div v-if="form.role !== 'ADMIN'" class="space-y-3">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Organisation</p>
            <div>
              <label class="block text-xs font-medium text-gray-600 mb-1">Organisation</label>
              <select v-model="form.organizationId" @change="onOrgChange(form.organizationId)"
                class="w-full border rounded-lg px-3 py-2 text-sm">
                <option :value="null">— Sans organisation —</option>
                <option v-for="org in orgs" :key="org.id" :value="org.id">{{ org.name }}</option>
              </select>
            </div>
          </div>

          <!-- Projets -->
          <div v-if="form.role !== 'ADMIN' && form.organizationId" class="space-y-3">
            <p class="text-xs font-semibold text-gray-400 uppercase tracking-wide">Projets accessibles</p>
            <div v-if="loadingOrgProjects" class="text-xs text-gray-400">Chargement…</div>
            <div v-else-if="orgProjects.length === 0" class="text-xs text-gray-400 italic">
              Aucun projet rattaché à cette organisation.
            </div>
            <div v-else class="space-y-1 max-h-56 overflow-y-auto border rounded-lg p-3">
              <div v-for="p in orgProjects" :key="p.id"
                class="flex items-center gap-2 py-1">
                <input type="checkbox"
                  :checked="isProjectSelected(p.id)"
                  @change="toggleProject(p.id)"
                  class="rounded shrink-0" />
                <span class="text-sm flex-1 min-w-0 truncate">
                  {{ p.name }}
                  <span class="text-xs text-gray-400 font-mono ml-1">{{ p.key }}</span>
                </span>
                <select v-if="isProjectSelected(p.id)"
                  :value="getProjectRole(p.id)"
                  @change="setProjectRole(p.id, $event.target.value)"
                  class="text-xs border rounded px-2 py-1 shrink-0">
                  <option v-for="r in projectRoles" :key="r.code" :value="r.code">{{ r.label }}</option>
                </select>
              </div>
            </div>
            <p class="text-xs text-gray-400">
              {{ form.projectEntries.length }} projet(s) sélectionné(s)
            </p>
          </div>
        </div>

        <p v-if="editError" class="text-sm text-red-600 mt-4">{{ editError }}</p>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="editing = null"
            class="text-sm text-gray-500 hover:text-gray-700 px-4 py-2">Annuler</button>
          <button @click="saveUser" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? '…' : 'Enregistrer' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminUsersApi, organizationsApi } from '../services/api.js'
import { useAuthStore } from '../stores/auth.js'
import { useConfigStore } from '../stores/config.js'

const auth = useAuthStore()
const config = useConfigStore()
const currentUserId = computed(() => auth.user?.id)
const projectRoles = computed(() => config.projectRoles)

const users = ref([])
const orgs = ref([])
const loading = ref(true)

const pendingCount = computed(() =>
  users.value.filter(u => isPending(u)).length
)

onMounted(async () => {
  const [{ data: u }, { data: o }] = await Promise.all([
    adminUsersApi.list(),
    organizationsApi.list()
  ])
  users.value = u
  orgs.value = o
  loading.value = false
})

function isPending(u) {
  return u.role !== 'ADMIN' && !u.organizationId
}

function roleBadge(role) {
  const map = {
    ADMIN: 'bg-purple-100 text-purple-700 text-xs px-2 py-0.5 rounded font-medium',
    USER:  'bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded font-medium',
  }
  return map[role] ?? map.USER
}

// --- Edit ---
const editing = ref(null)
const form = ref({})
const editError = ref('')
const saving = ref(false)
const orgProjects = ref([])
const loadingOrgProjects = ref(false)

function openEdit(u) {
  editing.value = u
  form.value = {
    role: u.role,
    active: u.active,
    organizationId: u.organizationId ?? null,
    projectEntries: (u.projectRoles || []).map(pr => ({ projectId: pr.projectId, role: pr.role }))
  }
  orgProjects.value = []
  editError.value = ''
  if (u.organizationId) loadOrgProjects(u.organizationId)
}

async function loadOrgProjects(orgId) {
  if (!orgId) { orgProjects.value = []; return }
  loadingOrgProjects.value = true
  try {
    const { data } = await organizationsApi.get(orgId)
    orgProjects.value = data.projects || []
  } finally {
    loadingOrgProjects.value = false
  }
}

function onOrgChange(newOrgId) {
  form.value.projectEntries = []
  loadOrgProjects(newOrgId)
}

function isProjectSelected(projectId) {
  return form.value.projectEntries.some(e => e.projectId === projectId)
}

function toggleProject(projectId) {
  const idx = form.value.projectEntries.findIndex(e => e.projectId === projectId)
  if (idx === -1) form.value.projectEntries.push({ projectId, role: 'MEMBER' })
  else form.value.projectEntries.splice(idx, 1)
}

function getProjectRole(projectId) {
  return form.value.projectEntries.find(e => e.projectId === projectId)?.role || 'MEMBER'
}

function setProjectRole(projectId, role) {
  const entry = form.value.projectEntries.find(e => e.projectId === projectId)
  if (entry) entry.role = role
}

async function saveUser() {
  editError.value = ''
  saving.value = true
  try {
    let updated = editing.value

    // 1. role + active
    if (form.value.role !== editing.value.role || form.value.active !== editing.value.active) {
      const { data } = await adminUsersApi.update(editing.value.id, {
        role: form.value.role !== editing.value.role ? form.value.role : undefined,
        active: form.value.active !== editing.value.active ? form.value.active : undefined
      })
      updated = data
    }

    // 2. org
    const orgChanged = form.value.organizationId !== (editing.value.organizationId ?? null)
    if (orgChanged && form.value.role !== 'ADMIN') {
      const { data } = await adminUsersApi.assignOrganization(editing.value.id, {
        organizationId: form.value.organizationId
      })
      updated = data
    }

    // 3. projects
    if (form.value.organizationId && form.value.role !== 'ADMIN') {
      const origMap = new Map((editing.value.projectRoles || []).map(pr => [pr.projectId, pr.role]))
      const newEntries = form.value.projectEntries || []
      const projectsChanged = orgChanged ||
        origMap.size !== newEntries.length ||
        newEntries.some(e => origMap.get(e.projectId) !== e.role)
      if (projectsChanged) {
        const { data } = await adminUsersApi.updateProjects(editing.value.id, {
          entries: newEntries
        })
        updated = data
      }
    }

    const idx = users.value.findIndex(u => u.id === editing.value.id)
    if (idx !== -1) users.value.splice(idx, 1, updated)
    editing.value = null
  } catch (e) {
    editError.value = e.response?.data?.detail || 'Erreur lors de la mise à jour'
  } finally {
    saving.value = false
  }
}
</script>
