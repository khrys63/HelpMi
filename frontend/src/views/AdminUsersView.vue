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
    <div v-if="editing" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-sm">
        <h2 class="text-lg font-bold mb-1">Modifier l'utilisateur</h2>
        <p class="text-sm text-gray-500 mb-4">{{ editing.firstName }} {{ editing.lastName }}</p>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">Rôle</label>
            <select v-model="form.role" class="w-full border rounded-lg px-3 py-2 text-sm">
              <option value="ADMIN">ADMIN</option>
              <option value="AGENT">AGENT</option>
              <option value="CLIENT">CLIENT</option>
            </select>
          </div>

          <div v-if="form.role !== 'ADMIN'">
            <label class="block text-xs font-medium text-gray-600 mb-1">Organisation</label>
            <select v-model="form.organizationId" class="w-full border rounded-lg px-3 py-2 text-sm">
              <option :value="null">— Sans organisation —</option>
              <option v-for="org in orgs" :key="org.id" :value="org.id">{{ org.name }}</option>
            </select>
          </div>

          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input type="checkbox" v-model="form.active" class="rounded" />
            Compte actif
          </label>
        </div>

        <p v-if="editError" class="text-sm text-red-600 mt-3">{{ editError }}</p>
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

const auth = useAuthStore()
const currentUserId = computed(() => auth.user?.id)

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
    AGENT: 'bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded font-medium',
    CLIENT: 'bg-gray-100 text-gray-600 text-xs px-2 py-0.5 rounded font-medium'
  }
  return map[role] ?? map.CLIENT
}

// --- Edit ---
const editing = ref(null)
const form = ref({})
const editError = ref('')
const saving = ref(false)

function openEdit(u) {
  editing.value = u
  form.value = { role: u.role, organizationId: u.organizationId ?? null, active: u.active }
  editError.value = ''
}

async function saveUser() {
  editError.value = ''
  saving.value = true
  try {
    let updated = editing.value

    // Update role + active if changed
    if (form.value.role !== editing.value.role || form.value.active !== editing.value.active) {
      const { data } = await adminUsersApi.update(editing.value.id, {
        role: form.value.role !== editing.value.role ? form.value.role : undefined,
        active: form.value.active !== editing.value.active ? form.value.active : undefined
      })
      updated = data
    }

    // Update org if changed
    const orgChanged = form.value.organizationId !== (editing.value.organizationId ?? null)
    if (orgChanged && form.value.role !== 'ADMIN') {
      const { data } = await adminUsersApi.assignOrganization(editing.value.id, {
        organizationId: form.value.organizationId
      })
      updated = data
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
