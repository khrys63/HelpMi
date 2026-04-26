<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Projets</h1>
      <button v-if="isAdmin" @click="showCreate = true"
        class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
        + Nouveau projet
      </button>
    </div>

    <div v-if="loading" class="text-center py-12 text-gray-400">Chargement…</div>
    <div v-else-if="projects.length === 0" class="text-center py-12 text-gray-400">Aucun projet.</div>
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <router-link v-for="p in projects" :key="p.id"
        :to="`/projects/${p.id}`"
        class="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow">
        <div class="flex items-center gap-2 mb-2">
          <span class="bg-blue-100 text-blue-700 text-xs font-bold px-2 py-0.5 rounded">{{ p.key }}</span>
          <span class="font-semibold text-gray-900 truncate">{{ p.name }}</span>
        </div>
        <p class="text-sm text-gray-500 line-clamp-2 mb-3">{{ p.description || 'Pas de description' }}</p>
        <p class="text-xs text-gray-400">{{ p.ticketCount }} ticket(s)</p>
      </router-link>
    </div>

    <!-- Modal création projet -->
    <div v-if="showCreate" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div class="bg-white rounded-xl shadow-xl p-6 w-full max-w-md">
        <h2 class="text-lg font-bold mb-4">Nouveau projet</h2>
        <div class="space-y-3">
          <input v-model="form.name" placeholder="Nom du projet" class="w-full border rounded-lg px-3 py-2 text-sm" />
          <input v-model="form.key" placeholder="Clé (ex: PROJ)" maxlength="10"
            class="w-full border rounded-lg px-3 py-2 text-sm uppercase" />
          <textarea v-model="form.description" placeholder="Description (optionnelle)" rows="3"
            class="w-full border rounded-lg px-3 py-2 text-sm resize-none" />
        </div>
        <div class="flex justify-end gap-2 mt-4">
          <button @click="showCreate = false" class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900">Annuler</button>
          <button @click="createProject" :disabled="saving"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ saving ? 'Création…' : 'Créer' }}
          </button>
        </div>
        <p v-if="error" class="text-sm text-red-600 mt-2">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { projectsApi } from '../services/api.js'
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()
const projects = ref([])
const loading = ref(true)
const showCreate = ref(false)
const saving = ref(false)
const error = ref('')
const form = ref({ name: '', key: '', description: '' })

const isAdmin = computed(() => auth.user?.role === 'ADMIN')

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
    error.value = e.response?.data?.detail || 'Erreur lors de la création'
  } finally {
    saving.value = false
  }
}
</script>
