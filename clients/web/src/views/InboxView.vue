<template>
  <div class="inbox-view">
    <header class="inbox-header">
      <h2>消息</h2>
      <div class="inbox-actions">
        <el-button-group>
          <el-button :type="filter === 'ALL' ? 'primary' : 'default'" size="small" @click="filter = 'ALL'">
            全部
          </el-button>
          <el-button :type="filter === 'UNREAD' ? 'primary' : 'default'" size="small" @click="filter = 'UNREAD'">
            未读 {{ unreadCount > 0 ? `(${unreadCount})` : '' }}
          </el-button>
        </el-button-group>
        <el-button size="small" text @click="handleMarkAllRead" v-if="unreadCount > 0">
          全部标为已读
        </el-button>
      </div>
    </header>

    <div v-if="loading && filteredMessages.length === 0" class="loading-state">
      <p>正在加载消息...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <el-button @click="inboxStore.fetchMessages(true)">重试</el-button>
    </div>

    <div v-else class="message-list">
      <div
        v-for="msg in filteredMessages"
        :key="msg.messageId"
        class="msg-item"
        :class="{ unread: !msg.isRead }"
        @click="handleMarkRead(msg.messageId)"
      >
        <div class="msg-icon">{{ typeIcon(msg.type) }}</div>
        <div class="msg-body">
          <h4>{{ msg.title }}</h4>
          <p>{{ msg.body }}</p>
        </div>
        <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
      </div>
    </div>
    <p v-if="filteredMessages.length === 0 && !loading" class="empty-text">
      {{ filter === 'UNREAD' ? '没有未读消息' : '暂无消息' }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useInboxStore } from '@/stores/useInboxStore'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'

const inboxStore = useInboxStore()
const { filteredMessages, loading, error, filter, unreadCount } = storeToRefs(inboxStore)

onMounted(() => {
  inboxStore.fetchMessages(true)
})

function typeIcon(type: string) {
  const icons: Record<string, string> = { SYSTEM: '📢', SOCIAL: '💬', SPACE: '🌟', MONETIZATION: '💰' }
  return icons[type] || '📌'
}

function formatTime(iso: string) {
  const date = new Date(iso)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

async function handleMarkRead(messageId: string) {
  await inboxStore.markAsRead(messageId)
}

async function handleMarkAllRead() {
  await inboxStore.markAllAsRead()
  ElMessage.success('已全部标为已读')
}
</script>

<style lang="scss" scoped>
.inbox-view { max-width: 680px; margin: 0 auto; padding: 24px; min-height: 100vh; }
.inbox-header {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 12px;
  margin-bottom: 20px;
  h2 { font-size: 28px; }
  .inbox-actions { display: flex; align-items: center; gap: 12px; }
}
.msg-item {
  display: flex; align-items: flex-start; gap: 14px;
  padding: 16px;
  background: var(--solra-bg-card);
  border: 1px solid var(--solra-border);
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  &.unread { border-color: var(--solra-accent); background: rgba(108, 92, 231, 0.08); }
  .msg-icon { font-size: 24px; }
  .msg-body { flex: 1; h4 { font-size: 15px; margin-bottom: 4px; } p { color: var(--solra-text-secondary); font-size: 13px; } }
  .msg-time { color: var(--solra-text-secondary); font-size: 12px; white-space: nowrap; }
}
.empty-text { text-align: center; color: var(--solra-text-secondary); margin-top: 60px; }
.loading-state, .error-state { text-align: center; padding: 60px 20px; color: var(--solra-text-secondary); }
</style>
