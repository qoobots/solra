<script setup lang="ts">
/**
 * 3D 渲染视口组件 — 重构版
 *
 * 渲染策略（双通道降级）：
 *   1. 优先尝试通过 Tauri IPC 使用 Core SDK 原生渲染（OpenGL/Vulkan/DirectX）
 *   2. Core SDK 不可用时 → 使用 Three.js WebGL 作为备选渲染后端
 *
 * 架构：
 *   - SceneManager — 场景对象/光照/装饰管理
 *   - OrbitController — 轨道相机控制
 *   - 暴露方法供父组件（SpaceDetailView）调用
 */
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as THREE from 'three'
import { useRendererStore } from '@/stores/useRendererStore'
import { SceneManager, type SceneObjectDescriptor, type LightingConfig } from './SceneManager'
import { OrbitController } from './OrbitController'

const props = defineProps<{
  width?: number
  height?: number
}>()

const emit = defineEmits<{
  (e: 'ready'): void
  (e: 'fps-update', fps: number): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const rendererStore = useRendererStore()

// Three.js 核心对象
let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let animationFrameId: number | null = null

// 场景管理器 & 相机控制器
let sceneManager: SceneManager | null = null
let orbitController: OrbitController | null = null

// FPS 统计
let frameCount = 0
let lastFpsTime = performance.now()
const currentFps = ref(60)

// 使用 Core SDK 标志
const useCoreSdk = ref(false)
// 渲染就绪标志
const isReady = ref(false)

onMounted(async () => {
  const width = props.width || window.innerWidth
  const height = props.height || window.innerHeight

  // 尝试初始化 Core SDK
  useCoreSdk.value = await tryInitCoreSdk(width, height)

  // 初始化 Three.js
  await nextTick()
  initThreeJS(width, height)

  // 创建场景管理器
  sceneManager = new SceneManager()
  if (scene) {
    sceneManager.setupScene(scene)
  }

  // 创建轨道控制器
  if (camera && containerRef.value) {
    orbitController = new OrbitController(camera, containerRef.value)
    orbitController.speed = 1.0
  }

  rendererStore.initialized = true
  isReady.value = true
  emit('ready')

  startRenderLoop()
})

onUnmounted(() => {
  stopRenderLoop()
  disposeAll()
})

watch(() => [props.width, props.height], ([w, h]) => {
  if (w && h && !useCoreSdk.value) {
    resizeThreeJS(Number(w), Number(h))
  }
})

// ========== Core SDK 初始化 ==========
async function tryInitCoreSdk(width: number, height: number): Promise<boolean> {
  try {
    const { invoke } = await import('@tauri-apps/api/core')
    const state = await invoke<{
      initialized: boolean
      fps: number
      gpu_backend: string
      core_sdk_loaded?: boolean
    }>('init_renderer', { width, height })

    if (state.core_sdk_loaded === true) {
      rendererStore.fps = state.fps
      rendererStore.gpuBackend = state.gpu_backend
      console.log('[RenderViewport] Core SDK 渲染引擎就绪:', state.gpu_backend)
      return true
    }
    console.log('[RenderViewport] Core SDK 未加载，使用 Three.js 备选渲染')
  } catch {
    console.log('[RenderViewport] Tauri IPC 不可用，使用 Three.js 渲染')
  }
  return false
}

// ========== Three.js 初始化 ==========
function initThreeJS(width: number, height: number) {
  const container = containerRef.value
  if (!container) return

  // 渲染器
  renderer = new THREE.WebGLRenderer({
    canvas: canvasRef.value!,
    antialias: true,
    alpha: false,
  })
  renderer.setSize(width, height, false)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.2

  // 场景
  scene = new THREE.Scene()

  // 相机
  camera = new THREE.PerspectiveCamera(55, width / height, 0.5, 200)
  camera.position.set(8, 5, 12)
  camera.lookAt(0, 1.5, 0)

  // GPU 信息
  if (!rendererStore.gpuBackend || rendererStore.gpuBackend === 'Three.js WebGL') {
    rendererStore.gpuBackend = detectGPUInfo()
  }
}

function detectGPUInfo(): string {
  const gl = renderer?.getContext()
  if (!gl) return 'Three.js WebGL'
  const debugInfo = gl.getExtension('WEBGL_debug_renderer_info')
  if (debugInfo) {
    const gpu = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL)
    return `WebGL · ${gpu.split('/').pop()?.trim() ?? gpu}`
  }
  return 'Three.js WebGL'
}

// ========== 渲染循环 ==========
function startRenderLoop() {
  if (useCoreSdk.value) {
    startCoreSdkLoop()
  } else {
    startThreeJSLoop()
  }
}

function startThreeJSLoop() {
  const clock = new THREE.Clock()

  const loop = () => {
    animationFrameId = requestAnimationFrame(loop)
    const delta = Math.min(clock.getDelta(), 0.1) // 防止大帧跳跃

    // 更新轨道控制器
    orbitController?.update()

    // 更新粒子
    sceneManager?.updateParticles(delta)

    // 渲染
    renderFrame()
    updateFps()
  }
  animationFrameId = requestAnimationFrame(loop)
}

function startCoreSdkLoop() {
  const loop = async () => {
    animationFrameId = requestAnimationFrame(loop)
    try {
      const { invoke } = await import('@tauri-apps/api/core')
      const fps = await invoke<number>('get_fps')
      currentFps.value = fps
      rendererStore.fps = fps
      emit('fps-update', fps)
    } catch { /* 降级 */ }

    orbitController?.update()
    sceneManager?.updateParticles(0.016)
    renderFrame()
  }
  animationFrameId = requestAnimationFrame(loop)
}

function renderFrame() {
  if (renderer && scene && camera) {
    renderer.render(scene, camera)
  }
}

function updateFps() {
  frameCount++
  const now = performance.now()
  if (now - lastFpsTime >= 500) {
    const fps = Math.round(frameCount / ((now - lastFpsTime) / 1000))
    currentFps.value = fps
    rendererStore.fps = fps
    emit('fps-update', fps)
    frameCount = 0
    lastFpsTime = now
  }
}

function resizeThreeJS(width: number, height: number) {
  if (renderer) renderer.setSize(width, height, false)
  if (camera) {
    camera.aspect = width / height
    camera.updateProjectionMatrix()
  }
}

function stopRenderLoop() {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
}

function disposeAll() {
  stopRenderLoop()
  orbitController?.dispose()
  orbitController = null
  sceneManager?.dispose(scene!)
  sceneManager = null

  if (renderer) {
    renderer.dispose()
    renderer = null
  }
  if (scene) {
    scene.clear()
    scene = null
  }
  camera = null
}

// ========== 对外暴露 API（供父组件调用） ==========

/** 向场景中添加对象 */
function addObject(desc: SceneObjectDescriptor): THREE.Object3D | null {
  if (!sceneManager || !scene) return null
  return sceneManager.addObject(scene, desc)
}

/** 批量添加对象 */
function addObjects(descs: SceneObjectDescriptor[]): THREE.Object3D[] {
  if (!sceneManager || !scene) return []
  return sceneManager.addObjects(scene, descs)
}

/** 移除对象 */
function removeObject(id: string): boolean {
  if (!sceneManager || !scene) return false
  return sceneManager.removeObject(scene, id)
}

/** 获取对象 */
function getObject(id: string): THREE.Object3D | undefined {
  return sceneManager?.getObject(id)
}

/** 更新对象变换 */
function updateObjectTransform(id: string, position?: [number, number, number], rotation?: [number, number, number], scale?: [number, number, number]): boolean {
  if (!sceneManager) return false
  return sceneManager.updateTransform(id, position, rotation, scale)
}

/** 清除所有场景对象 */
function clearObjects(): void {
  if (!sceneManager || !scene) return
  sceneManager.clearObjects(scene)
}

/** 更新光照 */
function updateLighting(config: Partial<LightingConfig>): void {
  sceneManager?.updateLighting(config)
}

/** 获取光照配置 */
function getLightingConfig(): LightingConfig | null {
  return sceneManager?.getLightingConfig() ?? null
}

/** 设置网格可见性 */
function setGridVisible(visible: boolean): void {
  if (!sceneManager || !scene) return
  sceneManager.setGridVisible(scene, visible)
}

/** 设置粒子可见性 */
function setParticlesVisible(visible: boolean): void {
  if (!sceneManager || !scene) return
  sceneManager.setParticlesVisible(scene, visible)
}

/** 设置线框模式 */
function setWireframeMode(enabled: boolean): void {
  if (!sceneManager || !scene) return
  sceneManager.setWireframeMode(scene, enabled)
}

/** 设置相机速度 */
function setCameraSpeed(speed: number): void {
  if (orbitController) orbitController.speed = speed
}

/** 聚焦到某对象 */
function focusOnObject(id: string, distance?: number): void {
  const obj = sceneManager?.getObject(id)
  if (obj && orbitController) {
    orbitController.focusOn(obj, distance)
  }
}

/** 重置相机 */
function resetCamera(position?: [number, number, number], target?: [number, number, number]): void {
  orbitController?.reset(position, target)
}

/** 设置相机目标点 */
function setCameraTarget(x: number, y: number, z: number): void {
  orbitController?.setTarget(x, y, z)
}

defineExpose({
  // 核心引用
  canvasRef,
  containerRef,
  scene,
  camera,
  // 状态
  isReady,
  currentFps,
  // 对象管理
  addObject,
  addObjects,
  removeObject,
  getObject,
  updateObjectTransform,
  clearObjects,
  // 场景控制
  updateLighting,
  getLightingConfig,
  setGridVisible,
  setParticlesVisible,
  setWireframeMode,
  setCameraSpeed,
  // 相机控制
  focusOnObject,
  resetCamera,
  setCameraTarget,
})
</script>

<template>
  <div ref="containerRef" class="render-viewport">
    <canvas ref="canvasRef" class="render-canvas" />
    <!-- 初始化占位 -->
    <div v-if="!isReady" class="render-placeholder">
      <div class="placeholder-content">
        <div class="spinner"></div>
        <span>3D 渲染引擎初始化中...</span>
      </div>
    </div>
    <!-- HUD 叠加层 -->
    <div v-if="isReady" class="render-overlay">
      <span class="fps-badge">{{ currentFps }} FPS</span>
      <span class="gpu-badge">{{ rendererStore.gpuBackend }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.render-viewport {
  position: relative;
  width: 100%;
  height: 100%;
  background: radial-gradient(ellipse at center, #1a1a3e 0%, #0a0a1a 60%, #050510 100%);

  .render-canvas {
    display: block;
    width: 100%;
    height: 100%;
  }

  .render-placeholder {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(5, 5, 16, 0.85);

    .placeholder-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      color: #6e7681;
      font-size: 14px;
    }

    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid rgba(74, 108, 247, 0.2);
      border-top-color: #4a6cf7;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .render-overlay {
    position: absolute;
    top: 8px;
    right: 8px;
    display: flex;
    gap: 8px;
    z-index: 5;

    .fps-badge,
    .gpu-badge {
      background: rgba(0, 0, 0, 0.65);
      color: #0f0;
      padding: 3px 10px;
      border-radius: 4px;
      font-size: 12px;
      font-family: monospace;
      letter-spacing: 0.5px;
    }

    .gpu-badge {
      color: #58a6ff;
    }
  }
}
</style>
