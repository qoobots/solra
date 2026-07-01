import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { useTheme } from './composables/useTheme'
import './styles/global.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// Initialize theme system (reads localStorage, applies CSS vars)
const { currentMode } = useTheme()

app.mount('#app')
