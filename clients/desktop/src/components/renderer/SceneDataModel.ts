/**
 * 空间场景数据模型 — SceneDataModel
 *
 * 职责：
 *   1. 定义场景图（SceneGraph）的完整数据结构
 *   2. 提供默认场景生成器（Demo/Placeholder）
 *   3. 将服务端 sceneGraph JSON 转换为 SceneObjectDescriptor[]
 *   4. 管理空间场景的序列化与反序列化
 */

import type { SceneObjectDescriptor } from './SceneManager'

// ========== 场景图数据模型 ==========

/** 场景图顶层结构 */
export interface SceneGraph {
  version: string
  objects: SceneObjectDescriptor[]
  lighting?: SceneLightingDef
  environment?: EnvironmentDef
  spawnPoints?: SpawnPoint[]
}

/** 场景光照定义 */
export interface SceneLightingDef {
  ambientColor: string
  ambientIntensity: number
  sunDirection: [number, number, number]
  sunColor: string
  sunIntensity: number
}

/** 环境定义 */
export interface EnvironmentDef {
  skyColor: string
  groundColor: string
  fogColor?: string
  fogDensity?: number
}

/** 出生点 */
export interface SpawnPoint {
  id: string
  position: [number, number, number]
  rotation?: [number, number, number]
  label?: string
}

// ========== 默认场景生成器 ==========

/**
 * 默认 Demo 场景 — 用于开发测试和首次进入时的占位展示
 * 包含：中心神殿结构、环形广场、柱子阵列、浮动装饰
 */
export function createDefaultScene(): SceneObjectDescriptor[] {
  return [
    // 中心神殿主体
    {
      id: 'temple-base',
      type: 'cylinder',
      position: [0, 0.3, 0],
      scale: [3, 0.6, 3],
      color: '#c9b896',
      material: { roughness: 0.4, metalness: 0.3 },
      castShadow: true,
      receiveShadow: true,
      tags: ['architecture', 'temple'],
    },
    // 中心柱
    {
      id: 'temple-pillar',
      type: 'cylinder',
      position: [0, 2.5, 0],
      params: { radiusTop: 0.6, radiusBottom: 0.8, height: 4, segments: 16 },
      color: '#e8dcc8',
      material: { roughness: 0.3, metalness: 0.4 },
      castShadow: true,
      receiveShadow: true,
      tags: ['architecture', 'temple'],
    },
    // 顶部穹顶
    {
      id: 'temple-dome',
      type: 'sphere',
      position: [0, 4.8, 0],
      params: { radius: 1.2, segments: 32, rings: 16 },
      color: '#4a6cf7',
      material: { roughness: 0.15, metalness: 0.7, emissive: '#1a2a55', emissiveIntensity: 0.5 },
      castShadow: true,
      receiveShadow: true,
      tags: ['architecture', 'temple', 'emissive'],
    },
    // 环形广场
    {
      id: 'plaza-ring',
      type: 'torus',
      position: [0, 0.05, 0],
      params: { radius: 4.5, tube: 0.25, radialSegments: 8, tubularSegments: 80 },
      color: '#8899aa',
      material: { roughness: 0.5, metalness: 0.3 },
      castShadow: true,
      receiveShadow: true,
      tags: ['architecture', 'plaza'],
    },
    // 外围柱子阵列
    ...createPillarRing(8, 5.5, 'outer-pillar'),
    // 内围柱子阵列
    ...createPillarRing(6, 3.8, 'inner-pillar'),
    // 四角灯塔
    ...createBeaconTowers(),
    // 浮动水晶
    ...createFloatingCrystals(12),
  ]
}

/** 创建一圈柱子 */
function createPillarRing(count: number, radius: number, prefix: string): SceneObjectDescriptor[] {
  return Array.from({ length: count }, (_, i) => {
    const angle = (i / count) * Math.PI * 2
    return {
      id: `${prefix}-${i}`,
      type: 'cylinder',
      position: [Math.cos(angle) * radius, 1.5, Math.sin(angle) * radius],
      params: { radiusTop: 0.15, radiusBottom: 0.2, height: 3, segments: 12 },
      color: '#b8a080',
      material: { roughness: 0.35, metalness: 0.3 },
      castShadow: true,
      receiveShadow: true,
      tags: ['architecture', 'pillar', prefix],
    }
  })
}

/** 四角灯塔 */
function createBeaconTowers(): SceneObjectDescriptor[] {
  const positions: [number, number, number][] = [
    [7, 1.8, 7], [-7, 1.8, 7], [7, 1.8, -7], [-7, 1.8, -7],
  ]
  return positions.map((pos, i) => ({
    id: `beacon-${i}`,
    type: 'group',
    position: pos,
    tags: ['architecture', 'beacon'],
    children: [
      {
        id: `beacon-${i}-base`,
        type: 'box',
        position: [0, 0, 0],
        scale: [1, 3.6, 1],
        color: '#665544',
        material: { roughness: 0.5, metalness: 0.2 },
        castShadow: true,
        receiveShadow: true,
      },
      {
        id: `beacon-${i}-light`,
        type: 'sphere',
        position: [0, 2.2, 0],
        params: { radius: 0.4, segments: 16, rings: 8 },
        color: '#ff9944',
        material: { roughness: 0.1, metalness: 0.1, emissive: '#ff6600', emissiveIntensity: 0.8 },
        tags: ['emissive', 'light-source'],
      },
    ],
  }))
}

/** 浮动水晶 */
function createFloatingCrystals(count: number): SceneObjectDescriptor[] {
  return Array.from({ length: count }, (_, i) => {
    const angle = (i / count) * Math.PI * 2 + (Math.random() - 0.5) * 0.3
    const radius = 2.5 + Math.random() * 3
    const height = 1.5 + Math.random() * 3.5
    const crystalTypes = ['octahedron', 'dodecahedron', 'icosahedron'] as const
    const type = crystalTypes[i % crystalTypes.length]
    const colors = ['#6c5ce7', '#4a6cf7', '#00b894', '#e17055', '#fdcb6e']
    const color = colors[i % colors.length]

    return {
      id: `crystal-${i}`,
      type,
      position: [Math.cos(angle) * radius, height, Math.sin(angle) * radius],
      params: { radius: 0.2 + Math.random() * 0.3, detail: 0 },
      color,
      material: {
        roughness: 0.15,
        metalness: 0.7,
        emissive: color,
        emissiveIntensity: 0.4,
      },
      castShadow: true,
      receiveShadow: true,
      tags: ['decoration', 'crystal', 'floating'],
    }
  })
}

// ========== 场景数据转换 ==========

/**
 * 将服务端返回的 sceneGraph JSON 转换为 SceneObjectDescriptor[]
 *
 * 支持的输入格式：
 *   1. SceneGraph 对象 { version, objects, ... }
 *   2. SceneObjectDescriptor[] 数组
 *   3. 任意包含 objects 字段的对象
 */
export function parseSceneGraph(raw: unknown): SceneObjectDescriptor[] {
  if (!raw) return []

  // 已是数组
  if (Array.isArray(raw)) {
    return raw as SceneObjectDescriptor[]
  }

  // 是对象
  if (typeof raw === 'object' && raw !== null) {
    const obj = raw as Record<string, unknown>

    // SceneGraph 格式
    if (Array.isArray(obj.objects)) {
      return obj.objects as SceneObjectDescriptor[]
    }
  }

  return []
}

/**
 * 如果服务端没有返回 sceneGraph，生成默认 Demo 场景
 */
export function getSceneObjects(spaceData: { sceneGraph?: unknown } | null): SceneObjectDescriptor[] {
  if (!spaceData?.sceneGraph) {
    return createDefaultScene()
  }

  const parsed = parseSceneGraph(spaceData.sceneGraph)
  if (parsed.length === 0) {
    return createDefaultScene()
  }

  return parsed
}
