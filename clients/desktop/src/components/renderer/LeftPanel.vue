<script setup lang="ts">
/**
 * 左侧面板 — 场景控制 / 空间导航 / 工具面板
 *
 * 在 SpaceDetailView 中定位在 3D 视口左侧，
 * 提供场景渲染控制、空间切换、快捷工具等功能。
 */
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useRendererStore } from '@/stores/useRendererStore'

const router = useRouter()
const spaceStore = useSpaceStore()
const rendererStore = useRendererStore()

// 面板折叠状态
const activeSection = ref<'scene' | 'spaces' | 'tools'>('scene')
const scenePanelCollapsed = ref(false)
const spacesPanelCollapsed = ref(false)
const toolsPanelCollapsed = ref(false)

// 场景控制参数
const wireframeMode = ref(false)
const showGrid = ref(true)
const showParticles = ref(true)
const ambientIntensity = ref(0.6)
const sunIntensity = ref(3.5)
const cameraSpeed = ref(1.0)

// 空间列表（从 store 获取）
const spaceList = computed(() => spaceStore.spaces)

// 当前空间信息
const currentSpaceName = computed(() =>
  spaceStore.currentSpace?.title || '未命名空间'
)

function toggleSection(section: 'scene' | 'spaces' | 'tools') {
  if (activeSection.value === section) {
    activeSection.value = section // keep open, toggle content visibility
  } else {
    activeSection.value = section
  }
}

function isSectionActive(section: 'scene' | 'spaces' | 'tools') {
  return activeSection.value === section
}

function goToSpace(spaceId: string) {
  router.push(`/spaces/${spaceId}`)
}

function goHome() {
  router.push('/')
}

function resetSceneDefaults() {
  wireframeMode.value = false
  showGrid.value = true
  showParticles.value = true
  ambientIntensity.value = 0.6
  sunIntensity.value = 3.5
  cameraSpeed.value = 1.0
}
</script>

<template>
  <div class="left-panel">
    <!-- 面板头部 — 空间名称 -->
    <div class="panel-header">
      <div class="header-space-info">
        <span class="header-icon">🌌</span>
        <span class="header-title" :title="currentSpaceName">{{ currentSpaceName }}</span>
      </div>
      <button class="header-home-btn" @click="goHome" title="返回首页">
        ⌂
      </button>
    </div>

    <!-- 分区导航标签 -->
    <div class="section-tabs">
      <button
        :class="['tab-btn', { active: isSectionActive('scene') }]"
        @click="toggleSection('scene')"
        title="场景控制"
      >
        🎬 场景
      </button>
      <button
        :class="['tab-btn', { active: isSectionActive('spaces') }]"
        @click="toggleSection('spaces')"
        title="空间列表"
      >
        📁 空间
      </button>
      <button
        :class="['tab-btn', { active: isSectionActive('tools') }]"
        @click="toggleSection('tools')"
        title="工具面板"
      >
        🔧 工具
      </button>
    </div>

    <!-- 面板内容 -->
    <div class="panel-content">
      <!-- 场景控制区 -->
      <div v-show="isSectionActive('scene')" class="content-section">
        <div class="section-header" @click="scenePanelCollapsed = !scenePanelCollapsed">
          <span>场景控制</span>
          <span class="collapse-arrow">{{ scenePanelCollapsed ? '▶' : '▼' }}</span>
        </div>
        <div v-show="!scenePanelCollapsed" class="section-body">
          <div class="control-row">
            <label>线框模式</label>
            <input type="checkbox" v-model="wireframeMode" />
          </div>
          <div class="control-row">
            <label>显示网格</label>
            <input type="checkbox" v-model="showGrid" />
          </div>
          <div class="control-row">
            <label>显示粒子</label>
            <input type="checkbox" v-model="showParticles" />
          </div>
          <div class="control-row">
            <label>环境光强度</label>
            <input
              type="range"
              v-model.number="ambientIntensity"
              min="0"
              max="2"
              step="0.1"
            />
            <span class="range-value">{{ ambientIntensity.toFixed(1) }}</span>
          </div>
          <div class="control-row">
            <label>主光源强度</label>
            <input
              type="range"
              v-model.number="sunIntensity"
              min="0"
              max="10"
              step="0.5"
            />
            <span class="range-value">{{ sunIntensity.toFixed(1) }}</span>
          </div>
          <div class="control-row">
            <label>相机速度</label>
            <input
              type="range"
              v-model.number="cameraSpeed"
              min="0.1"
              max="5"
              step="0.1"
            />
            <span class="range-value">{{ cameraSpeed.toFixed(1) }}</span>
          </div>
          <button class="reset-btn" @click="resetSceneDefaults">重置默认</button>
        </div>
      </div>

      <!-- 空间列表 -->
      <div v-show="isSectionActive('spaces')" class="content-section">
        <div class="section-header" @click="spacesPanelCollapsed = !spacesPanelCollapsed">
          <span>推荐空间</span>
          <span class="collapse-arrow">{{ spacesPanelCollapsed ? '▶' : '▼' }}</span>
        </div>
        <div v-show="!spacesPanelCollapsed" class="section-body">
          <div v-if="spaceList.length === 0" class="empty-hint">
            <p>暂无空间数据</p>
            <p class="sub-hint">返回首页发现更多空间</p>
          </div>
          <div v-else class="space-list">
            <div
              v-for="space in spaceList.slice(0, 6)"
              :key="space.spaceId"
              :class="['space-item', { current: space.spaceId === spaceStore.currentSpace?.spaceId }]"
              @click="goToSpace(space.spaceId)"
            >
              <span class="space-item-icon">{{ space.spaceId === spaceStore.currentSpace?.spaceId ? '📍' : '🌍' }}</span>
              <div class="space-item-info">
                <span class="space-item-name">{{ space.title }}</span>
                <span class="space-item-meta">🟢 {{ space.onlineCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 工具面板 -->
      <div v-show="isSectionActive('tools')" class="content-section">
        <div class="section-header" @click="toolsPanelCollapsed = !toolsPanelCollapsed">
          <span>快捷工具</span>
          <span class="collapse-arrow">{{ toolsPanelCollapsed ? '▶' : '▼' }}</span>
        </div>
        <div v-show="!toolsPanelCollapsed" class="section-body">
          <div class="tool-group">
            <h4>渲染信息</h4>
            <div class="info-row">
              <span class="info-label">GPU 后端</span>
              <span class="info-value">{{ rendererStore.gpuBackend }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">当前 FPS</span>
              <span class="info-value fps-value">{{ rendererStore.fps }} FPS</span>
            </div>
          </div>
          <div class="tool-group">
            <h4>操作提示</h4>
            <div class="tip-list">
              <div class="tip-item">🖱️ 拖拽旋转视角</div>
              <div class="tip-item">🔍 滚轮缩放</div>
              <div class="tip-item">⌨️ WASD 移动</div>
              <div class="tip-item">📷 右键平移</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.left-panel {
  position: absolute;
  top: 48px;
  left: 0;
  bottom: 0;
  width: 280px;
  background: rgba(13, 17, 23, 0.92);
  backdrop-filter: blur(12px);
  border-right: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  z-index: 10;
  user-select: none;
}

// ---- 头部 ----
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #21262d;

  .header-space-info {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    flex: 1;

    .header-icon {
      font-size: 18px;
      flex-shrink: 0;
    }

    .header-title {
      font-size: 14px;
      font-weight: 600;
      color: #e6edf3;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .header-home-btn {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.12);
    color: #8b949e;
    width: 28px;
    height: 28px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-left: 8px;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
      color: #e6edf3;
    }
  }
}

// ---- 分区标签 ----
.section-tabs {
  display: flex;
  border-bottom: 1px solid #21262d;

  .tab-btn {
    flex: 1;
    padding: 10px 0;
    background: transparent;
    border: none;
    border-bottom: 2px solid transparent;
    color: #6e7681;
    font-size: 13px;
    cursor: pointer;
    transition: color 0.15s, border-color 0.15s;

    &:hover {
      color: #8b949e;
    }

    &.active {
      color: #58a6ff;
      border-bottom-color: #58a6ff;
    }
  }
}

// ---- 内容区 ----
.panel-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #30363d;
    border-radius: 2px;
  }
}

.content-section {
  border-bottom: 1px solid #21262d;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
    color: #8b949e;
    text-transform: uppercase;
    letter-spacing: 0.5px;

    &:hover {
      color: #c9d1d9;
      background: rgba(255, 255, 255, 0.03);
    }

    .collapse-arrow {
      font-size: 10px;
      color: #484f58;
    }
  }

  .section-body {
    padding: 0 14px 14px;
  }
}

// ---- 控制行 ----
.control-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;

  label {
    flex: 1;
    font-size: 12px;
    color: #8b949e;
    min-width: 0;
  }

  input[type="checkbox"] {
    width: 16px;
    height: 16px;
    accent-color: #58a6ff;
    cursor: pointer;
  }

  input[type="range"] {
    flex: 2;
    height: 4px;
    accent-color: #58a6ff;
    cursor: pointer;
  }

  .range-value {
    font-size: 11px;
    color: #58a6ff;
    width: 30px;
    text-align: right;
    font-family: monospace;
  }
}

.reset-btn {
  width: 100%;
  margin-top: 8px;
  padding: 6px 0;
  background: rgba(88, 166, 255, 0.1);
  border: 1px solid rgba(88, 166, 255, 0.25);
  border-radius: 6px;
  color: #58a6ff;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: rgba(88, 166, 255, 0.2);
  }
}

// ---- 空间列表 ----
.empty-hint {
  text-align: center;
  padding: 20px 0;

  p {
    margin: 0;
    font-size: 13px;
    color: #484f58;
  }

  .sub-hint {
    font-size: 11px;
    margin-top: 4px;
    color: #30363d;
  }
}

.space-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.space-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  &.current {
    background: rgba(88, 166, 255, 0.1);
    border: 1px solid rgba(88, 166, 255, 0.2);
  }

  .space-item-icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  .space-item-info {
    flex: 1;
    min-width: 0;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .space-item-name {
      font-size: 13px;
      color: #c9d1d9;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .space-item-meta {
      font-size: 11px;
      color: #6e7681;
      flex-shrink: 0;
      margin-left: 8px;
    }
  }
}

// ---- 工具面板 ----
.tool-group {
  margin-bottom: 12px;

  h4 {
    margin: 0 0 8px;
    font-size: 12px;
    font-weight: 600;
    color: #8b949e;
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;

  .info-label {
    font-size: 12px;
    color: #6e7681;
  }

  .info-value {
    font-size: 12px;
    color: #c9d1d9;
    font-family: monospace;

    &.fps-value {
      color: #3fb950;
    }
  }
}

.tip-list {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .tip-item {
    font-size: 11px;
    color: #484f58;
    padding: 2px 0;
  }
}
</style>
