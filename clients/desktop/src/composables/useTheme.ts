/**
 * 主题系统 Composable
 *
 * 支持暗色/亮色模式切换，自动检测系统偏好，
 * 通过 CSS 自定义属性实现全局主题。
 */
import { ref, watchEffect } from 'vue'

export type ThemeMode = 'dark' | 'light' | 'auto'

const THEME_STORAGE_KEY = 'solra_theme_mode'

// 全局响应式状态
const currentMode = ref<ThemeMode>(
  (localStorage.getItem(THEME_STORAGE_KEY) as ThemeMode) || 'auto'
)

const isDark = ref(true)

// 检测系统偏好
function detectSystemPreference(): boolean {
  if (typeof window === 'undefined') return true
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

// 应用主题
function applyTheme(dark: boolean) {
  const root = document.documentElement

  if (dark) {
    // 暗色主题 — GitHub Dark 风格
    root.style.setProperty('--solra-bg-primary', '#0d1117')
    root.style.setProperty('--solra-bg-secondary', '#161b22')
    root.style.setProperty('--solra-bg-tertiary', '#21262d')
    root.style.setProperty('--solra-text-primary', '#e6edf3')
    root.style.setProperty('--solra-text-secondary', '#8b949e')
    root.style.setProperty('--solra-text-tertiary', '#484f58')
    root.style.setProperty('--solra-border', '#30363d')
    root.style.setProperty('--solra-border-light', '#21262d')
    root.style.setProperty('--solra-brand', '#58a6ff')
    root.style.setProperty('--solra-brand-hover', '#79c0ff')
    root.style.setProperty('--solra-success', '#238636')
    root.style.setProperty('--solra-warning', '#d29922')
    root.style.setProperty('--solra-error', '#f85149')
    root.style.setProperty('--solra-scrollbar-thumb', '#30363d')
    root.style.setProperty('--solra-scrollbar-track', '#161b22')
    root.style.setProperty('--solra-card-shadow', '0 1px 3px rgba(0,0,0,0.4)')
  } else {
    // 亮色主题 — GitHub Light 风格
    root.style.setProperty('--solra-bg-primary', '#ffffff')
    root.style.setProperty('--solra-bg-secondary', '#f6f8fa')
    root.style.setProperty('--solra-bg-tertiary', '#eaeef2')
    root.style.setProperty('--solra-text-primary', '#1f2328')
    root.style.setProperty('--solra-text-secondary', '#656d76')
    root.style.setProperty('--solra-text-tertiary', '#8b949e')
    root.style.setProperty('--solra-border', '#d0d7de')
    root.style.setProperty('--solra-border-light', '#eaeef2')
    root.style.setProperty('--solra-brand', '#0969da')
    root.style.setProperty('--solra-brand-hover', '#0550ae')
    root.style.setProperty('--solra-success', '#1a7f37')
    root.style.setProperty('--solra-warning', '#9a6700')
    root.style.setProperty('--solra-error', '#cf222e')
    root.style.setProperty('--solra-scrollbar-thumb', '#d0d7de')
    root.style.setProperty('--solra-scrollbar-track', '#f6f8fa')
    root.style.setProperty('--solra-card-shadow', '0 1px 3px rgba(0,0,0,0.08)')
  }

  root.setAttribute('data-theme', dark ? 'dark' : 'light')
  isDark.value = dark
}

// 监听模式变化并应用
watchEffect(() => {
  const mode = currentMode.value
  if (mode === 'dark') {
    applyTheme(true)
  } else if (mode === 'light') {
    applyTheme(false)
  } else {
    // auto: 跟随系统
    applyTheme(detectSystemPreference())

    // 监听系统主题变化
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handler = (e: MediaQueryListEvent) => {
      if (currentMode.value === 'auto') {
        applyTheme(e.matches)
      }
    }
    mediaQuery.addEventListener('change', handler)
  }
})

export function useTheme() {
  function setTheme(mode: ThemeMode) {
    currentMode.value = mode
    localStorage.setItem(THEME_STORAGE_KEY, mode)
  }

  function toggleTheme() {
    const next = isDark.value ? 'light' : 'dark'
    setTheme(next as ThemeMode)
  }

  return {
    currentMode,
    isDark,
    setTheme,
    toggleTheme,
  }
}
