<template>
  <div class="inbox-view">
    <header class="inbox-header"><h2>消息</h2></header>
    <div class="message-list">
      <div v-for="msg in messages" :key="msg.messageId" class="msg-item" :class="{ unread: !msg.isRead }">
        <div class="msg-icon">{{ typeIcon(msg.type) }}</div>
        <div class="msg-body">
          <h4>{{ msg.title }}</h4>
          <p>{{ msg.body }}</p>
        </div>
        <span class="msg-time">{{ formatTime(msg.createdAt) }}</span>
      </div>
    </div>
    <p v-if="messages.length === 0" class="empty-text">暂无消息</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const messages = ref([
  { messageId: '1', type: 'SYSTEM', title: '欢迎来到索拉', body: '开始探索虚拟空间吧！', isRead: false, createdAt: '2026-06-29T08:00:00Z' },
  { messageId: '2', type: 'SOCIAL', title: 'Alice 邀请你', body: '快来我的空间"赛博茶馆"一起聊天', isRead: true, createdAt: '2026-06-28T18:30:00Z' },
])

function typeIcon(type: string) {
  const icons: Record<string, string> = { SYSTEM: '📢', SOCIAL: '💬', SPACE: '🌟', MONETIZATION: '💰' }
  return icons[type] || '📌'
}

function formatTime(iso: string) {
  return new Date(iso).toLocaleDateString('zh-CN')
}
</script>

<style lang="scss" scoped>
.inbox-view { max-width: 680px; margin: 0 auto; padding: 24px; min-height: 100vh; }
.inbox-header { margin-bottom: 20px; h2 { font-size: 28px; } }
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
</style>
