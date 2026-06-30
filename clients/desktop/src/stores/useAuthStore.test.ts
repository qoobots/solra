import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Both mockApi must be in vi.hoisted for the vi.mock factories
const { mockApi } = vi.hoisted(() => {
  const mockApi = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  }
  return { mockApi }
})

// Mock axios so that @/api/index.ts's top-level axios.create() doesn't fail
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

import { useAuthStore } from './useAuthStore'

describe('useAuthStore (Desktop)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useAuthStore()
    expect(store.user).toBeNull()
    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.isAuthenticated).toBe(false)
  })

  it('should be authenticated when token exists in localStorage', () => {
    localStorage.setItem('solra_token', 'test-token-123')
    const store = useAuthStore()
    expect(store.accessToken).toBe('test-token-123')
    expect(store.isAuthenticated).toBe(true)
  })

  it('should login successfully', async () => {
    const store = useAuthStore()

    const loginResponse = {
      accessToken: 'jwt-access-token',
      refreshToken: 'jwt-refresh-token',
    }
    const profileResponse = {
      userId: 'user-1',
      displayName: 'TestUser',
      avatarUrl: 'https://example.com/avatar.png',
      bio: 'Hello world',
      faithLevel: 5,
      spaceCount: 3,
      email: 'test@example.com',
      phoneNumber: '13800138000',
      createdAt: '2024-01-01',
    }

    mockApi.post.mockResolvedValueOnce(loginResponse)
    mockApi.get.mockResolvedValueOnce(profileResponse)

    const result = await store.login('test@example.com', 'password123')

    expect(result).toBe(true)
    expect(store.accessToken).toBe('jwt-access-token')
    expect(store.refreshToken).toBe('jwt-refresh-token')
    expect(store.error).toBeNull()
    expect(localStorage.getItem('solra_token')).toBe('jwt-access-token')
    expect(localStorage.getItem('solra_refresh_token')).toBe('jwt-refresh-token')
    expect(store.user?.displayName).toBe('TestUser')
    expect(store.user?.faithLevel).toBe(5)
  })

  it('should handle login failure', async () => {
    const store = useAuthStore()

    mockApi.post.mockRejectedValueOnce(new Error('Invalid credentials'))

    const result = await store.login('bad@example.com', 'wrong')

    expect(result).toBe(false)
    expect(store.accessToken).toBeNull()
    expect(store.error).toBe('Invalid credentials')
  })

  it('should handle login failure with response data message', async () => {
    const store = useAuthStore()

    mockApi.post.mockRejectedValueOnce({
      response: { data: { message: '账号或密码错误' } },
      message: 'Request failed',
    })

    const result = await store.login('bad@example.com', 'wrong')

    expect(result).toBe(false)
    expect(store.error).toBe('账号或密码错误')
  })

  it('should register successfully', async () => {
    const store = useAuthStore()

    mockApi.post.mockResolvedValueOnce({
      accessToken: 'reg-token',
      refreshToken: 'reg-refresh',
    })
    mockApi.get.mockResolvedValueOnce({
      userId: 'new-user',
      displayName: 'NewUser',
      avatarUrl: '',
      bio: '',
      faithLevel: 0,
      spaceCount: 0,
      email: 'new@example.com',
      phoneNumber: '',
      createdAt: '2024-06-01',
    })

    const result = await store.register({
      email: 'new@example.com',
      password: 'pass123',
      displayName: 'NewUser',
    })

    expect(result).toBe(true)
    expect(store.accessToken).toBe('reg-token')
    expect(store.user?.displayName).toBe('NewUser')
  })

  it('should handle register failure', async () => {
    const store = useAuthStore()

    mockApi.post.mockRejectedValueOnce({
      response: { data: { message: '邮箱已被注册' } },
    })

    const result = await store.register({
      email: 'existing@example.com',
      password: 'pass123',
      displayName: 'Dup',
    })

    expect(result).toBe(false)
    expect(store.error).toBe('邮箱已被注册')
  })

  it('should logout and clear all state', () => {
    localStorage.setItem('solra_token', 'some-token')
    localStorage.setItem('solra_refresh_token', 'some-refresh')

    const store = useAuthStore()
    store.$patch({
      accessToken: 'some-token',
      refreshToken: 'some-refresh',
      user: {
        userId: 'u1',
        displayName: 'Test',
        avatarUrl: '',
        bio: '',
        faithLevel: 1,
        spaceCount: 0,
        email: '',
        phoneNumber: '',
        createdAt: '',
      },
    })

    store.logout()

    expect(store.accessToken).toBeNull()
    expect(store.refreshToken).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(localStorage.getItem('solra_token')).toBeNull()
    expect(localStorage.getItem('solra_refresh_token')).toBeNull()
  })

  it('should compute userDisplayName correctly', () => {
    const store = useAuthStore()

    expect(store.userDisplayName).toBe('')

    store.$patch({
      user: {
        userId: 'u1',
        displayName: 'Alice',
        avatarUrl: '',
        bio: '',
        faithLevel: 1,
        spaceCount: 0,
        email: '',
        phoneNumber: '',
        createdAt: '',
      },
    })

    expect(store.userDisplayName).toBe('Alice')
  })

  it('should handle fetchProfile error gracefully', async () => {
    const store = useAuthStore()

    mockApi.post.mockResolvedValueOnce({
      accessToken: 'tok',
      refreshToken: 'ref',
    })
    mockApi.get.mockRejectedValueOnce(new Error('Network error'))

    const result = await store.login('test@example.com', 'pass')

    expect(result).toBe(true)
    expect(store.accessToken).toBe('tok')
    expect(store.user).toBeNull()
  })

  it('should set loading state during async operations', async () => {
    const store = useAuthStore()

    const loadingStates: boolean[] = []
    store.$subscribe(() => {
      loadingStates.push(store.loading)
    })

    mockApi.post.mockResolvedValueOnce({
      accessToken: 'tok',
      refreshToken: 'ref',
    })
    mockApi.get.mockResolvedValueOnce({
      userId: 'u1',
      displayName: 'T',
      avatarUrl: '',
      bio: '',
      faithLevel: 0,
      spaceCount: 0,
      email: '',
      phoneNumber: '',
      createdAt: '',
    })

    await store.login('test@example.com', 'pass')

    expect(store.loading).toBe(false)
    expect(loadingStates).toContain(true)
  })
})
