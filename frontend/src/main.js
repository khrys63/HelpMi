import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
import { useAuthStore } from './stores/auth.js'
import './assets/main.css'

async function bootstrap() {
  const app = createApp(App)
  const pinia = createPinia()
  app.use(pinia)
  app.use(router)

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
