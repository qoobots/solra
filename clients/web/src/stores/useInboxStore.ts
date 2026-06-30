import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface NotificationMessage {
  messageId: string
  type: 'SYSTEM' | 'SOCIAL' | 'SPACE' | 'MONETIZATION'
  title: string
  body: string
  isRead: boolean
  actionUrl: string
  createdAt: string
}

export const useInboxStore = defineStore('inbox', () => {
  const messages = ref<NotificationMessage[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const page = ref(0)
  const hasMore = ref(true)
  const filter = ref<'ALL' | 'UNREAD'>('ALL')

  const unreadCount = computed(() => messages.value.filter(m => !m.isRead).length)
  const filteredMessages = computed(() =>
    filter.value === 'UNREAD'
      ? messages.value.filter(m => !m.isRead)
      : messages.value
  )

  async function fetchMessages(reset = false): Promise<void> {
    if (reset) {
      page.value = 0
      hasMore.value = true
    }
    if (!hasMore.value && !reset) return
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/api/not/v1/messages', {
        params: { page: page.value, pageSize: 20 },
      }) as any
      const newMsgs = (res.messages || res.data || []).map(mapMessage)
      if (reset) {
        messages.value = newMsgs
      } else {
        messages.value = [...messages.value, ...newMsgs]
      }
      hasMore.value = newMsgs.length >= 20
      page.value++
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '加载消息失败'
    } finally {
      loading.value = false
    }
  }

  async function markAsRead(messageId: string): Promise<void> {
    try {
      await api.put(`/api/not/v1/messages/${messageId}/read`)
      const msg = messages.value.find(m => m.messageId === messageId)
      if (msg) msg.isRead = true
    } catch (e: any) {
      console.error('Failed to mark message as read:', e)
    }
  }

  async function markAllAsRead(): Promise<void> {
    try {
      await api.put('/api/not/v1/messages/read-all')
      messages.value.forEach(m => (m.isRead = true))
    } catch (e: any) {
      console.error('Failed to mark all messages as read:', e)
    }
  }

  function mapMessage(raw: any): NotificationMessage {
    return {
      messageId: raw.messageId || raw.id || '',
      type: raw.type || 'SYSTEM',
      title: raw.title || '',
      body: raw.body || '',
      isRead: raw.isRead ?? false,
      actionUrl: raw.actionUrl || '',
      createdAt: raw.createdAt || '',
    }
  }

  return {
    messages,
    loading,
    error,
    page,
    hasMore,
    filter,
    unreadCount,
    filteredMessages,
    fetchMessages,
    markAsRead,
    markAllAsRead,
  }
})
