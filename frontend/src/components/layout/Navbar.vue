<template>
  <nav class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
      <router-link to="/projects" class="flex items-center gap-2 text-blue-600 font-bold text-lg tracking-tight">
        <img :src="logo" alt="HelpMi" />
        HelpMi
      </router-link>
      <div class="flex items-center gap-4">
        <router-link to="/projects" class="text-sm text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400">{{ $t('nav.projects') }}</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/organizations"
          class="text-sm text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400">{{ $t('nav.organizations') }}</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/users"
          class="text-sm text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400">{{ $t('nav.users') }}</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/config"
          class="text-sm text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400">{{ $t('nav.config') }}</router-link>
        <div v-if="auth.user" class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
          <router-link to="/profile"
            class="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-medium text-xs hover:bg-blue-700 transition-colors"
            :title="`${auth.user.firstName} ${auth.user.lastName}`">
            {{ initials }}
          </router-link>
          <button @click="auth.logout()"
            class="ml-2 text-xs text-gray-500 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400">{{ $t('nav.logout') }}</button>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { useThemeStore } from '../../stores/theme.js'
import logo from '../../assets/HelpMi_50.png'

const auth = useAuthStore()
const theme = useThemeStore()
const initials = computed(() => {
  if (!auth.user) return '?'
  return ((auth.user.firstName?.[0] || '') + (auth.user.lastName?.[0] || '')).toUpperCase()
})
</script>
