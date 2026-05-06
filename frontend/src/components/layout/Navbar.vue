<template>
  <nav class="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 flex items-center justify-between h-14">
      <router-link to="/projects" class="flex items-center gap-2 text-blue-600 font-bold text-lg tracking-tight">
        <img :src="logo" alt="HelpMi" />
        HelpMi
      </router-link>

      <div class="flex items-center gap-4">
        <!-- Desktop nav links -->
        <div class="hidden md:flex items-center gap-1">
          <router-link to="/project"
            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            :class="currentRoute('/projects') ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'">
            {{ $t('nav.projects') }}
          </router-link>
          <router-link to="/dashboard"
            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            :class="currentRoute('/dashboard') ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'">
            {{ $t('nav.dashboard') }}
          </router-link>
          <router-link v-if="isManager" to="/dashboard/managers"
            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
            :class="currentRoute('/dashboard/managers') ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'">
            {{ $t('nav.manager_tracking') }}
          </router-link>
          <template v-if="isAdmin">
            <router-link to="/admin/organizations"
              class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
              :class="currentRoute('/admin/organizations') ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'">
              {{ $t('nav.organizations') }}
            </router-link>
            <router-link to="/admin/users"
              class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
              :class="currentRoute('/admin/users') ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'">
              {{ $t('nav.users') }}
            </router-link>
            <router-link to="/admin/config"
              class="px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
              :class="currentRoute('/admin/config') ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'">
              {{ $t('nav.config') }}
            </router-link>
          </template>
        </div>

        <!-- Mobile menu button -->
        <div class="md:hidden relative">
          <button @click="menuOpen = !menuOpen"
            class="text-gray-600 dark:text-gray-300 p-1">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path v-if="!menuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 6h16M4 12h16M4 18h16"/>
              <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
          <div v-if="menuOpen"
            @click.outside="menuOpen = false"
            class="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 py-2 z-50">
            <router-link @click="menuOpen = false" to="/projects"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.projects') }}
            </router-link>
            <router-link @click="menuOpen = false" to="/dashboard"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.dashboard') }}
            </router-link>
            <router-link v-if="isManager" @click="menuOpen = false" to="/dashboard/managers"
              class="block px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700">
              {{ $t('nav.manager_tracking') }}
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

        <!-- User avatar -->
        <div v-if="auth.user" class="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
          <router-link to="/profile"
            class="w-7 h-7 rounded-full bg-blue-600 text-white flex items-center justify-center font-medium text-xs hover:bg-blue-700 transition-colors"
            :title="`${auth.user.firstName} ${auth.user.lastName}`">
            {{ initials }}
          </router-link>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../stores/auth.js'
import { useThemeStore } from '../../stores/theme.js'
import logo from '../../assets/HelpMi_50.png'

const route = useRoute()
const auth = useAuthStore()
const theme = useThemeStore()
const menuOpen = ref(false)

const isManager = computed(() => auth.user?.projectRoles?.some(pr => pr.role === 'MANAGER') ?? false)
const isAdmin = computed(() => auth.user?.role === 'ADMIN')

const currentRoute = (path) => route.path === path || (path !== '/dashboard' && route.path.startsWith(path))

const initials = computed(() => {
  if (!auth.user) return '?'
  return ((auth.user.firstName?.[0] || '') + (auth.user.lastName?.[0] || '')).toUpperCase()
})
</script>
