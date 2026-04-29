import { defineStore } from 'pinia'
import { ref } from 'vue'
import { i18n } from '../i18n.js'
import { usersApi } from '../services/api.js'

export const useLocaleStore = defineStore('locale', () => {
  const current = ref('fr')

  function apply(locale) {
    current.value = locale
    i18n.global.locale.value = locale
  }

  async function set(locale) {
    apply(locale)
    try {
      const { data } = await usersApi.updateLocale(locale)
      apply(data.locale)
    } catch {
      apply(current.value === 'fr' ? 'en' : 'fr')
    }
  }

  return { current, apply, set }
})
