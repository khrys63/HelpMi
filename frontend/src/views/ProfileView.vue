<template>
  <div class="max-w-2xl mx-auto">

    <!-- Infos utilisateur -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900 mb-4">Mon profil</h1>
      <div class="bg-white border border-gray-200 rounded-xl p-5 flex items-center gap-4">
        <span class="w-12 h-12 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-lg">
          {{ initials }}
        </span>
        <div>
          <p class="font-semibold text-gray-900">{{ auth.user?.firstName }} {{ auth.user?.lastName }}</p>
          <p class="text-sm text-gray-500">{{ auth.user?.email }}</p>
          <span class="text-xs text-gray-400 uppercase">{{ auth.user?.role }}</span>
        </div>
      </div>
    </div>

    <!-- Tokens API -->
    <div>
      <div class="flex items-center justify-between mb-3">
        <div>
          <h2 class="text-lg font-bold text-gray-900">Tokens API</h2>
          <p class="text-sm text-gray-500">Permettent d'appeler l'API HelpMi sans passer par Keycloak.</p>
        </div>
        <button @click="showForm = !showForm"
          class="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700">
          + Nouveau token
        </button>
      </div>

      <!-- Formulaire création -->
      <div v-if="showForm" class="bg-gray-50 border border-gray-200 rounded-xl p-4 mb-4 space-y-3">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Nom du token</label>
          <input v-model="form.name" type="text" placeholder="ex : Script de synchro"
            class="w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Expiration (optionnel)</label>
          <input v-model="form.expiresAt" type="datetime-local"
            class="border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>
        <div class="flex gap-2">
          <button @click="createToken" :disabled="!form.name.trim()"
            class="bg-blue-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            Créer
          </button>
          <button @click="showForm = false; form.name = ''; form.expiresAt = ''"
            class="text-sm text-gray-500 hover:text-gray-700 px-3 py-1.5">
            Annuler
          </button>
        </div>
      </div>

      <!-- Token affiché une seule fois -->
      <div v-if="newToken"
        class="bg-green-50 border border-green-300 rounded-xl p-4 mb-4">
        <p class="text-sm font-semibold text-green-800 mb-2">
          Token créé — copiez-le maintenant, il ne sera plus affiché.
        </p>
        <div class="flex items-center gap-2">
          <code class="flex-1 bg-white border border-green-200 rounded-lg px-3 py-2 text-xs font-mono text-gray-800 break-all">
            {{ newToken.plainToken }}
          </code>
          <button @click="copyToken(newToken.plainToken)"
            class="shrink-0 text-xs text-green-700 border border-green-300 rounded-lg px-3 py-2 hover:bg-green-100">
            {{ copied ? 'Copié !' : 'Copier' }}
          </button>
        </div>
        <p class="text-xs text-green-600 mt-2">
          Utilisez-le dans vos requêtes : <code class="bg-green-100 px-1 rounded">Authorization: Bearer {{ newToken.plainToken.slice(0, 12) }}…</code>
        </p>
        <button @click="newToken = null; copied = false" class="mt-3 text-xs text-green-700 underline">
          Fermer
        </button>
      </div>

      <!-- Liste des tokens -->
      <div v-if="tokens.length === 0 && !loading" class="text-sm text-gray-400 py-4 text-center">
        Aucun token créé.
      </div>
      <div v-else class="space-y-2">
        <div v-for="t in tokens" :key="t.id"
          class="bg-white border border-gray-200 rounded-xl px-4 py-3 flex items-center gap-4">
          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-900 text-sm">{{ t.name }}</p>
            <p class="text-xs text-gray-400 mt-0.5">
              Créé le {{ formatDate(t.createdAt) }}
              <span v-if="t.lastUsedAt"> · Dernière utilisation {{ formatDate(t.lastUsedAt) }}</span>
              <span v-if="t.expiresAt" :class="isExpired(t.expiresAt) ? 'text-red-500' : 'text-gray-400'">
                · Expire le {{ formatDate(t.expiresAt) }}
              </span>
            </p>
          </div>
          <span v-if="isExpired(t.expiresAt)"
            class="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full shrink-0">
            Expiré
          </span>
          <button @click="deleteToken(t.id)"
            class="shrink-0 text-xs text-gray-400 hover:text-red-600 transition-colors">
            Révoquer
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { personalTokensApi } from '../services/api.js'

const auth = useAuthStore()

const tokens = ref([])
const loading = ref(true)
const showForm = ref(false)
const newToken = ref(null)
const copied = ref(false)
const form = ref({ name: '', expiresAt: '' })

const initials = computed(() => {
  if (!auth.user) return '?'
  return ((auth.user.firstName?.[0] || '') + (auth.user.lastName?.[0] || '')).toUpperCase()
})

onMounted(async () => {
  const { data } = await personalTokensApi.list()
  tokens.value = data
  loading.value = false
})

async function createToken() {
  const payload = { name: form.value.name.trim() }
  if (form.value.expiresAt) payload.expiresAt = form.value.expiresAt
  const { data } = await personalTokensApi.create(payload)
  newToken.value = data
  tokens.value.unshift({ id: data.id, name: data.name, createdAt: data.createdAt, lastUsedAt: null, expiresAt: data.expiresAt })
  showForm.value = false
  form.value = { name: '', expiresAt: '' }
}

async function deleteToken(id) {
  await personalTokensApi.remove(id)
  tokens.value = tokens.value.filter(t => t.id !== id)
}

async function copyToken(token) {
  await navigator.clipboard.writeText(token)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

function isExpired(expiresAt) {
  return expiresAt && new Date(expiresAt) < new Date()
}
</script>
