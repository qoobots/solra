/**
 * Solra Desktop — 国际化 (i18n) 模块
 *
 * 轻量级 i18n 实现，支持 zh-CN / en-US。
 * 语言偏好自动持久化到 localStorage。
 */

import { ref, computed, watch } from 'vue'
import zhCN from '@/locales/zh-CN.json'
import enUS from '@/locales/en-US.json'

type LocaleMessages = typeof zhCN

const SUPPORTED_LOCALES = {
  'zh-CN': zhCN as LocaleMessages,
  'en-US': enUS as LocaleMessages,
} as const

type SupportedLocale = keyof typeof SUPPORTED_LOCALES

const LOCALE_KEY = 'solra_locale'

function getBrowserLocale(): SupportedLocale {
  const nav = navigator.language
  if (nav.startsWith('zh')) return 'zh-CN'
  return 'en-US'
}

function loadLocale(): SupportedLocale {
  try {
    const stored = localStorage.getItem(LOCALE_KEY)
    if (stored && stored in SUPPORTED_LOCALES) return stored as SupportedLocale
  } catch { /* ignore */ }
  return getBrowserLocale()
}

const currentLocale = ref<SupportedLocale>(loadLocale())

watch(currentLocale, (val) => {
  try { localStorage.setItem(LOCALE_KEY, val) } catch { /* ignore */ }
  document.documentElement.lang = val
})

// 初始化 HTML lang 属性
document.documentElement.lang = currentLocale.value

/**
 * 翻译函数：根据 key 路径获取翻译文本，支持简单的插值。
 *
 * @param key 点分隔的 key 路径，如 "settings.title"
 * @param params 插值参数，如 { count: 5 }
 * @returns 翻译后的字符串
 */
export function $t(key: string, params?: Record<string, string | number>): string {
  const messages = SUPPORTED_LOCALES[currentLocale.value]
  const parts = key.split('.')
  let value: unknown = messages

  for (const part of parts) {
    if (value && typeof value === 'object' && part in value) {
      value = (value as Record<string, unknown>)[part]
    } else {
      // 回退：返回 key 本身
      return key
    }
  }

  if (typeof value !== 'string') return key

  // 插值替换
  if (params) {
    return value.replace(/\{(\w+)\}/g, (_, k) => {
      const v = params[k]
      return v !== undefined ? String(v) : `{${k}}`
    })
  }

  return value
}

/**
 * 获取当前语言
 */
export function getCurrentLocale(): SupportedLocale {
  return currentLocale.value
}

/**
 * 设置当前语言
 */
export function setLocale(locale: SupportedLocale) {
  if (locale in SUPPORTED_LOCALES) {
    currentLocale.value = locale
  }
}

/**
 * 获取所有支持的语言列表
 */
export function getSupportedLocales(): { code: SupportedLocale; name: string }[] {
  return [
    { code: 'zh-CN', name: '简体中文' },
    { code: 'en-US', name: 'English' },
  ]
}

/**
 * Vue Composable：在组件中使用 i18n
 */
export function useI18n() {
  return {
    t: $t,
    locale: computed(() => currentLocale.value),
    setLocale,
    supportedLocales: getSupportedLocales(),
  }
}

export default {
  install(app: any) {
    app.config.globalProperties.$t = $t
    app.provide('i18n', { t: $t, locale: currentLocale, setLocale })
  }
}
