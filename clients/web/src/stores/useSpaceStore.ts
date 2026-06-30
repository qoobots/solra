import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface SpaceItem {
  spaceId: string
  title: string
  description: string
  thumbnailUrl: string
  tags: string[]
  category: string
  visibility: 'PUBLIC' | 'FRIENDS_ONLY' | 'PRIVATE'
  creator: {
    userId: string
    displayName: string
    avatarUrl: string
  } | null
  onlineCount: number
  totalVisits: number
  faithScore: number
  createdAt: string
  updatedAt: string
}

export interface SpaceDetail extends SpaceItem {
  sceneGraph: any
  settings: Record<string, any>
  participants: Array<{
    userId: string
    displayName: string
    avatarUrl: string
    isOnline: boolean
  }>
}

export const useSpaceStore = defineStore('space', () => {
  const spaces = ref<SpaceItem[]>([])
  const currentSpace = ref<SpaceDetail | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const page = ref(0)
  const hasMore = ref(true)
  const totalSpaces = ref(0)

  const sortedSpaces = computed(() =>
    [...spaces.value].sort((a, b) => b.onlineCount - a.onlineCount)
  )

  async function fetchSpaces(reset = false): Promise<void> {
    if (reset) {
      page.value = 0
      hasMore.value = true
    }
    if (!hasMore.value && !reset) return
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/api/spc/v1/spaces', {
        params: { page: page.value, pageSize: 20 },
      }) as any
      const newSpaces = (res.spaces || res.data || []).map(mapSpace)
      if (reset) {
        spaces.value = newSpaces
      } else {
        spaces.value = [...spaces.value, ...newSpaces]
      }
      totalSpaces.value = res.totalCount || spaces.value.length
      hasMore.value = newSpaces.length >= 20
      page.value++
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '加载空间失败'
    } finally {
      loading.value = false
    }
  }

  async function fetchSpaceDetail(spaceId: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const res = await api.get(`/api/spc/v1/spaces/${spaceId}`) as any
      currentSpace.value = {
        ...mapSpace(res),
        sceneGraph: res.sceneGraph || {},
        settings: res.settings || {},
        participants: (res.participants || []).map((p: any) => ({
          userId: p.userId,
          displayName: p.displayName,
          avatarUrl: p.avatarUrl || '',
          isOnline: p.isOnline ?? true,
        })),
      }
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '加载空间详情失败'
    } finally {
      loading.value = false
    }
  }

  async function searchSpaces(query: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/api/spc/v1/spaces/search', {
        params: { query, pageSize: 20 },
      }) as any
      spaces.value = (res.spaces || res.data || []).map(mapSpace)
      totalSpaces.value = res.totalCount || spaces.value.length
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '搜索失败'
    } finally {
      loading.value = false
    }
  }

  async function createSpace(params: {
    title: string
    description: string
    tags: string[]
    visibility: string
    category?: string
  }): Promise<SpaceItem | null> {
    loading.value = true
    error.value = null
    try {
      const res = await api.post('/api/crt/v1/spaces', params) as any
      const space = mapSpace(res)
      spaces.value.unshift(space)
      return space
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '创建空间失败'
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchRecommendations(): Promise<SpaceItem[]> {
    try {
      const res = await api.get('/api/spc/v1/recommendations', {
        params: { pageSize: 12 },
      }) as any
      const items = (res.spaces || res.data || []).map(mapSpace)
      return items
    } catch {
      return []
    }
  }

  function mapSpace(raw: any): SpaceItem {
    return {
      spaceId: raw.spaceId || raw.id || '',
      title: raw.title || '',
      description: raw.description || '',
      thumbnailUrl: raw.thumbnailUrl || raw.previewUrl || '',
      tags: raw.tags || [],
      category: raw.category || 'OTHER',
      visibility: raw.visibility || 'PUBLIC',
      creator: raw.creator
        ? {
            userId: raw.creator.userId || raw.creator.id || '',
            displayName: raw.creator.displayName || '',
            avatarUrl: raw.creator.avatarUrl || '',
          }
        : null,
      onlineCount: raw.onlineCount ?? 0,
      totalVisits: raw.totalVisits ?? 0,
      faithScore: raw.faithScore ?? 0,
      createdAt: raw.createdAt || '',
      updatedAt: raw.updatedAt || '',
    }
  }

  return {
    spaces,
    currentSpace,
    loading,
    error,
    page,
    hasMore,
    totalSpaces,
    sortedSpaces,
    fetchSpaces,
    fetchSpaceDetail,
    searchSpaces,
    createSpace,
    fetchRecommendations,
  }
})
