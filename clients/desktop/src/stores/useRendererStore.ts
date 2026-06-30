import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 渲染器状态管理
 *
 * 支持两种渲染模式：
 *   - Core SDK 原生渲染（OpenGL/Vulkan/DirectX），通过 Tauri IPC 驱动
 *   - Three.js WebGL 备选渲染，纯前端实现
 *
 * 组件初始化时按优先级自动选择渲染后端。
 */
export const useRendererStore = defineStore('renderer', () => {
  const initialized = ref(false)
  const fps = ref(60)
  const gpuBackend = ref('Three.js WebGL')

  async function initRenderer(width: number, height: number) {
    try {
      const { invoke } = await import('@tauri-apps/api/core')
      const state = await invoke<{ initialized: boolean; fps: number; gpu_backend: string }>(
        'init_renderer',
        { width, height }
      )
      if (state.initialized) {
        initialized.value = true
        fps.value = state.fps
        gpuBackend.value = state.gpu_backend
        return
      }
    } catch {
      // Core SDK 不可用 — RenderViewport 将使用 Three.js
    }
    // 标记为已初始化（由 RenderViewport 完成 Three.js 初始化）
  }

  async function resizeRenderer(width: number, height: number) {
    try {
      const { invoke } = await import('@tauri-apps/api/core')
      await invoke('resize_renderer', { width, height })
    } catch {
      // 非 Tauri 环境，由 RenderViewport 自行处理 resize
    }
  }

  async function getFps(): Promise<number> {
    try {
      const { invoke } = await import('@tauri-apps/api/core')
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
