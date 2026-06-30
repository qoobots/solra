import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface LeaderboardEntry {
  rank: number
  user: {
    userId: string
    displayName: string
    avatarUrl: string
  }
  score: number
  faithLevel?: number
}

export type LeaderboardType = 'FAITH_LEVEL' | 'SPACE_COUNT' | 'SOCIAL_SCORE'
export type LeaderboardPeriod = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ALL_TIME'

export const useLeaderboardStore = defineStore('leaderboard', () => {
  const entries = ref<Record<LeaderboardType, LeaderboardEntry[]>>({
    FAITH_LEVEL: [],
    SPACE_COUNT: [],
    SOCIAL_SCORE: [],
  })
  const boardType = ref<LeaderboardType>('FAITH_LEVEL')
  const period = ref<LeaderboardPeriod>('ALL_TIME')
  const loading = ref(false)
  const error = ref<string | null>(null)

  const currentEntries = computed(() => entries.value[boardType.value] || [])

  async function fetchLeaderboard(type?: LeaderboardType): Promise<void> {
    if (type) boardType.value = type
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/api/grw/v1/leaderboard', {
        params: {
          type: boardType.value,
          period: period.value,
          pageSize: 20,
        },
      }) as any
      const data = (res.entries || res.data || []).map((e: any, i: number) => ({
        rank: e.rank || i + 1,
        user: {
          userId: e.user?.userId || e.userId || '',
          displayName: e.user?.displayName || e.displayName || '',
          avatarUrl: e.user?.avatarUrl || e.avatarUrl || '',
        },
        score: e.score || 0,
        faithLevel: e.faithLevel,
      }))
      entries.value[boardType.value] = data
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '加载排行榜失败'
    } finally {
      loading.value = false
    }
  }

  return {
    entries,
    boardType,
    period,
    loading,
    error,
    currentEntries,
    fetchLeaderboard,
  }
})
