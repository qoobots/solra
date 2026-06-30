import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Hoisted mock
const { mockInvoke } = vi.hoisted(() => {
  const mockInvoke = vi.fn()
  return { mockInvoke }
})

// Mock Tauri invoke
vi.mock('@tauri-apps/api/core', () => ({ invoke: mockInvoke }))

import { useRendererStore } from './useRendererStore'

describe('useRendererStore (Desktop)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const store = useRendererStore()
    expect(store.initialized).toBe(false)
    expect(store.fps).toBe(60)
    expect(store.gpuBackend).toBe('')
  })

  it('should init renderer successfully', async () => {
    const store = useRendererStore()

    mockInvoke.mockResolvedValueOnce({
      initialized: true,
      fps: 60,
      gpu_backend: 'Vulkan',
    })

    await store.initRenderer(1920, 1080)

    expect(store.initialized).toBe(true)
    expect(store.fps).toBe(60)
    expect(store.gpuBackend).toBe('Vulkan')
    expect(mockInvoke).toHaveBeenCalledWith('init_renderer', {
      width: 1920,
      height: 1080,
    })
  })

  it('should handle init renderer failure gracefully', async () => {
    const store = useRendererStore()

    mockInvoke.mockRejectedValueOnce(new Error('GPU not available'))

    await store.initRenderer(800, 600)

    expect(store.initialized).toBe(false)
    expect(store.fps).toBe(60)
    expect(store.gpuBackend).toBe('')
  })

  it('should resize renderer', async () => {
    const store = useRendererStore()

    mockInvoke.mockResolvedValueOnce(undefined)

    await store.resizeRenderer(1280, 720)

    expect(mockInvoke).toHaveBeenCalledWith('resize_renderer', {
      width: 1280,
      height: 720,
    })
  })

  it('should handle resize failure gracefully', async () => {
    const store = useRendererStore()

    mockInvoke.mockRejectedValueOnce(new Error('Renderer not initialized'))

    await expect(store.resizeRenderer(1024, 768)).resolves.toBeUndefined()
  })

  it('should get FPS successfully', async () => {
    const store = useRendererStore()

    mockInvoke.mockResolvedValueOnce(30)

    const fps = await store.getFps()

    expect(fps).toBe(30)
    expect(store.fps).toBe(30)
  })

  it('should return current fps when getFps IPC fails', async () => {
    const store = useRendererStore()

    store.$patch({ fps: 45 })

    mockInvoke.mockRejectedValueOnce(new Error('IPC error'))

    const fps = await store.getFps()

    expect(fps).toBe(45)
  })

  it('should support multiple GPU backend names', async () => {
    const store = useRendererStore()

    const backends = ['Metal', 'Vulkan', 'DirectX 12']

    for (const backend of backends) {
      mockInvoke.mockResolvedValueOnce({
        initialized: true,
        fps: 60,
        gpu_backend: backend,
      })
      await store.initRenderer(1920, 1080)
      expect(store.gpuBackend).toBe(backend)
    }
  })
})
