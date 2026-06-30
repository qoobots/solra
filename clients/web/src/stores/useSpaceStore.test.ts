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

import { useSpaceStore } from '@/stores/useSpaceStore'

const mockSpace = (overrides: Record<string, any> = {}) => ({
  spaceId: 's1', title: 'Space 1', description: 'Desc 1', thumbnailUrl: '/img1.jpg',
  tags: ['explore'], category: 'SOCIAL', visibility: 'PUBLIC',
  creator: { userId: 'u1', displayName: 'Creator1', avatarUrl: '' },
  onlineCount: 5, totalVisits: 100, faithScore: 4.5, createdAt: '2026-01-01', updatedAt: '2026-01-02',
  ...overrides,
})

describe('useSpaceStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have empty initial state', () => {
    const store = useSpaceStore()
    expect(store.spaces).toEqual([])
    expect(store.currentSpace).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('should fetch spaces', async () => {
    mockAxios.get.mockResolvedValueOnce({
      spaces: [mockSpace(), mockSpace({ spaceId: 's2' })],
      totalCount: 2,
    })
    const store = useSpaceStore()
    await store.fetchSpaces()
    expect(store.spaces).toHaveLength(2)
    expect(store.totalSpaces).toBe(2)
  })

  it('should reset on fetchSpaces(true)', async () => {
    mockAxios.get.mockResolvedValueOnce({ spaces: [mockSpace()], totalCount: 1 })
    const store = useSpaceStore()
    store.page = 5
    store.hasMore = false
    await store.fetchSpaces(true)
    expect(store.page).toBe(1)
  })

  it('should not fetch when hasMore is false', async () => {
    const store = useSpaceStore()
    store.hasMore = false
    await store.fetchSpaces()
    expect(mockAxios.get).not.toHaveBeenCalled()
  })

  it('should handle API errors', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Network error'))
    const store = useSpaceStore()
    await store.fetchSpaces()
    expect(store.error).toBe('Network error')
  })

  it('should fetch space detail', async () => {
    mockAxios.get.mockResolvedValueOnce({
      ...mockSpace({ spaceId: 's-detail' }),
      sceneGraph: { nodes: [] }, settings: {}, participants: [],
    })
    const store = useSpaceStore()
    await store.fetchSpaceDetail('s-detail')
    expect(store.currentSpace?.spaceId).toBe('s-detail')
  })

  it('should search spaces', async () => {
    mockAxios.get.mockResolvedValueOnce({
      spaces: [mockSpace({ title: 'Result 1' })], totalCount: 1,
    })
    const store = useSpaceStore()
    await store.searchSpaces('test')
    expect(store.spaces[0].title).toBe('Result 1')
  })

  it('should create space', async () => {
    mockAxios.post.mockResolvedValueOnce(mockSpace({ spaceId: 'new', title: 'New Space' }))
    const store = useSpaceStore()
    const result = await store.createSpace({
      title: 'New Space', description: '', tags: [], visibility: 'PUBLIC',
    })
    expect(result).not.toBeNull()
    expect(store.spaces).toHaveLength(1)
  })

  it('should handle create space failure', async () => {
    mockAxios.post.mockRejectedValueOnce({ response: { data: { message: 'Error' } } })
    const store = useSpaceStore()
    const result = await store.createSpace({
      title: 'Bad', description: '', tags: [], visibility: 'PUBLIC',
    })
    expect(result).toBeNull()
  })

  it('should return empty array on recommendation error', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Failed'))
    const store = useSpaceStore()
    const items = await store.fetchRecommendations()
    expect(items).toEqual([])
  })

  it('should sort by onlineCount descending', () => {
    const store = useSpaceStore()
    store.spaces = [
      mockSpace({ spaceId: 'a', onlineCount: 3 }),
      mockSpace({ spaceId: 'b', onlineCount: 10 }),
      mockSpace({ spaceId: 'c', onlineCount: 5 }),
    ] as any
    const sorted = store.sortedSpaces
    expect(sorted[0].onlineCount).toBe(10)
    expect(sorted[2].onlineCount).toBe(3)
  })
})
