import { defineStore } from 'pinia'
import { ref } from 'vue'
import { i18n } from '../i18n.js'
import { usersApi } from '../services/api.js'
import { useConfigStore } from './config.js'

const LANG_ATTR = { fr: 'fr', en: 'en-GB', bg: 'bg' }

export const useLocaleStore = defineStore('locale', () => {
  const current = ref('fr')

  function apply(locale) {
    current.value = locale
    i18n.global.locale.value = locale
    document.documentElement.lang = LANG_ATTR[locale] ?? 'fr'
    const config = useConfigStore()
    if (config.loaded) config.localizeAll()
  }

  async function set(locale) {
    const previous = current.value
    apply(locale)
    try {
      const { data } = await usersApi.updateLocale(locale)
      apply(data.locale)
    } catch {
      apply(previous)
    }
  }

  return { current, apply, set }
})
