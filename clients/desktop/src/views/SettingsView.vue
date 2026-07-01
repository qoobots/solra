<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTheme, type ThemeMode } from '@/composables/useTheme'
import { useSettings } from '@/composables/useSettings'

const router = useRouter()
const { currentMode, isDark, setTheme } = useTheme()
const { settings, updateRender, updateNotification, resetRender, clearCache } = useSettings()

// Tab state
const activeTab = ref<'appearance' | 'render' | 'notification' | 'about'>('appearance')

// Confirm dialog
const showConfirm = ref(false)
const confirmAction = ref<() => void>(() => {})

function confirm(title: string, action: () => void) {
  confirmAction.value = action
  showConfirm.value = true
}

function goBack() {
  router.back()
}
</script>

<template>
  <div class="settings-view">
    <!-- Header -->
    <header class="settings-header">
      <button class="back-btn" @click="goBack">
        <span class="back-arrow">←</span>
      </button>
      <h1>设置</h1>
    </header>

    <div class="settings-layout">
      <!-- Sidebar Tabs -->
      <nav class="settings-nav">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="['nav-item', { active: activeTab === tab.id }]"
          @click="activeTab = tab.id"
        >
          <span class="nav-icon">{{ tab.icon }}</span>
          <span class="nav-label">{{ tab.label }}</span>
        </button>
      </nav>

      <!-- Content Area -->
      <main class="settings-content">
        <!-- Appearance -->
        <section v-if="activeTab === 'appearance'" class="settings-section">
          <h2>外观设置</h2>

          <div class="setting-group">
            <h3>主题模式</h3>
            <div class="theme-selector">
              <label
                v-for="opt in themeOptions"
                :key="opt.value"
                :class="['theme-option', { active: currentMode === opt.value }]"
                @click="setTheme(opt.value as ThemeMode)"
              >
                <span class="theme-icon">{{ opt.icon }}</span>
                <span class="theme-label">{{ opt.label }}</span>
              </label>
            </div>
            <p class="setting-hint">
              当前：{{ isDark ? '暗色模式' : '亮色模式' }}
            </p>
          </div>

          <div class="setting-group">
            <h3>界面显示</h3>
            <div class="setting-row">
              <span class="setting-label">显示 FPS</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.showFps" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <span class="setting-label">显示 GPU 信息</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.showGpuInfo" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <span class="setting-label">语言</span>
              <select v-model="settings.language" class="setting-select">
                <option value="zh-CN">简体中文</option>
                <option value="en-US">English</option>
              </select>
            </div>
          </div>
        </section>

        <!-- Render -->
        <section v-if="activeTab === 'render'" class="settings-section">
          <h2>渲染设置</h2>

          <div class="setting-group">
            <h3>3D 渲染</h3>
            <div class="setting-row">
              <span class="setting-label">线框模式</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.render.wireframeMode" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <span class="setting-label">显示网格</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.render.showGrid" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <span class="setting-label">显示粒子</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.render.showParticles" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-group">
            <h3>光照</h3>
            <div class="setting-row slider-row">
              <span class="setting-label">环境光强度</span>
              <div class="slider-control">
                <input
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                  v-model.number="settings.render.ambientIntensity"
                />
                <span class="slider-value">{{ settings.render.ambientIntensity.toFixed(1) }}</span>
              </div>
            </div>
            <div class="setting-row slider-row">
              <span class="setting-label">太阳光强度</span>
              <div class="slider-control">
                <input
                  type="range"
                  min="0"
                  max="10"
                  step="0.5"
                  v-model.number="settings.render.sunIntensity"
                />
                <span class="slider-value">{{ settings.render.sunIntensity.toFixed(1) }}</span>
              </div>
            </div>
          </div>

          <div class="setting-group">
            <h3>性能</h3>
            <div class="setting-row slider-row">
              <span class="setting-label">分辨率缩放</span>
              <div class="slider-control">
                <input
                  type="range"
                  min="0.5"
                  max="2"
                  step="0.25"
                  v-model.number="settings.render.resolutionScale"
                />
                <span class="slider-value">{{ settings.render.resolutionScale.toFixed(2) }}x</span>
              </div>
            </div>
            <div class="setting-row slider-row">
              <span class="setting-label">相机速度</span>
              <div class="slider-control">
                <input
                  type="range"
                  min="0.1"
                  max="5"
                  step="0.1"
                  v-model.number="settings.render.cameraSpeed"
                />
                <span class="slider-value">{{ settings.render.cameraSpeed.toFixed(1) }}</span>
              </div>
            </div>
          </div>

          <button class="reset-btn" @click="confirm('重置渲染设置', resetRender)">
            重置为默认值
          </button>
        </section>

        <!-- Notification -->
        <section v-if="activeTab === 'notification'" class="settings-section">
          <h2>通知设置</h2>

          <div class="setting-group">
            <div class="setting-row">
              <div>
                <span class="setting-label">声音提醒</span>
                <p class="setting-desc">收到消息时播放提示音</p>
              </div>
              <label class="toggle">
                <input type="checkbox" v-model="settings.notification.enableSound" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <div>
                <span class="setting-label">桌面通知</span>
                <p class="setting-desc">通过系统通知推送消息</p>
              </div>
              <label class="toggle">
                <input type="checkbox" v-model="settings.notification.enableDesktopNotification" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-group">
            <h3>通知类型</h3>
            <div class="setting-row">
              <span class="setting-label">新消息</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.notification.messageNotification" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <span class="setting-label">空间活动</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.notification.spaceNotification" />
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="setting-row">
              <span class="setting-label">系统公告</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.notification.systemNotification" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
        </section>

        <!-- About -->
        <section v-if="activeTab === 'about'" class="settings-section">
          <h2>关于索拉</h2>

          <div class="about-card">
            <div class="about-logo">S</div>
            <h3>Solra Desktop</h3>
            <p class="about-version">版本 v0.1.0-alpha</p>
            <p class="about-desc">
              索拉 — 面向未来虚实共生空间的智能体交互平台。
              自研 GPU 渲染引擎 + 端侧推理 + P2P 实时通信。
            </p>

            <div class="about-links">
              <a href="#" class="about-link">📄 用户协议</a>
              <a href="#" class="about-link">🔒 隐私政策</a>
              <a href="#" class="about-link">📖 开源许可</a>
            </div>
          </div>

          <div class="setting-group">
            <div class="setting-row">
              <span class="setting-label">自动检查更新</span>
              <label class="toggle">
                <input type="checkbox" v-model="settings.autoUpdate" />
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <button class="reset-btn" @click="confirm('清除所有缓存', clearCache)">
            清除缓存
          </button>
        </section>
      </main>
    </div>

    <!-- Confirm Dialog -->
    <div v-if="showConfirm" class="confirm-overlay" @click.self="showConfirm = false">
      <div class="confirm-dialog">
        <p class="confirm-text">确定要执行此操作吗？</p>
        <div class="confirm-actions">
          <button class="confirm-btn cancel" @click="showConfirm = false">取消</button>
          <button class="confirm-btn ok" @click="confirmAction(); showConfirm = false">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
const tabs = [
  { id: 'appearance' as const, icon: '🎨', label: '外观' },
  { id: 'render' as const, icon: '🖥️', label: '渲染' },
  { id: 'notification' as const, icon: '🔔', label: '通知' },
  { id: 'about' as const, icon: 'ℹ️', label: '关于' },
]

const themeOptions = [
  { value: 'dark', icon: '🌙', label: '暗色' },
  { value: 'light', icon: '☀️', label: '亮色' },
  { value: 'auto', icon: '🔄', label: '跟随系统' },
]
</script>

<style lang="scss" scoped>
.settings-view {
  min-height: 100vh;
  background: var(--solra-bg-primary);
  color: var(--solra-text-primary);
}

.settings-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  border-bottom: 1px solid var(--solra-border);
  background: var(--solra-bg-secondary);

  h1 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.back-btn {
  background: none;
  border: none;
  color: var(--solra-text-secondary);
  cursor: pointer;
  font-size: 18px;
  padding: 4px 8px;
  border-radius: 6px;

  &:hover {
    background: var(--solra-bg-tertiary);
    color: var(--solra-text-primary);
  }
}

.settings-layout {
  display: flex;
  max-width: 960px;
  margin: 0 auto;
  min-height: calc(100vh - 57px);
}

.settings-nav {
  width: 180px;
  border-right: 1px solid var(--solra-border);
  background: var(--solra-bg-secondary);
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: none;
  color: var(--solra-text-secondary);
  cursor: pointer;
  font-size: 14px;
  text-align: left;
  transition: all 0.15s;

  &:hover {
    background: var(--solra-bg-tertiary);
    color: var(--solra-text-primary);
  }

  &.active {
    background: rgba(88, 166, 255, 0.12);
    color: var(--solra-brand);

    .nav-icon {
      transform: scale(1.1);
    }
  }
}

.nav-icon {
  font-size: 16px;
}

.settings-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
}

.settings-section {
  h2 {
    margin: 0 0 20px;
    font-size: 20px;
    font-weight: 600;
  }
}

.setting-group {
  background: var(--solra-bg-secondary);
  border: 1px solid var(--solra-border);
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;

  h3 {
    margin: 0 0 12px;
    font-size: 14px;
    font-weight: 600;
    color: var(--solra-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--solra-border-light);

  &:last-child {
    border-bottom: none;
  }

  &.slider-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
}

.setting-label {
  font-size: 14px;
  color: var(--solra-text-primary);
}

.setting-desc {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--solra-text-tertiary);
}

.setting-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--solra-text-tertiary);
}

.setting-select {
  background: var(--solra-bg-tertiary);
  border: 1px solid var(--solra-border);
  border-radius: 6px;
  color: var(--solra-text-primary);
  padding: 6px 10px;
  font-size: 13px;
  outline: none;

  &:focus {
    border-color: var(--solra-brand);
  }
}

/* Theme Selector */
.theme-selector {
  display: flex;
  gap: 8px;
}

.theme-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 8px;
  border: 2px solid var(--solra-border);
  border-radius: 8px;
  background: var(--solra-bg-tertiary);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: var(--solra-text-tertiary);
  }

  &.active {
    border-color: var(--solra-brand);
    background: rgba(88, 166, 255, 0.08);
  }
}

.theme-icon {
  font-size: 22px;
}

.theme-label {
  font-size: 12px;
  color: var(--solra-text-secondary);
}

/* Toggle Switch */
.toggle {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;

  input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .toggle-slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--solra-bg-tertiary);
    border: 1px solid var(--solra-border);
    border-radius: 24px;
    transition: all 0.2s;

    &::before {
      content: '';
      position: absolute;
      height: 18px;
      width: 18px;
      left: 2px;
      bottom: 2px;
      background: var(--solra-text-secondary);
      border-radius: 50%;
      transition: all 0.2s;
    }
  }

  input:checked + .toggle-slider {
    background: var(--solra-brand);
    border-color: var(--solra-brand);

    &::before {
      transform: translateX(20px);
      background: #fff;
    }
  }
}

/* Slider */
.slider-control {
  display: flex;
  align-items: center;
  gap: 12px;

  input[type='range'] {
    flex: 1;
    height: 4px;
    -webkit-appearance: none;
    appearance: none;
    background: var(--solra-bg-tertiary);
    border-radius: 2px;
    outline: none;

    &::-webkit-slider-thumb {
      -webkit-appearance: none;
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: var(--solra-brand);
      cursor: pointer;
    }
  }
}

.slider-value {
  font-size: 13px;
  color: var(--solra-text-secondary);
  min-width: 36px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.reset-btn {
  display: block;
  width: 100%;
  padding: 10px;
  background: transparent;
  border: 1px solid var(--solra-border);
  border-radius: 8px;
  color: var(--solra-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--solra-bg-tertiary);
    color: var(--solra-text-primary);
  }
}

/* About */
.about-card {
  background: var(--solra-bg-secondary);
  border: 1px solid var(--solra-border);
  border-radius: 10px;
  padding: 24px;
  text-align: center;
  margin-bottom: 16px;
}

.about-logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #58a6ff, #1f6feb);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin: 0 auto 12px;
}

.about-card h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.about-version {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--solra-text-tertiary);
}

.about-desc {
  font-size: 13px;
  color: var(--solra-text-secondary);
  line-height: 1.6;
  max-width: 400px;
  margin: 0 auto;
}

.about-links {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

.about-link {
  font-size: 12px;
  color: var(--solra-brand);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

/* Confirm Dialog */
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-dialog {
  background: var(--solra-bg-secondary);
  border: 1px solid var(--solra-border);
  border-radius: 12px;
  padding: 24px;
  min-width: 300px;
  box-shadow: var(--solra-card-shadow);
}

.confirm-text {
  margin: 0 0 20px;
  font-size: 15px;
  text-align: center;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.confirm-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--solra-border);
  font-size: 14px;
  cursor: pointer;
  background: var(--solra-bg-tertiary);
  color: var(--solra-text-primary);

  &.ok {
    background: var(--solra-brand);
    border-color: var(--solra-brand);
    color: #fff;

    &:hover {
      background: var(--solra-brand-hover);
    }
  }

  &.cancel:hover {
    background: var(--solra-border);
  }
}
</style>
