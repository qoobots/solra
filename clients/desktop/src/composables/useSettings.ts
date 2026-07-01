/**
 * 设置管理 Composable
 *
 * 管理用户本地设置（渲染偏好、通知、缓存等），
 * 自动持久化到 localStorage。
 */
import { ref, watch } from 'vue'

export interface RenderSettings {
  wireframeMode: boolean
  showGrid: boolean
  showParticles: boolean
  ambientIntensity: number
  sunIntensity: number
  cameraSpeed: number
  resolutionScale: number
}

export interface NotificationSettings {
  enableSound: boolean
  enableDesktopNotification: boolean
  messageNotification: boolean
  spaceNotification: boolean
  systemNotification: boolean
}

export interface AppSettings {
  render: RenderSettings
  notification: NotificationSettings
  autoUpdate: boolean
  language: string
  showFps: boolean
  showGpuInfo: boolean
}

const SETTINGS_KEY = 'solra_app_settings'

const defaultSettings: AppSettings = {
  render: {
    wireframeMode: false,
    showGrid: true,
    showParticles: true,
    ambientIntensity: 0.6,
    sunIntensity: 3.5,
    cameraSpeed: 1.0,
    resolutionScale: 1.0,
  },
  notification: {
    enableSound: true,
    enableDesktopNotification: true,
    messageNotification: true,
    spaceNotification: true,
    systemNotification: false,
  },
  autoUpdate: true,
  language: 'zh-CN',
  showFps: true,
  showGpuInfo: true,
}

function loadSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY)
    if (raw) {
      return { ...defaultSettings, ...JSON.parse(raw) }
    }
  } catch {
    // corrupted settings, use defaults
  }
  return { ...defaultSettings }
}

function saveSettings(settings: AppSettings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
}

// 全局响应式状态
const settings = ref<AppSettings>(loadSettings())

// 自动持久化
watch(
  settings,
  (val) => saveSettings(val),
  { deep: true }
)

export function useSettings() {
  function updateRender<K extends keyof RenderSettings>(
    key: K,
    value: RenderSettings[K]
  ) {
    settings.value.render[key] = value
  }

  function updateNotification<K extends keyof NotificationSettings>(
    key: K,
    value: NotificationSettings[K]
  ) {
    settings.value.notification[key] = value
  }

  function resetRender() {
    settings.value.render = { ...defaultSettings.render }
  }

  function resetAll() {
    settings.value = { ...defaultSettings }
    localStorage.removeItem(SETTINGS_KEY)
  }

  function clearCache() {
    // Clear streaming cache if available
    try {
      localStorage.removeItem('solra_scene_cache')
      localStorage.removeItem('solra_asset_cache')
    } catch {
      // ignore
    }
  }

  return {
    settings,
    updateRender,
    updateNotification,
    resetRender,
    resetAll,
    clearCache,
  }
}
