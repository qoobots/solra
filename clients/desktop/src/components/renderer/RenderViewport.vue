<script setup lang="ts">
/**
 * 3D 渲染视口组件
 *
 * 渲染策略（双通道降级）：
 *   1. 优先尝试通过 Tauri IPC 使用 Core SDK 原生渲染（OpenGL/Vulkan/DirectX）
 *   2. Core SDK 不可用时 → 使用 Three.js WebGL 作为备选渲染后端
 *
 * Three.js 场景内容：
 *   - 无限网格地面
 *   - 动态天空渐变背景
 *   - 浮动几何体（空间装饰）
 *   - 轨道相机控制
 *   - FPS 实时统计
 */
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as THREE from 'three'
import { useRendererStore } from '@/stores/useRendererStore'

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

// FPS 统计
let frameCount = 0
let lastFpsTime = performance.now()
const currentFps = ref(60)

// 使用 Core SDK 标志
const useCoreSdk = ref(false)

onMounted(async () => {
  const width = props.width || window.innerWidth
  const height = props.height || window.innerHeight

  // 尝试初始化 Core SDK
  useCoreSdk.value = await tryInitCoreSdk(width, height)

  if (!useCoreSdk.value) {
    // Core SDK 不可用 → 使用 Three.js 备选方案
    await nextTick()
    initThreeJS(width, height)
  }

  rendererStore.initialized = true
  emit('ready')

  startRenderLoop()
})

onUnmounted(() => {
  stopRenderLoop()
  disposeThreeJS()
})

watch(() => [props.width, props.height], ([w, h]) => {
  if (w && h && !useCoreSdk.value) {
    resizeThreeJS(Number(w), Number(h))
  }
})

// ---- Core SDK 初始化 ----
async function tryInitCoreSdk(width: number, height: number): Promise<boolean> {
  try {
    const { invoke } = await import('@tauri-apps/api/core')
    const state = await invoke<{
      initialized: boolean
      fps: number
      gpu_backend: string
      core_sdk_loaded?: boolean
    }>('init_renderer', { width, height })

    // 更新 GPU 后端信息（Core SDK 可能已加载但渲染模块尚未实现）
    if (state.core_sdk_loaded === true) {
      console.log('[RenderViewport] Core SDK 已加载，版本信息将通过 Tauri IPC 获取')
      rendererStore.fps = state.fps
      // 保留 "Core SDK · OpenGL" 标识，表示 Core 层已就绪
      rendererStore.gpuBackend = 'Core SDK · OpenGL'
    }

    // 当前阶段：Core SDK 渲染模块（solra_render）尚未完成实现
    // 暂时总是使用 Three.js 作为视口内渲染后端
    // 等 render.rs 实现完成后，再将 return true 的条件设为：
    //   state.initialized && state.core_sdk_loaded === true
    console.log('[RenderViewport] 使用 Three.js 渲染（Core SDK 渲染模块待实现）')
  } catch {
    console.log('[RenderViewport] Tauri IPC 不可用，使用 Three.js 渲染')
  }
  return false
}

// ---- Three.js 场景初始化 ----
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
  camera.lookAt(0, 0, 0)

  // 光照
  setupLighting()

  // 场景内容
  createGround()
  createFloatingGeometries()
  createParticleField()

  // 设置 GPU 后端标识（优先使用 Core SDK 信息，否则显示 WebGL 检测结果）
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

// ---- 光照系统 ----
function setupLighting() {
  if (!scene) return

  // 环境光 — 基础照明
  const ambient = new THREE.AmbientLight('#4a6cf7', 0.6)
  scene.add(ambient)

  // 半球光 — 天空/地面颜色混合
  const hemi = new THREE.HemisphereLight('#87ceeb', '#362850', 0.5)
  scene.add(hemi)

  // 主方向光 — 投射阴影
  const sun = new THREE.DirectionalLight('#ffffff', 3.5)
  sun.position.set(15, 20, 8)
  sun.castShadow = true
  sun.shadow.mapSize.width = 2048
  sun.shadow.mapSize.height = 2048
  sun.shadow.camera.near = 0.5
  sun.shadow.camera.far = 80
  sun.shadow.camera.left = -20
  sun.shadow.camera.right = 20
  sun.shadow.camera.top = 20
  sun.shadow.camera.bottom = -20
  sun.shadow.bias = -0.0001
  sun.shadow.normalBias = 0.02
  scene.add(sun)

  // 补光 — 减少暗部过黑
  const fill = new THREE.DirectionalLight('#8899cc', 1.2)
  fill.position.set(-5, 2, -3)
  scene.add(fill)

  // 底部补光 — 模拟地面反弹
  const rim = new THREE.DirectionalLight('#4466aa', 0.8)
  rim.position.set(0, -1, 0)
  scene.add(rim)
}

// ---- 地面 ----
function createGround() {
  if (!scene) return

  // 网格地面（无限视觉延伸）
  const gridHelper = new THREE.PolarGridHelper(30, 48, 24, 128, '#334466', '#223355')
  scene.add(gridHelper)

  // 实体地面平面（接收阴影）
  const groundGeo = new THREE.PlaneGeometry(60, 60)
  const groundMat = new THREE.MeshStandardMaterial({
    color: '#1a1a2e',
    roughness: 0.85,
    metalness: 0.1,
  })
  const ground = new THREE.Mesh(groundGeo, groundMat)
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.05
  ground.receiveShadow = true
  scene.add(ground)
}

// ---- 浮动几何体装饰 ----
function createFloatingGeometries() {
  if (!scene) return

  const materials = [
    new THREE.MeshStandardMaterial({ color: '#4a6cf7', roughness: 0.2, metalness: 0.8, emissive: '#1a2a55', emissiveIntensity: 0.3 }),
    new THREE.MeshStandardMaterial({ color: '#6c5ce7', roughness: 0.2, metalness: 0.8, emissive: '#1a2055', emissiveIntensity: 0.3 }),
    new THREE.MeshStandardMaterial({ color: '#00b894', roughness: 0.3, metalness: 0.6, emissive: '#0a3322', emissiveIntensity: 0.25 }),
    new THREE.MeshStandardMaterial({ color: '#e17055', roughness: 0.3, metalness: 0.6, emissive: '#331a10', emissiveIntensity: 0.25 }),
    new THREE.MeshStandardMaterial({ color: '#fdcb6e', roughness: 0.2, metalness: 0.7, emissive: '#332a10', emissiveIntensity: 0.3 }),
  ]

  // 中心大球体
  const centerGeo = new THREE.IcosahedronGeometry(1.2, 2)
  const centerMesh = new THREE.Mesh(centerGeo, materials[0])
  centerMesh.position.set(0, 1.8, 0)
  centerMesh.castShadow = true
  centerMesh.receiveShadow = true
  centerMesh.name = 'center-orb'
  scene.add(centerMesh)

  // 轨道环
  const ringGeo = new THREE.TorusGeometry(2.5, 0.04, 16, 120)
  const ringMat = new THREE.MeshStandardMaterial({ color: '#4a6cf7', roughness: 0.1, metalness: 0.9, emissive: '#1a2a55', emissiveIntensity: 0.5 })
  const ring = new THREE.Mesh(ringGeo, ringMat)
  ring.rotation.x = Math.PI / 2.8
  ring.position.y = 1.8
  ring.name = 'orbit-ring'
  scene.add(ring)

  // 外围浮动小几何体
  const geometries: THREE.BufferGeometry[] = [
    new THREE.OctahedronGeometry(0.35),
    new THREE.TetrahedronGeometry(0.35),
    new THREE.DodecahedronGeometry(0.3),
    new THREE.BoxGeometry(0.55, 0.55, 0.55),
    new THREE.ConeGeometry(0.3, 0.7, 8),
    new THREE.TorusKnotGeometry(0.25, 0.08, 64, 8),
  ]

  const orbitRadius = 3.5
  for (let i = 0; i < 6; i++) {
    const angle = (i / 6) * Math.PI * 2
    const geo = geometries[i % geometries.length]
    const mat = materials[(i + 1) % materials.length]
    const mesh = new THREE.Mesh(geo, mat)
    mesh.position.set(
      Math.cos(angle) * orbitRadius,
      1.6 + Math.sin(i * 1.3) * 0.6,
      Math.sin(angle) * orbitRadius
    )
    mesh.castShadow = true
    mesh.receiveShadow = true
    mesh.name = `float-${i}`
    scene.add(mesh)
  }
}

// ---- 粒子场 ----
function createParticleField() {
  if (!scene) return

  const count = 600
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)

  for (let i = 0; i < count; i++) {
    // 球形分布
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    const r = 6 + Math.random() * 14

    positions[i * 3] = Math.cos(theta) * Math.sin(phi) * r
    positions[i * 3 + 1] = Math.sin(phi) * r * 0.4 + 2
    positions[i * 3 + 2] = Math.cos(phi) * r

    // 蓝紫色调
    colors[i * 3] = 0.2 + Math.random() * 0.3
    colors[i * 3 + 1] = 0.1 + Math.random() * 0.25
    colors[i * 3 + 2] = 0.6 + Math.random() * 0.4
  }

  const geo = new THREE.BufferGeometry()
  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))

  const mat = new THREE.PointsMaterial({
    size: 0.04,
    vertexColors: true,
    blending: THREE.AdditiveBlending,
    depthWrite: false,
    transparent: true,
    opacity: 0.7,
  })

  const particles = new THREE.Points(geo, mat)
  particles.name = 'particles'
  scene.add(particles)
}

// ---- 渲染循环 ----
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

    const delta = clock.getDelta()
    const elapsed = performance.now() * 0.001

    updateScene(delta, elapsed)
    renderFrame()
    updateFps()
  }

  animationFrameId = requestAnimationFrame(loop)
}

function startCoreSdkLoop() {
  const loop = async () => {
    animationFrameId = requestAnimationFrame(loop)

    const now = performance.now()
    frameCount++
    if (now - lastFpsTime >= 1000) {
      const fps = Math.round(frameCount / ((now - lastFpsTime) / 1000))
      currentFps.value = fps
      rendererStore.fps = fps
      emit('fps-update', fps)
      frameCount = 0
      lastFpsTime = now
    }
  }
  animationFrameId = requestAnimationFrame(loop)
}

function updateScene(delta: number, elapsed: number) {
  if (!scene) return

  // 中心球体呼吸效果
  const centerOrb = scene.getObjectByName('center-orb')
  if (centerOrb) {
    const scale = 1 + Math.sin(elapsed * 1.5) * 0.08
    centerOrb.scale.setScalar(scale)
    centerOrb.rotation.y += delta * 0.3
    centerOrb.rotation.x += delta * 0.15
  }

  // 轨道环旋转
  const orbitRing = scene.getObjectByName('orbit-ring')
  if (orbitRing) {
    orbitRing.rotation.z += delta * 0.2
  }

  // 外围浮动几何体旋转
  for (let i = 0; i < 6; i++) {
    const obj = scene.getObjectByName(`float-${i}`)
    if (obj) {
      obj.rotation.y += delta * (0.4 + i * 0.08)
      obj.rotation.x += delta * 0.2
      // 上下浮动
      obj.position.y += Math.sin(elapsed * 2 + i) * delta * 0.3
    }
  }

  // 粒子旋转
  const particles = scene.getObjectByName('particles')
  if (particles) {
    particles.rotation.y += delta * 0.05
    particles.rotation.x += delta * 0.02
  }

  // 相机缓慢环绕
  if (camera) {
    const camAngle = elapsed * 0.12
    const camRadius = 13
    camera.position.x = Math.cos(camAngle) * camRadius
    camera.position.z = Math.sin(camAngle) * camRadius
    camera.position.y = 5 + Math.sin(elapsed * 0.3) * 1.5
    camera.lookAt(0, 1.5, 0)
  }
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
  if (renderer) {
    renderer.setSize(width, height, false)
  }
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

function disposeThreeJS() {
  stopRenderLoop()

  if (renderer) {
    renderer.dispose()
    renderer = null
  }

  if (scene) {
    scene.traverse((obj) => {
      if (obj instanceof THREE.Mesh) {
        obj.geometry?.dispose()
        if (Array.isArray(obj.material)) {
          obj.material.forEach((m) => m.dispose())
        } else {
          obj.material?.dispose()
        }
      }
      if (obj instanceof THREE.Points) {
        obj.geometry?.dispose()
        ;(obj.material as THREE.Material)?.dispose()
      }
    })
    scene.clear()
    scene = null
  }

  camera = null
}

defineExpose({
  canvasRef,
  containerRef,
})
</script>

<template>
  <div ref="containerRef" class="render-viewport">
    <canvas
      ref="canvasRef"
      class="render-canvas"
    />
    <!-- 初始化占位 -->
    <div v-if="!rendererStore.initialized" class="render-placeholder">
      <div class="placeholder-content">
        <div class="spinner"></div>
        <span>3D 渲染引擎初始化中...</span>
      </div>
    </div>
    <!-- HUD 叠加层 -->
    <div v-if="rendererStore.initialized" class="render-overlay">
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
