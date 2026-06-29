<script setup lang="ts">
/**
 * 3D 渲染视口组件
 * 通过 <canvas> 元素托管 OpenGL/Vulkan 渲染上下文，
 * 实际渲染由 Core SDK 引擎驱动，通过 Tauri IPC 控制。
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRendererStore } from '@/stores/useRendererStore'

const props = defineProps<{
  width?: number
  height?: number
}>()

const emit = defineEmits<{
  (e: 'ready'): void
  (e: 'fps-update', fps: number): void
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const rendererStore = useRendererStore()
let animationFrameId: number | null = null
let lastFpsUpdate = 0

onMounted(async () => {
  const width = props.width || window.innerWidth
  const height = props.height || window.innerHeight

  // 初始化 Core SDK 渲染器（通过 Tauri IPC）
  await rendererStore.initRenderer(width, height)

  emit('ready')

  // 启动渲染循环（FPS 监控）
  startRenderLoop()
})

onUnmounted(() => {
  if (animationFrameId !== null) {
    cancelAnimationFrame(animationFrameId)
  }
})

watch(() => [props.width, props.height], ([w, h]) => {
  if (w && h) {
    rendererStore.resizeRenderer(Number(w), Number(h))
  }
})

function startRenderLoop() {
  const loop = () => {
    const now = performance.now()
    if (now - lastFpsUpdate > 1000) {
      rendererStore.getFps().then(fps => {
        emit('fps-update', fps)
      })
      lastFpsUpdate = now
    }
    animationFrameId = requestAnimationFrame(loop)
  }
  animationFrameId = requestAnimationFrame(loop)
}

defineExpose({
  canvasRef,
})
</script>

<template>
  <div class="render-viewport">
    <canvas
      ref="canvasRef"
      class="render-canvas"
      :style="{
        width: (props.width || '100%') + 'px',
        height: (props.height || '100%') + 'px',
      }"
    />
    <div v-if="!rendererStore.initialized" class="render-placeholder">
      <span>3D 渲染引擎初始化中...</span>
    </div>
    <div class="render-overlay">
      <span class="fps-badge">FPS: {{ rendererStore.fps }}</span>
      <span class="gpu-badge">{{ rendererStore.gpuBackend }}</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.render-viewport {
  position: relative;
  width: 100%;
  height: 100%;
  background: #0a0a0a;

  .render-canvas {
    display: block;
    width: 100%;
    height: 100%;
  }

  .render-placeholder {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: #666;
    font-size: 14px;
  }

  .render-overlay {
    position: absolute;
    top: 8px;
    right: 8px;
    display: flex;
    gap: 8px;

    .fps-badge,
    .gpu-badge {
      background: rgba(0, 0, 0, 0.6);
      color: #0f0;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      font-family: monospace;
    }
  }
}
</style>
