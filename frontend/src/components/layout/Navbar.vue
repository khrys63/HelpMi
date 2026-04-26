<template>
  <nav class="bg-white border-b border-gray-200 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
      <router-link to="/projects" class="text-blue-600 font-bold text-lg tracking-tight">HelpMi</router-link>
      <div class="flex items-center gap-4">
        <router-link to="/projects" class="text-sm text-gray-600 hover:text-blue-600">Projets</router-link>
        <router-link v-if="auth.user?.role === 'ADMIN'" to="/admin/config"
          class="text-sm text-gray-600 hover:text-blue-600">Configuration</router-link>
        <div v-if="auth.user" class="flex items-center gap-2 text-sm text-gray-700">
          <span class="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-medium text-xs">
            {{ initials }}
          </span>
          <span>{{ auth.user.firstName }} {{ auth.user.lastName }}</span>
          <span class="text-xs text-gray-400 uppercase">{{ auth.user.role }}</span>
          <button v-if="!isDevMode" @click="auth.logout()"
            class="ml-2 text-xs text-gray-500 hover:text-red-600">Déconnexion</button>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../../stores/auth.js'

const auth = useAuthStore()
const isDevMode = import.meta.env.VITE_DEV_MODE === 'true'
const initials = computed(() => {
  if (!auth.user) return '?'
  return ((auth.user.firstName?.[0] || '') + (auth.user.lastName?.[0] || '')).toUpperCase()
})
</script>
