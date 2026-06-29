import { defineStore } from 'pinia'
import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'

export const useRendererStore = defineStore('renderer', () => {
  const initialized = ref(false)
  const fps = ref(60)
  const gpuBackend = ref('')

  async function initRenderer(width: number, height: number) {
    try {
      const state = await invoke<{ initialized: boolean; fps: number; gpu_backend: string }>(
        'init_renderer',
        { width, height }
      )
      initialized.value = state.initialized
      fps.value = state.fps
      gpuBackend.value = state.gpu_backend
    } catch (e) {
      console.error('初始化渲染器失败:', e)
    }
  }

  async function resizeRenderer(width: number, height: number) {
    try {
      await invoke('resize_renderer', { width, height })
    } catch (e) {
      console.error('调整渲染器尺寸失败:', e)
    }
  }

  async function getFps(): Promise<number> {
    try {
      const currentFps = await invoke<number>('get_fps')
      fps.value = currentFps
      return currentFps
    } catch {
      return fps.value
    }
  }

  return {
    initialized,
    fps,
    gpuBackend,
    initRenderer,
    resizeRenderer,
    getFps,
  }
})
