import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface UserProfile {
  userId: string
  username: string
  displayName: string
  avatarUrl: string
  bio: string
  faithLevel: number
  spaceCount: number
  email: string
  phoneNumber: string
  subscriptionTier: string
  createdAt: string
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null)
  const accessToken = ref<string | null>(localStorage.getItem('solra_token'))
  const refreshToken = ref<string | null>(localStorage.getItem('solra_refresh_token'))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const userDisplayName = computed(() => user.value?.displayName ?? '')

  async function login(username: string, password: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      const res = await api.post('/api/auth/v1/login', {
        email: username,
        password,
        loginMethod: 'PASSWORD',
      }) as any
      accessToken.value = res.accessToken
      refreshToken.value = res.refreshToken
      localStorage.setItem('solra_token', res.accessToken)
      localStorage.setItem('solra_refresh_token', res.refreshToken)
      await fetchProfile()
      return true
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '登录失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function register(params: {
    email: string
    password: string
    displayName: string
  }): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      const res = await api.post('/api/auth/v1/register', params) as any
      accessToken.value = res.accessToken
      refreshToken.value = res.refreshToken
      localStorage.setItem('solra_token', res.accessToken)
      localStorage.setItem('solra_refresh_token', res.refreshToken)
      await fetchProfile()
      return true
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '注册失败'
      return false
    } finally {
      loading.value = false
    }
  }

  async function fetchProfile(): Promise<void> {
    try {
      const res = await api.get('/api/auth/v1/profile') as any
      user.value = {
        userId: res.userId,
        username: res.username || res.displayName || '',
        displayName: res.displayName,
        avatarUrl: res.avatarUrl || '',
        bio: res.bio || '',
        faithLevel: res.faithLevel || 0,
        spaceCount: res.spaceCount || 0,
        email: res.email || '',
        phoneNumber: res.phoneNumber || '',
        subscriptionTier: res.subscriptionTier || 'Free',
        createdAt: res.createdAt || '',
      }
    } catch (e: any) {
      console.error('Failed to fetch profile:', e)
    }
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('solra_token')
    localStorage.removeItem('solra_refresh_token')
  }

  return {
    user,
    accessToken,
    refreshToken,
    loading,
    error,
    isAuthenticated,
    userDisplayName,
    login,
    register,
    fetchProfile,
    logout,
  }
})
