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

import { useStoreStore } from '@/stores/useStoreStore'

const mockItem = (overrides: Record<string, any> = {}) => ({
  itemId: 'i1', name: 'Skin A', description: '', price: 10, currency: 'CNY',
  category: 'AVATAR_SKIN', icon: '', previewUrl: '', isOwned: false, createdAt: '',
  ...overrides,
})

describe('useStoreStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have empty initial state', () => {
    const store = useStoreStore()
    expect(store.items).toEqual([])
    expect(store.subscriptions).toEqual([])
    expect(store.selectedCategory).toBe('all')
  })

  it('should have 6 categories', () => {
    const store = useStoreStore()
    expect(store.categories).toHaveLength(6)
  })

  it('should filter items by category', () => {
    const store = useStoreStore()
    store.selectedCategory = 'AVATAR_SKIN'
    store.items = [mockItem(), mockItem({ itemId: 'i2', category: 'EFFECT' })] as any
    expect(store.filteredItems).toHaveLength(1)
  })

  it('should fetch store items', async () => {
    mockAxios.get.mockResolvedValueOnce({
      items: [
        { itemId: 'i1', name: 'Cool Skin', price: 29.9, category: 'AVATAR_SKIN' },
        { itemId: 'i2', name: 'Fire Effect', price: 15.0, category: 'EFFECT' },
      ],
    })
    const store = useStoreStore()
    await store.fetchItems()
    expect(store.items).toHaveLength(2)
    expect(store.items[0].name).toBe('Cool Skin')
    expect(store.items[0].price).toBe(29.9)
  })

  it('should handle fetch error', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Server down'))
    const store = useStoreStore()
    await store.fetchItems()
    expect(store.error).toBe('Server down')
  })

  it('should handle subscription fetch error gracefully', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Failed'))
    const store = useStoreStore()
    await store.fetchSubscriptions()
    expect(store.subscriptions).toEqual([])
  })

  it('should purchase and mark as owned', async () => {
    mockAxios.post.mockResolvedValueOnce({})
    const store = useStoreStore()
    store.items = [mockItem()] as any
    const result = await store.purchaseItem('i1')
    expect(result).toBe(true)
    expect(store.items[0].isOwned).toBe(true)
  })

  it('should handle purchase failure', async () => {
    mockAxios.post.mockRejectedValueOnce({
      response: { data: { message: 'Insufficient balance' } },
    })
    const store = useStoreStore()
    const result = await store.purchaseItem('i1')
    expect(result).toBe(false)
    expect(store.error).toBe('Insufficient balance')
  })
})
