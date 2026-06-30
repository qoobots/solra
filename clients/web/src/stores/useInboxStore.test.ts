import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const { mockAxios } = vi.hoisted(() => {
  const mockAxios = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
    interceptors: {
      request: { use: vi.fn(), eject: vi.fn(), clear: vi.fn() },
      response: { use: vi.fn(), eject: vi.fn(), clear: vi.fn() },
    },
    defaults: { headers: { common: {} } },
  }
  return { mockAxios }
})

vi.mock('axios', () => ({
  default: { create: () => mockAxios },
}))

import { useInboxStore } from '@/stores/useInboxStore'

const mockMsg = (overrides: Record<string, any> = {}) => ({
  messageId: 'm1', type: 'SYSTEM', title: 'Welcome', body: 'Welcome',
  isRead: false, actionUrl: '', createdAt: '2026-06-01',
  ...overrides,
})

describe('useInboxStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have empty initial state', () => {
    const store = useInboxStore()
    expect(store.messages).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.filter).toBe('ALL')
    expect(store.unreadCount).toBe(0)
  })

  it('should fetch messages and calculate unread count', async () => {
    mockAxios.get.mockResolvedValueOnce({
      messages: [
        mockMsg(),
        mockMsg({ messageId: 'm2', type: 'SOCIAL', isRead: true }),
        mockMsg({ messageId: 'm3', type: 'SPACE' }),
      ],
    })
    const store = useInboxStore()
    await store.fetchMessages()
    expect(store.messages).toHaveLength(3)
    expect(store.unreadCount).toBe(2)
  })

  it('should reset on fetchMessages(true)', async () => {
    mockAxios.get.mockResolvedValueOnce({ messages: [mockMsg()] })
    const store = useInboxStore()
    store.page = 3
    store.messages = [mockMsg({ messageId: 'old' })] as any
    await store.fetchMessages(true)
    expect(store.messages).toHaveLength(1)
  })

  it('should mark a message as read', async () => {
    mockAxios.put.mockResolvedValueOnce({})
    const store = useInboxStore()
    store.messages = [mockMsg()] as any
    await store.markAsRead('m1')
    expect(store.messages[0].isRead).toBe(true)
  })

  it('should handle markAsRead error gracefully', async () => {
    mockAxios.put.mockRejectedValueOnce(new Error('Failed'))
    const store = useInboxStore()
    store.messages = [mockMsg()] as any
    await store.markAsRead('m1')
    expect(store.messages[0].isRead).toBe(false)
  })

  it('should mark all as read', async () => {
    mockAxios.put.mockResolvedValueOnce({})
    const store = useInboxStore()
    store.messages = [mockMsg(), mockMsg({ messageId: 'm2' })] as any
    await store.markAllAsRead()
    expect(store.unreadCount).toBe(0)
  })

  it('should filter unread messages', () => {
    const store = useInboxStore()
    store.filter = 'UNREAD'
    store.messages = [
      mockMsg(),
      mockMsg({ messageId: 'm2', isRead: true }),
      mockMsg({ messageId: 'm3' }),
    ] as any
    expect(store.filteredMessages).toHaveLength(2)
  })
})
