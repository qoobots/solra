import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Both mockApi and mockAxiosInstance must be in vi.hoisted for the vi.mock factories
const { mockApi, mockAxiosInstance } = vi.hoisted(() => {
  const mockApi = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  }
  const mockAxiosInstance = {
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
  return { mockApi, mockAxiosInstance }
})

// Mock axios so that @/api/index.ts's top-level axios.create() doesn't fail
vi.mock('axios', () => ({
  default: {
    create: () => mockAxiosInstance,
    post: vi.fn(),
    get: vi.fn(),
  },
}))

// Mock @/api so that stores get our controlled mock
vi.mock('@/api', () => ({
  default: mockApi,
}))

import { useAuthStore } from '@/stores/useAuthStore'

// Setup localStorage mock
const localStorageStore: Record<string, string> = {}
const lsMock = {
  getItem: vi.fn((key: string) => localStorageStore[key] ?? null),
  setItem: vi.fn((key: string, value: string) => { localStorageStore[key] = value }),
  removeItem: vi.fn((key: string) => { delete localStorageStore[key] }),
  clear: vi.fn(() => { Object.keys(localStorageStore).forEach(k => delete localStorageStore[k]) }),
}
Object.defineProperty(global, 'localStorage', { value: lsMock, writable: true })

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    lsMock.clear()
    vi.clearAllMocks()
  })

  it('should have null user and tokens by default', () => {
    const store = useAuthStore()
    expect(store.user).toBeNull()
    expect(store.accessToken).toBeNull()
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('isAuthenticated should be false when no token', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
  })

  it('should login successfully', async () => {
    mockApi.post.mockResolvedValueOnce({
      accessToken: 'access-123',
      refreshToken: 'refresh-123',
    })
    mockApi.get.mockResolvedValueOnce({
      userId: 'u1', displayName: 'TestUser', avatarUrl: '',
      bio: '', faithLevel: 1, spaceCount: 0,
      email: 'test@example.com', phoneNumber: '', createdAt: '2026-01-01',
    })

    const store = useAuthStore()
    const result = await store.login('test@example.com', 'password123')

    expect(result).toBe(true)
    expect(store.accessToken).toBe('access-123')
    expect(store.user?.displayName).toBe('TestUser')
  })

  it('should handle login failure', async () => {
    mockApi.post.mockRejectedValueOnce({
      response: { data: { message: 'Invalid credentials' } },
    })

    const store = useAuthStore()
    const result = await store.login('test@example.com', 'wrong')

    expect(result).toBe(false)
    expect(store.error).toBe('Invalid credentials')
  })

  it('should register user', async () => {
    mockApi.post.mockResolvedValueOnce({
      accessToken: 'access-reg', refreshToken: 'refresh-reg',
    })
    mockApi.get.mockResolvedValueOnce({
      userId: 'u2', displayName: 'NewUser', avatarUrl: '',
      bio: '', faithLevel: 0, spaceCount: 0,
      email: 'new@example.com', phoneNumber: '', createdAt: '2026-01-01',
    })

    const store = useAuthStore()
    const result = await store.register({
      email: 'new@example.com', password: 'pass123', displayName: 'NewUser',
    })

    expect(result).toBe(true)
    expect(store.user?.displayName).toBe('NewUser')
  })

  it('should logout and clear state', async () => {
    const store = useAuthStore()
    store.user = {
      userId: 'u1', displayName: 'Test', avatarUrl: '', bio: '',
      faithLevel: 1, spaceCount: 0, email: '', phoneNumber: '', createdAt: '',
    }
    await store.logout()
    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
  })

  it('should refresh token successfully', async () => {
    lsMock.setItem('solra_refresh_token', 'old-refresh')
    mockApi.post.mockResolvedValueOnce({
      accessToken: 'new-token', refreshToken: 'new-refresh',
    })

    const store = useAuthStore()
    const result = await store.refreshAccessToken()

    expect(result).toBe(true)
    expect(store.accessToken).toBe('new-token')
  })

  it('should logout on refresh failure', async () => {
    lsMock.setItem('solra_token', 'old')
    lsMock.setItem('solra_refresh_token', 'old-refresh')
    mockApi.post.mockRejectedValueOnce(new Error('Token expired'))

    const store = useAuthStore()
    const result = await store.refreshAccessToken()

    expect(result).toBe(false)
    expect(store.accessToken).toBeNull()
  })

  it('should return false when no refresh token', async () => {
    const store = useAuthStore()
    const result = await store.refreshAccessToken()
    expect(result).toBe(false)
  })

  it('should fetch profile', async () => {
    mockApi.get.mockResolvedValueOnce({
      userId: 'u-profile', displayName: 'ProfileUser',
      avatarUrl: 'https://img.example.com/u.png', bio: 'Hello world',
      faithLevel: 5, spaceCount: 10, email: 'profile@example.com',
      phoneNumber: '13800138000', createdAt: '2025-06-01',
    })

    const store = useAuthStore()
    await store.fetchProfile()

    expect(store.user?.userId).toBe('u-profile')
    expect(store.user?.faithLevel).toBe(5)
  })

  it('should handle missing profile fields', async () => {
    mockApi.get.mockResolvedValueOnce({ userId: 'minimal', displayName: 'Min' })

    const store = useAuthStore()
    await store.fetchProfile()

    expect(store.user?.avatarUrl).toBe('')
    expect(store.user?.faithLevel).toBe(0)
  })
})
