<script setup lang="ts">
import { ref } from 'vue'

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
