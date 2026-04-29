<template>
  <div class="max-w-2xl mx-auto">

    <!-- Infos utilisateur -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-4">{{ $t('profile.title') }}</h1>
      <div class="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-5 flex items-center gap-4">
        <span class="w-12 h-12 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold text-lg">
          {{ initials }}
        </span>
        <div>
          <p class="font-semibold text-gray-900 dark:text-gray-100">{{ auth.user?.firstName }} {{ auth.user?.lastName }}</p>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ auth.user?.email }}</p>
          <span class="text-xs text-gray-400 dark:text-gray-500 uppercase">{{ auth.user?.role }}</span>
        </div>
      </div>
    </div>

    <!-- Préférences -->
    <div class="mb-8">
      <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100 mb-3">{{ $t('profile.preferences_title') }}</h2>
      <div class="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl p-5 space-y-4">
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('profile.pref_theme') }}</span>
          <div class="flex gap-2">
            <button @click="theme.toggle()"
              :class="['px-3 py-1.5 rounded-lg text-sm font-medium transition-colors', theme.current === 'light' ? 'bg-blue-600 text-white' : 'border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700']">
              ☀️ {{ $t('profile.pref_theme_light') }}
            </button>
            <button @click="theme.toggle()"
              :class="['px-3 py-1.5 rounded-lg text-sm font-medium transition-colors', theme.current === 'dark' ? 'bg-blue-600 text-white' : 'border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700']">
              🌙 {{ $t('profile.pref_theme_dark') }}
            </button>
          </div>
        </div>
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">{{ $t('profile.pref_locale') }}</span>
          <div class="flex gap-2">
            <button @click="locale.set('fr')"
              :class="['px-3 py-1.5 rounded-lg text-sm font-medium transition-colors', locale.current === 'fr' ? 'bg-blue-600 text-white' : 'border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700']">
              🇫🇷 Français
            </button>
            <button @click="locale.set('en')"
              :class="['px-3 py-1.5 rounded-lg text-sm font-medium transition-colors', locale.current === 'en' ? 'bg-blue-600 text-white' : 'border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700']">
              🇬🇧 English
            </button>
            <button @click="locale.set('bg')"
              :class="['px-3 py-1.5 rounded-lg text-sm font-medium transition-colors', locale.current === 'bg' ? 'bg-blue-600 text-white' : 'border border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700']">
              🇧🇬 български
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Tokens API -->
    <div>
      <div class="flex items-center justify-between mb-3">
        <div>
          <h2 class="text-lg font-bold text-gray-900 dark:text-gray-100">{{ $t('profile.tokens_title') }}</h2>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ $t('profile.tokens_desc') }}</p>
        </div>
        <button @click="showForm = !showForm"
          class="bg-blue-600 text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700">
          {{ $t('profile.new_token') }}
        </button>
      </div>

      <!-- Formulaire création -->
      <div v-if="showForm" class="bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl p-4 mb-4 space-y-3">
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('profile.token_name_label') }}</label>
          <input v-model="form.name" type="text" :placeholder="$t('profile.token_name_placeholder')"
            class="w-full border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-800 dark:text-gray-100 dark:placeholder-gray-400" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{{ $t('profile.token_expires_label') }}</label>
          <input v-model="form.expiresAt" type="datetime-local"
            class="border dark:border-gray-600 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-800 dark:text-gray-100" />
        </div>
        <div class="flex gap-2">
          <button @click="createToken" :disabled="!form.name.trim()"
            class="bg-blue-600 text-white px-4 py-1.5 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {{ $t('profile.token_create') }}
          </button>
          <button @click="showForm = false; form.name = ''; form.expiresAt = ''"
            class="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 px-3 py-1.5">
            {{ $t('common.cancel') }}
          </button>
        </div>
      </div>

      <!-- Token affiché une seule fois -->
      <div v-if="newToken"
        class="bg-green-50 dark:bg-green-900/30 border border-green-300 dark:border-green-700 rounded-xl p-4 mb-4">
        <p class="text-sm font-semibold text-green-800 dark:text-green-300 mb-2">
          {{ $t('profile.token_created_msg') }}
        </p>
        <div class="flex items-center gap-2">
          <code class="flex-1 bg-white dark:bg-gray-800 border border-green-200 dark:border-green-700 rounded-lg px-3 py-2 text-xs font-mono text-gray-800 dark:text-gray-200 break-all">
            {{ newToken.plainToken }}
          </code>
          <button @click="copyToken(newToken.plainToken)"
            class="shrink-0 text-xs text-green-700 dark:text-green-400 border border-green-300 dark:border-green-600 rounded-lg px-3 py-2 hover:bg-green-100 dark:hover:bg-green-900/50">
            {{ copied ? $t('profile.token_copied') : $t('profile.token_copy') }}
          </button>
        </div>
        <p class="text-xs text-green-600 dark:text-green-400 mt-2">
          {{ $t('profile.token_usage') }} <code class="bg-green-100 dark:bg-green-900/50 px-1 rounded">Authorization: Bearer {{ newToken.plainToken.slice(0, 12) }}…</code>
        </p>
        <button @click="newToken = null; copied = false" class="mt-3 text-xs text-green-700 dark:text-green-400 underline">
          {{ $t('profile.token_close') }}
        </button>
      </div>

      <!-- Liste des tokens -->
      <div v-if="tokens.length === 0 && !loading" class="text-sm text-gray-400 dark:text-gray-500 py-4 text-center">
        {{ $t('profile.token_none') }}
      </div>
      <div v-else class="space-y-2">
        <div v-for="t in tokens" :key="t.id"
          class="bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl px-4 py-3 flex items-center gap-4">
          <div class="flex-1 min-w-0">
            <p class="font-medium text-gray-900 dark:text-gray-100 text-sm">{{ t.name }}</p>
            <p class="text-xs text-gray-400 dark:text-gray-500 mt-0.5">
              {{ $t('profile.token_created_at', { date: formatDate(t.createdAt) }) }}
              <span v-if="t.lastUsedAt"> · {{ $t('profile.token_last_used', { date: formatDate(t.lastUsedAt) }) }}</span>
              <span v-if="t.expiresAt" :class="isExpired(t.expiresAt) ? 'text-red-500' : 'text-gray-400 dark:text-gray-500'">
                · {{ $t('profile.token_expires', { date: formatDate(t.expiresAt) }) }}
              </span>
            </p>
          </div>
          <span v-if="isExpired(t.expiresAt)"
            class="text-xs bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 px-2 py-0.5 rounded-full shrink-0">
            {{ $t('profile.token_expired') }}
          </span>
          <button @click="deleteToken(t.id)"
            class="shrink-0 text-xs text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors">
            {{ $t('profile.token_revoke') }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth.js'
import { useThemeStore } from '../stores/theme.js'
import { useLocaleStore } from '../stores/locale.js'
import { personalTokensApi } from '../services/api.js'

const auth = useAuthStore()
const theme = useThemeStore()
const locale = useLocaleStore()

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
