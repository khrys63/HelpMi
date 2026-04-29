<template>
  <div class="max-w-2xl mx-auto">
    <div class="flex items-center gap-2 mb-6 text-sm text-gray-500 dark:text-gray-400">
      <router-link :to="`/projects/${projectId}`" class="hover:text-blue-600 dark:hover:text-blue-400">← Retour</router-link>
      <span>/</span>
      <span class="text-gray-700 dark:text-gray-300 font-medium">Nouveau ticket</span>
    </div>

    <div class="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
      <h1 class="text-xl font-bold text-gray-900 dark:text-gray-100 mb-6">Créer un ticket</h1>

      <div class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Titre *</label>
          <input ref="titleInput" v-model="form.title" placeholder="Décrivez le problème en une phrase…"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
            <select v-model="form.type" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option v-for="t in config.types" :key="t.code" :value="t.code">{{ t.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Priorité</label>
            <select v-model="form.priority" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
              <option v-for="p in config.priorities" :key="p.code" :value="p.code">{{ p.label }}</option>
            </select>
          </div>
        </div>

        <div v-if="canAssign">
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Assigné à</label>
          <select v-model="form.assigneeId" class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm dark:bg-gray-700 dark:text-gray-100">
            <option value="">Non assigné</option>
            <option v-for="u in users" :key="u.id" :value="u.id">
              {{ u.firstName }} {{ u.lastName }} ({{ u.role }})
            </option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Description</label>
          <textarea v-model="form.description" rows="6" placeholder="Étapes pour reproduire, comportement attendu…"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm resize-none focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100 dark:placeholder-gray-400" />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Échéance <span class="text-gray-400 dark:text-gray-500 font-normal">(facultative)</span></label>
          <input type="date" v-model="form.dueDate"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none dark:bg-gray-700 dark:text-gray-100" />
        </div>
      </div>

      <p v-if="error" class="text-sm text-red-600 mt-3">{{ error }}</p>

      <div class="flex justify-end gap-3 mt-6">
        <router-link :to="`/projects/${projectId}`"
          class="px-4 py-2 text-sm text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200">Annuler</router-link>
        <button @click="submit" :disabled="saving || !form.title.trim()"
          class="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
          {{ saving ? 'Création…' : 'Créer le ticket' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ticketsApi, usersApi } from '../services/api.js'
import { useConfigStore } from '../stores/config.js'
import { useAuthStore } from '../stores/auth.js'

const route = useRoute()
const router = useRouter()
const projectId = route.params.projectId
const config = useConfigStore()
const auth = useAuthStore()

const canAssign = computed(() => auth.user?.role === 'ADMIN' || auth.user?.role === 'USER')
const users = ref([])
const saving = ref(false)
const error = ref('')
const form = ref({ title: '', description: '', type: '', priority: '', assigneeId: '', dueDate: '' })
const titleInput = ref(null)

onMounted(async () => {
  nextTick(() => titleInput.value?.focus())
  if (canAssign.value) {
    const { data } = await usersApi.assignable(projectId)
    users.value = data
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
