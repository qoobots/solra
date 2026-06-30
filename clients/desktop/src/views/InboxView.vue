<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import api from '@/api'

interface Message {
  id: string
  type: 'system' | 'user' | 'mention'
  title: string
  content: string
  sender: string
  timestamp: string
  read: boolean
}

const messages = ref<Message[]>([])
const activeTab = ref<'all' | 'unread'>('all')
const loading = ref(false)

async function fetchMessages(tab: string) {
  loading.value = true
  try {
    // 优先 HTTP API
    const params: Record<string, string> = {}
    if (tab === 'unread') params.tab = 'unread'
    const res = await api.get('/api/msg/v1/messages', { params }) as any
    const data = (res.messages || res.data || []).map((m: any) => ({
      id: m.messageId || m.id || '',
      type: (m.type as 'system' | 'user' | 'mention') || 'system',
      title: m.title || '',
      content: m.content || '',
      sender: m.sender || '系统',
      timestamp: m.timestamp || m.createdAt || '',
      read: m.read ?? false,
    }))
    messages.value = data
  } catch {
    // 回退到 Tauri IPC
    try {
      const t = activeTab.value !== 'all' ? activeTab.value : undefined
      const data = await invoke<any[]>('get_messages', { tab: t })
      messages.value = data.map((m: any) => ({
        id: m.id,
        type: (m.msg_type as 'system' | 'user' | 'mention') || 'system',
        title: m.title,
        content: m.content,
        sender: m.sender,
        timestamp: m.timestamp,
        read: m.read,
      }))
    } catch (e) {
      console.error('加载消息失败:', e)
      messages.value = []
    }
  } finally {
    loading.value = false
  }
}

watch(activeTab, (tab) => {
  fetchMessages(tab)
})

onMounted(() => {
  fetchMessages(activeTab.value)
})
</script>

<template>
  <div class="inbox-view">
    <header class="page-header">
      <h1>消息</h1>
    </header>

    <div class="tab-bar">
      <button
        :class="['tab-btn', { active: activeTab === 'all' }]"
        @click="activeTab = 'all'"
      >
        全部
      </button>
      <button
        :class="['tab-btn', { active: activeTab === 'unread' }]"
        @click="activeTab = 'unread'"
      >
        未读
        <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
      </button>
    </div>

    <div class="inbox-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="messages.length === 0" class="empty-state">
        <div class="empty-icon">📬</div>
        <p>暂无消息</p>
      </div>
      <div v-else class="message-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-item', { unread: !msg.read }]"
        >
          <div :class="['type-indicator', msg.type]"></div>
          <div class="message-body">
            <div class="message-header">
              <span class="sender">{{ msg.sender }}</span>
              <span class="time">{{ msg.timestamp }}</span>
            </div>
            <h3>{{ msg.title }}</h3>
            <p>{{ msg.content }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.inbox-view {
  min-height: 100vh;
  background: var(--solra-bg-primary, #0d1117);
  color: var(--solra-text-primary, #e6edf3);
}

.page-header {
  padding: 16px 32px;
  border-bottom: 1px solid var(--solra-border, #30363d);

  h1 {
    margin: 0;
    font-size: 20px;
  }
}

.tab-bar {
  display: flex;
  gap: 8px;
  padding: 16px 32px;
  border-bottom: 1px solid #21262d;

  .tab-btn {
    background: none;
    border: 1px solid #30363d;
    color: #8b949e;
    padding: 6px 20px;
    border-radius: 20px;
    cursor: pointer;
    font-size: 13px;
    position: relative;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      background: #1f6feb;
      border-color: #1f6feb;
      color: #fff;
    }

    .badge {
      margin-left: 4px;
      background: #f85149;
      color: #fff;
      font-size: 10px;
      padding: 1px 6px;
      border-radius: 10px;
    }
  }
}

.inbox-content {
  max-width: 700px;
  margin: 0 auto;
  padding: 24px 32px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  p {
    font-size: 16px;
    color: #8b949e;
    margin: 0;
  }
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: #58a6ff;
  }

  &.unread {
    border-left: 3px solid #58a6ff;
  }

  .type-indicator {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-top: 6px;
    flex-shrink: 0;

    &.system { background: #f0c040; }
    &.user { background: #58a6ff; }
    &.mention { background: #f85149; }
  }

  .message-body {
    flex: 1;

    .message-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;

      .sender {
        font-size: 13px;
        color: #58a6ff;
      }

      .time {
        font-size: 11px;
        color: #484f58;
      }
    }

    h3 {
      margin: 0 0 4px;
      font-size: 15px;
    }

    p {
      margin: 0;
      font-size: 13px;
      color: #8b949e;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }
}

.loading {
  text-align: center;
  padding: 60px;
  color: #8b949e;
}
</style>

<script lang="ts">
const unreadCount = 0
</script>
