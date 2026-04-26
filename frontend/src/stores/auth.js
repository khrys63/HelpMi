import { defineStore } from 'pinia'
import { ref } from 'vue'
import Keycloak from 'keycloak-js'

const DEV_MODE = import.meta.env.VITE_DEV_MODE === 'true'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(null)
  const keycloak = ref(null)

  async function init() {
    if (DEV_MODE) {
      // In dev mode fetch the current user from the backend (DevAuthFilter sets it)
      try {
        const res = await fetch('/api/users/me')
        user.value = await res.json()
      } catch {
        user.value = { firstName: 'Admin', lastName: 'Dev', email: 'admin@helpmi.local', role: 'ADMIN' }
      }
      return
    }

    const kc = new Keycloak({
      url: import.meta.env.VITE_KEYCLOAK_URL,
      realm: import.meta.env.VITE_KEYCLOAK_REALM,
      clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID
    })
    keycloak.value = kc

    await kc.init({ onLoad: 'login-required', pkceMethod: 'S256' })
    token.value = kc.token

    // refresh token before expiry
    setInterval(async () => {
      try {
        await kc.updateToken(60)
        token.value = kc.token
      } catch {
        kc.login()
      }
    }, 30000)

    // Fetch user profile from backend (creates user on first login)
    const res = await fetch('/api/users/me', {
      headers: { Authorization: `Bearer ${kc.token}` }
    })
    user.value = await res.json()
  }

  function logout() {
    if (keycloak.value) {
      keycloak.value.logout()
    }
  }

  function getToken() {
    return keycloak.value ? keycloak.value.token : null
  }

  return { user, token, init, logout, getToken }
})
