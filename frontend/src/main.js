import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
import { i18n } from './i18n.js'
import { useAuthStore } from './stores/auth.js'
import './assets/main.css'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.use(router)
  app.use(i18n)

  const auth = useAuthStore()
  await auth.init()

  try {
    const { useConfigStore } = await import('./stores/config.js')
    await useConfigStore().load()
  } catch (e) {
    console.error('Failed to load config, mounting anyway:', e)
  }

  app.mount('#app')
}

bootstrap()
