<script setup lang="ts">
/**
 * 3D 空间详情页 — Desktop 核心页面
 * 嵌入 RenderViewport 组件进行 OpenGL/Vulkan 原生渲染
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useRendererStore } from '@/stores/useRendererStore'
import { useAuthStore } from '@/stores/useAuthStore'
import RenderViewport from '@/components/renderer/RenderViewport.vue'

const route = useRoute()
const router = useRouter()
const spaceStore = useSpaceStore()
const rendererStore = useRendererStore()
const authStore = useAuthStore()

const spaceId = route.params.spaceId as string
const rendererReady = ref(false)
const currentFps = ref(60)
const chatInput = ref('')
const chatMessages = ref<{ role: 'user' | 'avatar'; content: string }[]>([])
const isInSpace = ref(false)

onMounted(async () => {
  await spaceStore.enterSpace(spaceId)
  isInSpace.value = true
})

onUnmounted(async () => {
  if (isInSpace.value) {
    await spaceStore.exitSpace(spaceId)
  }
})

function onRendererReady() {
  rendererReady.value = true
}

function onFpsUpdate(fps: number) {
  currentFps.value = fps
}

async function sendChatMessage() {
  if (!chatInput.value.trim()) return
  const msg = chatInput.value.trim()
  chatMessages.value.push({ role: 'user', content: msg })
  chatInput.value = ''

  // TODO: 调用 AI 虚拟人对话
  chatMessages.value.push({ role: 'avatar', content: `收到你的消息：${msg}` })
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
        @ready="onRendererReady"
        @fps-update="onFpsUpdate"
      />

      <!-- 顶部工具栏 -->
      <div class="top-toolbar">
        <button class="toolbar-btn" @click="goBack">
          ← 返回
        </button>
        <span class="space-name">{{ spaceStore.currentSpace?.name || spaceId }}</span>
        <div class="toolbar-actions">
          <span class="fps-display">{{ currentFps }} FPS</span>
          <span class="gpu-tag">{{ rendererStore.gpuBackend }}</span>
        </div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panel">
        <div class="panel-section">
          <h3>空间信息</h3>
          <div v-if="spaceStore.currentSpace" class="space-info">
            <p class="author">作者：{{ spaceStore.currentSpace.author_name }}</p>
            <p class="desc">{{ spaceStore.currentSpace.description }}</p>
            <div class="stats">
              <span>👁️ {{ spaceStore.currentSpace.visitor_count?.toLocaleString() }}</span>
              <span>❤️ {{ spaceStore.currentSpace.like_count }}</span>
            </div>
            <div class="tags" v-if="spaceStore.currentSpace.tags?.length">
              <span v-for="tag in spaceStore.currentSpace.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
        </div>

        <div class="panel-section chat-section">
          <h3>AI 虚拟人</h3>
          <div class="chat-messages">
            <div
              v-for="(msg, idx) in chatMessages"
              :key="idx"
              :class="['chat-bubble', msg.role]"
            >
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
              @keyup.enter="sendChatMessage"
            />
            <button @click="sendChatMessage">发送</button>
          </div>
        </div>

        <div class="panel-section">
          <h3>在线用户</h3>
          <div class="online-list">
            <div class="online-empty">暂无其他用户</div>
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

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }
  }

  .space-name {
    font-size: 14px;
    font-weight: 600;
    color: #e6edf3;
  }

  .toolbar-actions {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 12px;

    .fps-display {
      font-family: monospace;
      font-size: 12px;
      color: #0f0;
    }

    .gpu-tag {
      font-size: 11px;
      color: #58a6ff;
      background: rgba(88, 166, 255, 0.15);
      padding: 2px 6px;
      border-radius: 4px;
    }
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

    h3 {
      margin: 0 0 12px;
      font-size: 14px;
      color: #58a6ff;
    }
  }

  .space-info {
    .author {
      font-size: 13px;
      color: #8b949e;
      margin: 0 0 8px;
    }

    .desc {
      font-size: 13px;
      color: #c9d1d9;
      margin: 0 0 12px;
      line-height: 1.5;
    }

    .stats {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #6e7681;
      margin-bottom: 8px;
    }

    .tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;

      .tag {
        font-size: 11px;
        color: #58a6ff;
        background: rgba(88, 166, 255, 0.1);
        padding: 2px 8px;
        border-radius: 10px;
      }
    }
  }
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
  }

  .chat-bubble {
    padding: 6px 10px;
    border-radius: 8px;
    font-size: 13px;
    max-width: 85%;
    word-break: break-word;

    &.user {
      align-self: flex-end;
      background: #1f6feb;
      color: #fff;
    }

    &.avatar {
      align-self: flex-start;
      background: #21262d;
      color: #c9d1d9;
    }
  }

  .chat-empty {
    text-align: center;
    color: #484f58;
    font-size: 13px;
    padding: 20px 0;
  }

  .chat-input-area {
    display: flex;
    gap: 8px;

    input {
      flex: 1;
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 8px 12px;
      color: #e6edf3;
      font-size: 13px;
      outline: none;

      &:focus {
        border-color: #58a6ff;
      }
    }

    button {
      background: #238636;
      border: none;
      color: #fff;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;

      &:hover {
        background: #2ea043;
      }
    }
  }
}

.online-list {
  .online-empty {
    text-align: center;
    color: #484f58;
    font-size: 13px;
    padding: 16px 0;
  }
}
</style>
