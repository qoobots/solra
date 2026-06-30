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

import { useLeaderboardStore } from '@/stores/useLeaderboardStore'

describe('useLeaderboardStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have default board type and period', () => {
    const store = useLeaderboardStore()
    expect(store.boardType).toBe('FAITH_LEVEL')
    expect(store.period).toBe('ALL_TIME')
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('should have empty entries for all types', () => {
    const store = useLeaderboardStore()
    expect(store.entries.FAITH_LEVEL).toEqual([])
    expect(store.entries.SPACE_COUNT).toEqual([])
    expect(store.entries.SOCIAL_SCORE).toEqual([])
  })

  it('should fetch faith level leaderboard', async () => {
    mockAxios.get.mockResolvedValueOnce({
      entries: [
        { rank: 1, user: { userId: 'u1', displayName: 'Top User', avatarUrl: '' }, score: 9999 },
        { rank: 2, user: { userId: 'u2', displayName: 'Second', avatarUrl: '' }, score: 8500 },
      ],
    })
    const store = useLeaderboardStore()
    await store.fetchLeaderboard()
    expect(store.entries.FAITH_LEVEL).toHaveLength(2)
    expect(store.entries.FAITH_LEVEL[0].user.displayName).toBe('Top User')
    expect(store.entries.FAITH_LEVEL[0].rank).toBe(1)
  })

  it('should switch board type', async () => {
    mockAxios.get.mockResolvedValueOnce({
      entries: [{ rank: 1, user: { userId: 'u1', displayName: 'Builder', avatarUrl: '' }, score: 100 }],
    })
    const store = useLeaderboardStore()
    await store.fetchLeaderboard('SPACE_COUNT')
    expect(store.boardType).toBe('SPACE_COUNT')
    expect(store.entries.SPACE_COUNT).toHaveLength(1)
  })

  it('should handle missing rank with index', async () => {
    mockAxios.get.mockResolvedValueOnce({
      entries: [
        { user: { userId: 'u1', displayName: 'U1', avatarUrl: '' }, score: 500 },
        { user: { userId: 'u2', displayName: 'U2', avatarUrl: '' }, score: 400 },
      ],
    })
    const store = useLeaderboardStore()
    await store.fetchLeaderboard()
    expect(store.entries.FAITH_LEVEL[0].rank).toBe(1)
    expect(store.entries.FAITH_LEVEL[1].rank).toBe(2)
  })

  it('should handle API error', async () => {
    mockAxios.get.mockRejectedValueOnce(new Error('Service unavailable'))
    const store = useLeaderboardStore()
    await store.fetchLeaderboard()
    expect(store.error).toBe('Service unavailable')
  })
})
