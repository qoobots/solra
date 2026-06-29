import { defineStore } from 'pinia'
import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'

export interface SpaceSummary {
  id: string
  name: string
  description: string
  thumbnail_url: string
  author_name: string
  visitor_count: number
  like_count: number
  category: string
  tags: string[]
}

export const useSpaceStore = defineStore('space', () => {
  const spaces = ref<SpaceSummary[]>([])
  const loading = ref(false)
  const currentSpace = ref<SpaceSummary | null>(null)

  async function fetchSpaces(page = 1, pageSize = 20) {
    loading.value = true
    try {
      const result = await invoke<SpaceSummary[]>('get_spaces', { page, pageSize })
      spaces.value = result
    } catch (e) {
      console.error('获取空间列表失败:', e)
      spaces.value = []
    } finally {
      loading.value = false
    }
  }

  async function enterSpace(spaceId: string) {
    try {
      await invoke('enter_space', { spaceId })
      const space = spaces.value.find(s => s.id === spaceId)
      if (space) {
        currentSpace.value = space
      }
    } catch (e) {
      console.error('进入空间失败:', e)
    }
  }

  async function exitSpace(spaceId: string) {
    try {
      await invoke('exit_space', { spaceId })
      currentSpace.value = null
    } catch (e) {
      console.error('退出空间失败:', e)
    }
  }

  return {
    spaces,
    loading,
    currentSpace,
    fetchSpaces,
    enterSpace,
    exitSpace,
  }
})
