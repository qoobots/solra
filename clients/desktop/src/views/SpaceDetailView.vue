<script setup lang="ts">
/**
 * 3D 空间详情页 — Desktop 核心页面
 *
 * 架构：
 *   SpaceDetailView（桥接层）
 *     ├── LeftPanel（UI 控制面板）── emit ──→ SpaceDetailView ──→ RenderViewport API
 *     ├── RenderViewport（3D 渲染视口）
 *     └── 右侧面板（空间信息 + AI 虚拟人 + 在线用户）
 *
 * 支持 AI 虚拟人对话 + WebRTC 音视频
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useRendererStore } from '@/stores/useRendererStore'
import { invoke } from '@tauri-apps/api/core'
import RenderViewport from '@/components/renderer/RenderViewport.vue'
import LeftPanel from '@/components/renderer/LeftPanel.vue'
import { getSceneObjects } from '@/components/renderer/SceneDataModel'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const rendererStore = useRendererStore()
const spaceId = route.params.spaceId as string

// 渲染视口引用
const viewportRef = ref<InstanceType<typeof RenderViewport> | null>(null)

const rendererReady = ref(false)
const currentFps = ref(60)
const chatInput = ref('')
const chatMessages = ref<{ role: 'user' | 'avatar'; content: string }[]>([])
const isInSpace = ref(false)
const conversationId = ref<string | null>(null)
const isStreaming = ref(false)
const isWebrtcConnected = ref(false)

onMounted(async () => {
  await spaceStore.enterSpace(spaceId)
  isInSpace.value = true
})

onUnmounted(async () => {
  if (isInSpace.value) {
    await spaceStore.exitSpace(spaceId)
  }
})

// ========== 渲染器就绪回调 ==========
function onRendererReady() {
  rendererReady.value = true
  // 渲染器就绪后，根据场景数据加载 3D 对象
  loadSceneFromGraph()
}

function onFpsUpdate(fps: number) {
  currentFps.value = fps
}

// ========== 从 sceneGraph 构建 3D 场景 ==========
function loadSceneFromGraph() {
  const viewport = viewportRef.value
  if (!viewport) return

  // 使用 SceneDataModel 解析场景数据，服务端无数据时自动生成默认 Demo 场景
  const objects = getSceneObjects(spaceStore.currentSpace)

  if (objects.length > 0) {
    viewport.addObjects(objects)
    console.log(`[SpaceDetailView] 已加载 ${objects.length} 个场景对象`)
  }
}

// ========== LeftPanel 事件处理 ==========

function onWireframeChange(enabled: boolean) {
  viewportRef.value?.setWireframeMode(enabled)
}

function onGridVisibilityChange(visible: boolean) {
  viewportRef.value?.setGridVisible(visible)
}

function onParticlesVisibilityChange(visible: boolean) {
  viewportRef.value?.setParticlesVisible(visible)
}

function onAmbientIntensityChange(value: number) {
  viewportRef.value?.updateLighting({ ambientIntensity: value })
}

function onSunIntensityChange(value: number) {
  viewportRef.value?.updateLighting({ sunIntensity: value })
}

function onCameraSpeedChange(value: number) {
  viewportRef.value?.setCameraSpeed(value)
}

function onSceneReset() {
  const viewport = viewportRef.value
  if (!viewport) return
  // 重置光照到默认
  viewport.updateLighting({
    ambientIntensity: 0.6,
    sunIntensity: 3.5,
  })
  viewport.setWireframeMode(false)
  viewport.setGridVisible(true)
  viewport.setParticlesVisible(true)
  viewport.setCameraSpeed(1.0)
}

// ========== AI 虚拟人对话 ==========
async function sendChatMessage() {
  if (!chatInput.value.trim() || isStreaming.value) return
  const msg = chatInput.value.trim()
  chatMessages.value.push({ role: 'user', content: msg })
  chatInput.value = ''
  isStreaming.value = true

  try {
    if (!conversationId.value) {
      const conv = await invoke<{ id: string; status: string }>(
        'start_conversation',
        { avatarId: spaceId, spaceId }
      )
      conversationId.value = conv.id
    }

    const reply = await invoke<string>('send_message', {
      conversationId: conversationId.value,
      message: msg,
    })
    chatMessages.value.push({ role: 'avatar', content: reply })
  } catch (e) {
    console.error('AI 对话失败:', e)
    chatMessages.value.push({
      role: 'avatar',
      content: '抱歉，虚拟人暂时无法回应。请检查 Core SDK 是否已加载。',
    })
  } finally {
    isStreaming.value = false
  }
}

function goBack() {
  router.push('/')
}
</script>

<template>
  <div class="space-detail-view">
    <!-- 3D 渲染视口 -->
    <div class="viewport-container">
      <RenderViewport
        ref="viewportRef"
        @ready="onRendererReady"
        @fps-update="onFpsUpdate"
      />

      <!-- 顶部工具栏 -->
      <div class="top-toolbar">
        <button class="toolbar-btn" @click="goBack">← 返回</button>
        <span class="space-name">{{ spaceStore.currentSpace?.title || spaceId }}</span>
        <div class="toolbar-actions">
          <span class="fps-display">{{ currentFps }} FPS</span>
          <span class="gpu-tag">{{ rendererStore.gpuBackend }}</span>
          <span
            class="webrtc-indicator"
            :class="{ connected: isWebrtcConnected }"
            :title="isWebrtcConnected ? 'WebRTC 已连接' : 'WebRTC 未连接'"
          >
            {{ isWebrtcConnected ? '🔊' : '🔇' }}
          </span>
        </div>
      </div>

      <!-- 左侧面板 -->
      <LeftPanel
        @wireframe-change="onWireframeChange"
        @grid-visibility-change="onGridVisibilityChange"
        @particles-visibility-change="onParticlesVisibilityChange"
        @ambient-intensity-change="onAmbientIntensityChange"
        @sun-intensity-change="onSunIntensityChange"
        @camera-speed-change="onCameraSpeedChange"
        @scene-reset="onSceneReset"
      />

      <!-- 右侧面板 -->
      <div class="side-panel">
        <div class="panel-section">
          <h3>空间信息</h3>
          <div v-if="spaceStore.currentSpace" class="space-info">
            <p class="author" v-if="spaceStore.currentSpace.creator">
              创作者：{{ spaceStore.currentSpace.creator.displayName }}
            </p>
            <p class="desc">{{ spaceStore.currentSpace.description }}</p>
            <div class="stats">
              <span>🟢 {{ spaceStore.currentSpace.onlineCount }} 在线</span>
              <span>👁️ {{ spaceStore.currentSpace.totalVisits?.toLocaleString() }} 访问</span>
            </div>
            <div class="tags" v-if="spaceStore.currentSpace.tags?.length">
              <span v-for="tag in spaceStore.currentSpace.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
          <div v-else-if="spaceStore.loading" class="info-loading">加载中...</div>
          <div v-else class="info-loading">空间信息不可用</div>
        </div>

        <div class="panel-section chat-section">
          <h3>AI 虚拟人 {{ isStreaming ? '💭' : '🤖' }}</h3>
          <div class="chat-messages">
            <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-bubble', msg.role]">
              {{ msg.content }}
            </div>
            <div v-if="chatMessages.length === 0" class="chat-empty">
              与空间中的 AI 虚拟人对话
            </div>
          </div>
          <div class="chat-input-area">
            <input
              v-model="chatInput"
              type="text"
              placeholder="输入消息..."
              :disabled="isStreaming"
              @keyup.enter="sendChatMessage"
            />
            <button @click="sendChatMessage" :disabled="isStreaming">
              {{ isStreaming ? '...' : '发送' }}
            </button>
          </div>
        </div>

        <div class="panel-section">
          <h3>在线用户</h3>
          <div class="online-list">
            <div v-if="spaceStore.currentSpace?.participants?.length" class="participant-items">
              <div v-for="p in spaceStore.currentSpace.participants" :key="p.userId" class="participant-item">
                <div class="participant-avatar">
                  <img v-if="p.avatarUrl" :src="p.avatarUrl" :alt="p.displayName" />
                  <span v-else>👤</span>
                </div>
                <span class="participant-name">{{ p.displayName }}</span>
                <span class="online-dot" :class="{ online: p.isOnline }"></span>
              </div>
            </div>
            <div v-else class="online-empty">暂无其他用户</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.space-detail-view {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #000;
}

.viewport-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.top-toolbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 16px;
  padding-left: 296px;
  padding-right: 336px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  z-index: 10;

  .toolbar-btn {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: #e6edf3;
    padding: 4px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    &:hover { background: rgba(255, 255, 255, 0.2); }
  }

  .space-name { font-size: 14px; font-weight: 600; color: #e6edf3; }

  .toolbar-actions {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 12px;

    .fps-display { font-family: monospace; font-size: 12px; color: #0f0; }
    .gpu-tag {
      font-size: 11px;
      color: #58a6ff;
      background: rgba(88, 166, 255, 0.15);
      padding: 2px 6px;
      border-radius: 4px;
    }
    .webrtc-indicator { font-size: 14px; opacity: 0.4; &.connected { opacity: 1; } }
  }
}

.side-panel {
  position: absolute;
  top: 48px;
  right: 0;
  bottom: 0;
  width: 320px;
  background: rgba(13, 17, 23, 0.92);
  backdrop-filter: blur(12px);
  border-left: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  z-index: 10;
  overflow-y: auto;

  .panel-section {
    padding: 16px;
    border-bottom: 1px solid #21262d;
    h3 { margin: 0 0 12px; font-size: 14px; color: #58a6ff; }
  }

  .space-info {
    .author { font-size: 13px; color: #8b949e; margin: 0 0 8px; }
    .desc { font-size: 13px; color: #c9d1d9; margin: 0 0 12px; line-height: 1.5; }
    .stats { display: flex; gap: 16px; font-size: 12px; color: #6e7681; margin-bottom: 8px; }
    .tags {
      display: flex; gap: 6px; flex-wrap: wrap;
      .tag {
        font-size: 11px; color: #58a6ff;
        background: rgba(88, 166, 255, 0.1);
        padding: 2px 8px; border-radius: 10px;
      }
    }
  }
  .info-loading { color: #484f58; font-size: 13px; text-align: center; padding: 20px 0; }
}

.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;

  .chat-messages {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 12px;
    min-height: 120px;
    max-height: 260px;
  }

  .chat-bubble {
    padding: 6px 10px;
    border-radius: 8px;
    font-size: 13px;
    max-width: 85%;
    word-break: break-word;

    &.user { align-self: flex-end; background: #1f6feb; color: #fff; }
    &.avatar { align-self: flex-start; background: #21262d; color: #c9d1d9; }
  }

  .chat-empty { text-align: center; color: #484f58; font-size: 13px; padding: 20px 0; }

  .chat-input-area {
    display: flex; gap: 8px;
    input {
      flex: 1;
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 8px 12px;
      color: #e6edf3;
      font-size: 13px;
      outline: none;
      &:focus { border-color: #58a6ff; }
      &:disabled { opacity: 0.5; }
    }
    button {
      background: #238636;
      border: none;
      color: #fff;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      &:hover { background: #2ea043; }
      &:disabled { opacity: 0.5; cursor: not-allowed; }
    }
  }
}

.online-list {
  .online-empty { text-align: center; color: #484f58; font-size: 13px; padding: 16px 0; }
  .participant-items { display: flex; flex-direction: column; gap: 8px; }
  .participant-item {
    display: flex; align-items: center; gap: 10px;
    .participant-avatar {
      width: 28px; height: 28px; border-radius: 50%;
      background: #21262d; display: flex; align-items: center; justify-content: center;
      overflow: hidden; font-size: 14px;
      img { width: 100%; height: 100%; object-fit: cover; }
    }
    .participant-name { flex: 1; font-size: 13px; color: #c9d1d9; }
    .online-dot { width: 8px; height: 8px; border-radius: 50%; background: #484f58; &.online { background: #3fb950; } }
  }
}
</style>
