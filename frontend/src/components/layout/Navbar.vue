<template>
  <nav class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
      <router-link to="/projects" class="flex items-center gap-2 text-blue-600 font-bold text-lg tracking-tight">
        <img :src="logo" alt="HelpMi" />
        HelpMi
      </router-link>

      <!-- Desktop: hamburger menu -->
      <div class="relative hidden md:block">
        <button
          @click="menuOpen = !menuOpen"
          @click.outside="menuOpen = false"
          class="flex items-center gap-1 text-sm text-gray-600 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400">
          <span>{{ $t('nav.menu') }}</span>
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  :class="{ 'rotate-180': !menuOpen }" d="M19 9l-7 7-7-7"/>
          </svg>
        </button>

        <div v-show="menuOpen"
          class="absolute right-0 mt-2 w-56 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-2 z-50">
          <router-link @click="menuOpen = false" to="/dashboard"
            class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
            {{ $t('nav.dashboard') }}
          </router-link>
          <router-link v-if="isManager" @click="menuOpen = false" to="/dashboard/managers"
            class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
            {{ $t('nav.manager_tracking') }}
          </router-link>
          <router-link @click="menuOpen = false" to="/projects"
            class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
            {{ $t('nav.projects') }}
          </router-link>
          <template v-if="isAdmin">
            <hr class="my-1 border-gray-200 dark:border-gray-700" />
            <router-link @click="menuOpen = false" to="/admin/organizations"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.organizations') }}
            </router-link>
            <router-link @click="menuOpen = false" to="/admin/users"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.users') }}
            </router-link>
            <router-link @click="menuOpen = false" to="/admin/config"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.config') }}
            </router-link>
          </template>
        </div>
      </div>

      <!-- Mobile: hamburger icon -->
      <div class="md:hidden">
        <button @click="menuOpen = !menuOpen"
          class="text-gray-600 dark:text-gray-300">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path v-if="!menuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M4 6h16M4 12h16M4 18h16"/>
            <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>

        <div v-if="menuOpen"
          class="absolute right-4 top-14 w-56 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-2 z-50">
          <router-link @click="menuOpen = false" to="/dashboard"
            class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
            {{ $t('nav.dashboard') }}
          </router-link>
          <router-link v-if="isManager" @click="menuOpen = false" to="/dashboard/managers"
            class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
            {{ $t('nav.manager_tracking') }}
          </router-link>
          <router-link @click="menuOpen = false" to="/projects"
            class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
            {{ $t('nav.projects') }}
          </router-link>
          <template v-if="isAdmin">
            <hr class="my-1 border-gray-200 dark:border-gray-700" />
            <router-link @click="menuOpen = false" to="/admin/organizations"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.organizations') }}
            </router-link>
            <router-link @click="menuOpen = false" to="/admin/users"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.users') }}
            </router-link>
            <router-link @click="menuOpen = false" to="/admin/config"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.config') }}
            </router-link>
          </template>
        </div>
      </div>

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
  </nav>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '../../stores/auth.js'
import { useThemeStore } from '../../stores/theme.js'
import logo from '../../assets/HelpMi_50.png'

const auth = useAuthStore()
const theme = useThemeStore()
const menuOpen = ref(false)

const isManager = computed(() => auth.user?.projectRoles?.some(pr => pr.role === 'MANAGER') ?? false)
const isAdmin = computed(() => auth.user?.role === 'ADMIN')
const initials = computed(() => {
  if (!auth.user) return '?'
  return ((auth.user.firstName?.[0] || '') + (auth.user.lastName?.[0] || '')).toUpperCase()
})
</script>
