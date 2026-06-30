import { defineStore } from 'pinia'
import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import api from '@/api'

export interface SpaceSummary {
  spaceId: string
  title: string
  description: string
  thumbnailUrl: string
  creatorName: string
  onlineCount: number
  likeCount: number
  category: string
  tags: string[]
}

export interface SpaceDetail {
  spaceId: string
  title: string
  description: string
  thumbnailUrl: string
  creator: {
    userId: string
    displayName: string
    avatarUrl: string
  } | null
  onlineCount: number
  totalVisits: number
  faithScore: number
  category: string
  tags: string[]
  sceneGraph: any
  settings: Record<string, any>
  participants: Array<{
    userId: string
    displayName: string
    avatarUrl: string
    isOnline: boolean
  }>
}

export const useSpaceStore = defineStore('space-desktop', () => {
  const spaces = ref<SpaceSummary[]>([])
  const currentSpace = ref<SpaceDetail | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 通过 HTTP API 获取空间列表
  async function fetchSpaces(_reset = false): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/api/spc/v1/spaces', {
        params: { page: 0, pageSize: 20 },
      }) as any
      const data = (res.spaces || res.data || []).map(mapSummary)
      spaces.value = data
    } catch {
      // HTTP API 不可用时回退到 Tauri IPC
      try {
        const data = await invoke<any[]>('get_spaces', { page: 1, pageSize: 20 })
        spaces.value = data.map((s: any) => ({
          spaceId: s.id || s.spaceId || '',
          title: s.name || s.title || '',
          description: s.description || '',
          thumbnailUrl: s.thumbnail_url || s.thumbnailUrl || '',
          creatorName: s.author_name || s.creator?.displayName || '',
          onlineCount: s.visitor_count || s.onlineCount || 0,
          likeCount: s.like_count || s.likeCount || 0,
          category: s.category || '',
          tags: s.tags || [],
        }))
      } catch (e2: any) {
        error.value = '加载空间列表失败'
        console.error('fetchSpaces error:', e2)
      }
    } finally {
      loading.value = false
    }
  }

  // 进入空间：通过 Tauri IPC 调用 Core SDK 流式加载
  async function enterSpace(spaceId: string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      // 先通过 HTTP API 获取详情
      const res = await api.get(`/api/spc/v1/spaces/${spaceId}`) as any
      currentSpace.value = mapDetail(res)

      // 通过 Tauri IPC 触发 Core SDK 加载
      try {
        await invoke('enter_space', { spaceId })
      } catch {
        console.warn('Core SDK enter_space 调用失败（开发模式可忽略）')
      }
    } catch {
      // HTTP API 不可用时回退到 Tauri IPC
      try {
        await invoke('enter_space', { spaceId })
        const detail = await invoke<any>('get_space_detail', { spaceId })
        currentSpace.value = {
          spaceId: detail.id || spaceId,
          title: detail.name || '',
          description: detail.description || '',
          thumbnailUrl: '',
          creator: detail.author_name ? {
            userId: detail.author_id || '',
            displayName: detail.author_name,
            avatarUrl: '',
          } : null,
          onlineCount: detail.visitor_count || 0,
          totalVisits: detail.visitor_count || 0,
          faithScore: 0,
          category: detail.category || '',
          tags: detail.tags || [],
          sceneGraph: {},
          settings: {},
          participants: [],
        }
      } catch (e2: any) {
        error.value = '进入空间失败'
        console.error('enterSpace error:', e2)
      }
    } finally {
      loading.value = false
    }
  }

  // 退出空间
  async function exitSpace(spaceId: string): Promise<void> {
    try {
      await invoke('exit_space', { spaceId })
    } catch {
      console.warn('Core SDK exit_space 调用失败（开发模式可忽略）')
    }
    currentSpace.value = null
  }

  function mapSummary(raw: any): SpaceSummary {
    return {
      spaceId: raw.spaceId || raw.id || '',
      title: raw.title || raw.name || '',
      description: raw.description || '',
      thumbnailUrl: raw.thumbnailUrl || raw.previewUrl || '',
      creatorName: raw.creator?.displayName || '',
      onlineCount: raw.onlineCount || 0,
      likeCount: 0,
      category: raw.category || '',
      tags: raw.tags || [],
    }
  }

  function mapDetail(raw: any): SpaceDetail {
    return {
      spaceId: raw.spaceId || raw.id || '',
      title: raw.title || raw.name || '',
      description: raw.description || '',
      thumbnailUrl: raw.thumbnailUrl || raw.previewUrl || '',
      creator: raw.creator ? {
        userId: raw.creator.userId || raw.creator.id || '',
        displayName: raw.creator.displayName || '',
        avatarUrl: raw.creator.avatarUrl || '',
      } : null,
      onlineCount: raw.onlineCount ?? 0,
      totalVisits: raw.totalVisits ?? 0,
      faithScore: raw.faithScore ?? 0,
      category: raw.category || '',
      tags: raw.tags || [],
      sceneGraph: raw.sceneGraph || {},
      settings: raw.settings || {},
      participants: (raw.participants || []).map((p: any) => ({
        userId: p.userId || '',
        displayName: p.displayName || '',
        avatarUrl: p.avatarUrl || '',
        isOnline: p.isOnline ?? true,
      })),
    }
  }

  return {
    spaces,
    currentSpace,
    loading,
    error,
    fetchSpaces,
    enterSpace,
    exitSpace,
  }
})
