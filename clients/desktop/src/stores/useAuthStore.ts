import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserProfile {
  id: string
  username: string
  displayName: string
  avatarUrl: string | null
  subscriptionTier: string
}

export const useAuthStore = defineStore('auth', () => {
  const isAuthenticated = ref(false)
  const accessToken = ref<string | null>(null)
  const userProfile = ref<UserProfile | null>(null)

  const isLoggedIn = computed(() => isAuthenticated.value)

  function setAuth(token: string, profile: UserProfile) {
    accessToken.value = token
    userProfile.value = profile
    isAuthenticated.value = true
  }

  function logout() {
    accessToken.value = null
    userProfile.value = null
    isAuthenticated.value = false
  }

  return {
    isAuthenticated,
    accessToken,
    userProfile,
    isLoggedIn,
    setAuth,
    logout,
  }
})
