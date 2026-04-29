<template>
  <div class="min-h-[60vh] flex items-center justify-center">
    <div class="text-center max-w-md">
      <div class="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h1 class="text-xl font-bold text-gray-900 dark:text-gray-100 mb-2">{{ $t('pending_org.title') }}</h1>
      <p class="text-gray-500 dark:text-gray-400 text-sm leading-relaxed">
        {{ $t('pending_org.message') }}<br>
        {{ $t('pending_org.contact') }}
      </p>
      <p class="mt-4 text-xs text-gray-400 dark:text-gray-500">{{ $t('pending_org.connected_as') }}
        <strong>{{ auth.user?.firstName }} {{ auth.user?.lastName }}</strong>
        ({{ auth.user?.email }})
      </p>
      <button @click="auth.logout()"
        class="mt-6 text-sm text-gray-500 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 underline">
        {{ $t('pending_org.logout') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()
</script>
