import { defineStore } from 'pinia'
import { ref } from 'vue'
import { usersApi } from '../services/api.js'

export const useThemeStore = defineStore('theme', () => {
  const current = ref('light')

  function apply(theme) {
    current.value = theme
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }

  async function toggle() {
    const next = current.value === 'light' ? 'dark' : 'light'
    apply(next)
    try {
      const { data } = await usersApi.updateTheme(next)
      apply(data.theme)
    } catch {
      apply(current.value === 'dark' ? 'light' : 'dark')
    }
  }

  return { current, apply, toggle }
})
