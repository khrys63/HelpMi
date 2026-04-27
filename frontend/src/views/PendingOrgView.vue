<template>
  <div class="min-h-[60vh] flex items-center justify-center">
    <div class="text-center max-w-md">
      <div class="w-16 h-16 bg-amber-100 rounded-full flex items-center justify-center mx-auto mb-4">
        <svg class="w-8 h-8 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <h1 class="text-xl font-bold text-gray-900 mb-2">Compte en attente d'activation</h1>
      <p class="text-gray-500 text-sm leading-relaxed">
        Votre compte a bien été créé, mais aucune organisation ne vous a encore été attribuée.<br>
        Contactez votre administrateur pour obtenir l'accès aux projets.
      </p>
      <p class="mt-4 text-xs text-gray-400">Connecté en tant que
        <strong>{{ auth.user?.firstName }} {{ auth.user?.lastName }}</strong>
        ({{ auth.user?.email }})
      </p>
      <button @click="auth.logout()"
        class="mt-6 text-sm text-gray-500 hover:text-red-600 underline">
        Se déconnecter
      </button>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()
</script>
