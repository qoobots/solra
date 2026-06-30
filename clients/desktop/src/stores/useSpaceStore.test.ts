import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Hoisted mocks
const { mockApi, mockInvoke } = vi.hoisted(() => {
  const mockApi = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  }
  const mockInvoke = vi.fn()
  return { mockApi, mockInvoke }
})

// Mock axios
vi.mock('axios', () => ({
  default: {
    create: () => ({
      get: mockApi.get,
      post: mockApi.post,
      put: mockApi.put,
      delete: mockApi.delete,
      patch: mockApi.patch,
      interceptors: {
        request: { use: vi.fn(), eject: vi.fn(), clear: vi.fn() },
        response: { use: vi.fn(), eject: vi.fn(), clear: vi.fn() },
      },
      defaults: { headers: { common: {} } },
    }),
  },
}))

vi.mock('@/api', () => ({ default: mockApi }))
vi.mock('@tauri-apps/api/core', () => ({ invoke: mockInvoke }))

import { useSpaceStore } from './useSpaceStore'

describe('useSpaceStore (Desktop)', () => {
  const mockSpaceDetail = {
    spaceId: 'sp-1',
    title: '赛博森林',
    description: '一个充满赛博朋克风格的虚拟森林',
    thumbnailUrl: 'https://img.example.com/1.jpg',
    creator: {
      userId: 'creator-1',
      displayName: 'Alice',
      avatarUrl: 'https://img.example.com/alice.png',
    },
    onlineCount: 42,
    totalVisits: 1520,
    faithScore: 4.8,
    category: 'nature',
    tags: ['森林', '赛博朋克'],
    sceneGraph: { nodes: [] },
    settings: { bgm: 'ambient' },
    participants: [
      { userId: 'u1', displayName: 'Alice', avatarUrl: '', isOnline: true },
      { userId: 'u2', displayName: 'Charlie', avatarUrl: '', isOnline: false },
    ],
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useSpaceStore()
    expect(store.spaces).toEqual([])
    expect(store.currentSpace).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('should fetch spaces via HTTP API', async () => {
    const store = useSpaceStore()

    mockApi.get.mockResolvedValueOnce({
      spaces: [
        {
          spaceId: 'sp-1',
          title: '赛博森林',
          description: '...',
          thumbnailUrl: '',
          creator: { displayName: 'Alice' },
          onlineCount: 42,
          category: 'nature',
          tags: ['森林'],
        },
      ],
    })

    await store.fetchSpaces()

    expect(store.spaces).toHaveLength(1)
    expect(store.spaces[0].spaceId).toBe('sp-1')
    expect(store.spaces[0].title).toBe('赛博森林')
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('should fallback to Tauri IPC when HTTP API fails', async () => {
    const store = useSpaceStore()

    mockApi.get.mockRejectedValueOnce(new Error('Network error'))
    mockInvoke.mockResolvedValueOnce([
      {
        id: 'tauri-sp-1',
        name: 'Tauri Space',
        description: 'From IPC',
        thumbnail_url: '',
        author_name: 'Bob',
        visitor_count: 5,
        like_count: 10,
        category: 'tech',
        tags: ['tauri'],
      },
    ])

    await store.fetchSpaces()

    expect(store.spaces).toHaveLength(1)
    expect(store.spaces[0].spaceId).toBe('tauri-sp-1')
    expect(store.spaces[0].title).toBe('Tauri Space')
  })

  it('should set error when both HTTP and IPC fail', async () => {
    const store = useSpaceStore()

    mockApi.get.mockRejectedValueOnce(new Error('HTTP down'))
    mockInvoke.mockRejectedValueOnce(new Error('IPC down'))

    await store.fetchSpaces()

    expect(store.error).toBe('加载空间列表失败')
    expect(store.spaces).toEqual([])
  })

  it('should enter space via HTTP API', async () => {
    const store = useSpaceStore()

    mockApi.get.mockResolvedValueOnce(mockSpaceDetail)
    mockInvoke.mockResolvedValueOnce(undefined)

    await store.enterSpace('sp-1')

    expect(store.currentSpace).not.toBeNull()
    expect(store.currentSpace?.spaceId).toBe('sp-1')
    expect(store.currentSpace?.title).toBe('赛博森林')
    expect(store.currentSpace?.creator?.displayName).toBe('Alice')
    expect(store.currentSpace?.participants).toHaveLength(2)
  })

  it('should enter space with Tauri IPC fallback', async () => {
    const store = useSpaceStore()

    mockApi.get.mockRejectedValueOnce(new Error('HTTP error'))
    mockInvoke.mockResolvedValueOnce(undefined)
    mockInvoke.mockResolvedValueOnce({
      id: 'tauri-sp-1',
      name: 'Tauri Space Detail',
      description: 'Detailed',
      category: 'tech',
      tags: ['detail'],
      visitor_count: 10,
      author_name: 'TauriAuthor',
      author_id: 'ta1',
    })

    await store.enterSpace('tauri-sp-1')

    expect(store.currentSpace).not.toBeNull()
    expect(store.currentSpace?.spaceId).toBe('tauri-sp-1')
    expect(store.currentSpace?.title).toBe('Tauri Space Detail')
    expect(store.currentSpace?.creator?.displayName).toBe('TauriAuthor')
  })

  it('should handle enterSpace failure', async () => {
    const store = useSpaceStore()

    mockApi.get.mockRejectedValueOnce(new Error('HTTP error'))
    mockInvoke.mockRejectedValueOnce(new Error('IPC error'))

    await store.enterSpace('bad-id')

    expect(store.error).toBe('进入空间失败')
    expect(store.currentSpace).toBeNull()
  })

  it('should exit space and clear currentSpace', async () => {
    const store = useSpaceStore()

    store.$patch({ currentSpace: mockSpaceDetail as any })

    mockInvoke.mockResolvedValueOnce(undefined)

    await store.exitSpace('sp-1')

    expect(store.currentSpace).toBeNull()
    expect(mockInvoke).toHaveBeenCalledWith('exit_space', { spaceId: 'sp-1' })
  })

  it('should exit space even if IPC fails (graceful)', async () => {
    const store = useSpaceStore()

    store.$patch({ currentSpace: mockSpaceDetail as any })

    mockInvoke.mockRejectedValueOnce(new Error('IPC not available'))

    await store.exitSpace('sp-1')

    expect(store.currentSpace).toBeNull()
  })
})
