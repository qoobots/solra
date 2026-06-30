/**
 * 3D 场景对象管理器 — SceneManager
 *
 * 职责：
 *   - 管理场景中所有 3D 对象（建筑、装饰、地形、用户化身等）的增删改查
 *   - 提供统一的对象创建工厂方法
 *   - 支持从 SpaceDetail.sceneGraph 数据批量构建场景
 *   - 管理光照系统参数
 *   - 管理网格地面 / 粒子场等场景装饰
 *
 * 设计原则：
 *   - 单一职责：只管理场景对象，不负责渲染循环或相机
 *   - 可测试：所有方法接受 THREE.Scene 作为显式参数
 */

import * as THREE from 'three'

// ---- 类型定义 ----

/** 场景对象描述 */
export interface SceneObjectDescriptor {
  id: string
  type: 'box' | 'sphere' | 'cylinder' | 'plane' | 'cone' | 'torus' | 'icosahedron' | 'dodecahedron' | 'octahedron' | 'torusKnot' | 'group' | 'model'
  position: [number, number, number]
  rotation?: [number, number, number]
  scale?: [number, number, number]
  color?: string
  /** 几何体参数 */
  params?: Record<string, number>
  /** 材质属性 */
  material?: {
    roughness?: number
    metalness?: number
    emissive?: string
    emissiveIntensity?: number
    wireframe?: boolean
    opacity?: number
    transparent?: boolean
  }
  /** 子对象（type=group 时） */
  children?: SceneObjectDescriptor[]
  /** 阴影 */
  castShadow?: boolean
  receiveShadow?: boolean
  /** 自定义标签 */
  tags?: string[]
}

/** 光照配置 */
export interface LightingConfig {
  ambientColor: string
  ambientIntensity: number
  hemisphereSkyColor: string
  hemisphereGroundColor: string
  hemisphereIntensity: number
  sunPosition: [number, number, number]
  sunColor: string
  sunIntensity: number
  fillPosition: [number, number, number]
  fillColor: string
  fillIntensity: number
  rimPosition: [number, number, number]
  rimColor: string
  rimIntensity: number
}

/** 场景装饰配置 */
export interface SceneDecorationConfig {
  showGrid: boolean
  showParticles: boolean
  gridSize: number
  gridDivisions: number
  particleCount: number
  particleRadius: number
}

// ---- 默认光照配置 ----
export const DEFAULT_LIGHTING: LightingConfig = {
  ambientColor: '#4a6cf7',
  ambientIntensity: 0.6,
  hemisphereSkyColor: '#87ceeb',
  hemisphereGroundColor: '#362850',
  hemisphereIntensity: 0.5,
  sunPosition: [15, 20, 8],
  sunColor: '#ffffff',
  sunIntensity: 3.5,
  fillPosition: [-5, 2, -3],
  fillColor: '#8899cc',
  fillIntensity: 1.2,
  rimPosition: [0, -1, 0],
  rimColor: '#4466aa',
  rimIntensity: 0.8,
}

// ---- 默认装饰配置 ----
export const DEFAULT_DECORATION: SceneDecorationConfig = {
  showGrid: true,
  showParticles: true,
  gridSize: 30,
  gridDivisions: 48,
  particleCount: 600,
  particleRadius: 14,
}

// ---- 场景管理器 ----
export class SceneManager {
  /** 已注册的对象 Map */
  private objects = new Map<string, THREE.Object3D>()
  /** 光照对象引用 */
  private lights: {
    ambient: THREE.AmbientLight | null
    hemisphere: THREE.HemisphereLight | null
    sun: THREE.DirectionalLight | null
    fill: THREE.DirectionalLight | null
    rim: THREE.DirectionalLight | null
  } = { ambient: null, hemisphere: null, sun: null, fill: null, rim: null }
  /** 装饰对象引用 */
  private decorations: {
    grid: THREE.PolarGridHelper | null
    ground: THREE.Mesh | null
    particles: THREE.Points | null
  } = { grid: null, ground: null, particles: null }

  /** 当前光照配置 */
  private lightingConfig: LightingConfig = { ...DEFAULT_LIGHTING }
  /** 当前装饰配置 */
  private decorationConfig: SceneDecorationConfig = { ...DEFAULT_DECORATION }

  // ========== 场景初始化 ==========

  /** 初始化完整场景：光照 + 地面 + 粒子 */
  setupScene(scene: THREE.Scene): void {
    this.setupLighting(scene)
    this.createGround(scene)
    this.createParticleField(scene)
  }

  /** 设置光照系统 */
  setupLighting(scene: THREE.Scene, config?: Partial<LightingConfig>): void {
    if (config) {
      Object.assign(this.lightingConfig, config)
    }
    const cfg = this.lightingConfig

    // 环境光
    this.lights.ambient = new THREE.AmbientLight(cfg.ambientColor, cfg.ambientIntensity)
    scene.add(this.lights.ambient)

    // 半球光
    this.lights.hemisphere = new THREE.HemisphereLight(
      cfg.hemisphereSkyColor,
      cfg.hemisphereGroundColor,
      cfg.hemisphereIntensity
    )
    scene.add(this.lights.hemisphere)

    // 主方向光（太阳）
    this.lights.sun = new THREE.DirectionalLight(cfg.sunColor, cfg.sunIntensity)
    this.lights.sun.position.set(...cfg.sunPosition)
    this.lights.sun.castShadow = true
    this.lights.sun.shadow.mapSize.width = 2048
    this.lights.sun.shadow.mapSize.height = 2048
    this.lights.sun.shadow.camera.near = 0.5
    this.lights.sun.shadow.camera.far = 80
    this.lights.sun.shadow.camera.left = -20
    this.lights.sun.shadow.camera.right = 20
    this.lights.sun.shadow.camera.top = 20
    this.lights.sun.shadow.camera.bottom = -20
    this.lights.sun.shadow.bias = -0.0001
    this.lights.sun.shadow.normalBias = 0.02
    scene.add(this.lights.sun)

    // 补光
    this.lights.fill = new THREE.DirectionalLight(cfg.fillColor, cfg.fillIntensity)
    this.lights.fill.position.set(...cfg.fillPosition)
    scene.add(this.lights.fill)

    // 底部补光
    this.lights.rim = new THREE.DirectionalLight(cfg.rimColor, cfg.rimIntensity)
    this.lights.rim.position.set(...cfg.rimPosition)
    scene.add(this.lights.rim)
  }

  /** 更新光照参数（实时调整） */
  updateLighting(config: Partial<LightingConfig>): void {
    Object.assign(this.lightingConfig, config)
    const cfg = this.lightingConfig

    if (this.lights.ambient) {
      this.lights.ambient.color.set(cfg.ambientColor)
      this.lights.ambient.intensity = cfg.ambientIntensity
    }
    if (this.lights.hemisphere) {
      this.lights.hemisphere.color.set(cfg.hemisphereSkyColor)
      this.lights.hemisphere.groundColor.set(cfg.hemisphereGroundColor)
      this.lights.hemisphere.intensity = cfg.hemisphereIntensity
    }
    if (this.lights.sun) {
      this.lights.sun.color.set(cfg.sunColor)
      this.lights.sun.intensity = cfg.sunIntensity
      this.lights.sun.position.set(...cfg.sunPosition)
    }
    if (this.lights.fill) {
      this.lights.fill.color.set(cfg.fillColor)
      this.lights.fill.intensity = cfg.fillIntensity
      this.lights.fill.position.set(...cfg.fillPosition)
    }
    if (this.lights.rim) {
      this.lights.rim.color.set(cfg.rimColor)
      this.lights.rim.intensity = cfg.rimIntensity
      this.lights.rim.position.set(...cfg.rimPosition)
    }
  }

  /** 获取当前光照配置 */
  getLightingConfig(): LightingConfig {
    return { ...this.lightingConfig }
  }

  // ========== 对象管理 ==========

  /**
   * 从描述符创建 3D 对象并添加到场景
   * @returns 创建的 Object3D
   */
  addObject(scene: THREE.Scene, desc: SceneObjectDescriptor): THREE.Object3D {
    const obj = this.createObjectFromDescriptor(desc)
    obj.name = desc.id
    obj.userData = { descriptor: desc, tags: desc.tags || [] }

    scene.add(obj)
    this.objects.set(desc.id, obj)
    return obj
  }

  /**
   * 批量添加对象
   */
  addObjects(scene: THREE.Scene, descriptors: SceneObjectDescriptor[]): THREE.Object3D[] {
    return descriptors.map((desc) => this.addObject(scene, desc))
  }

  /** 移除对象 */
  removeObject(scene: THREE.Scene, id: string): boolean {
    const obj = this.objects.get(id)
    if (!obj) return false

    scene.remove(obj)
    this.disposeObject(obj)
    this.objects.delete(id)
    return true
  }

  /** 获取对象 */
  getObject(id: string): THREE.Object3D | undefined {
    return this.objects.get(id)
  }

  /** 获取所有对象 ID */
  getObjectIds(): string[] {
    return Array.from(this.objects.keys())
  }

  /** 按标签查找对象 */
  getObjectsByTag(tag: string): THREE.Object3D[] {
    const result: THREE.Object3D[] = []
    this.objects.forEach((obj) => {
      const tags: string[] = obj.userData.tags || []
      if (tags.includes(tag)) {
        result.push(obj)
      }
    })
    return result
  }

  /** 清除所有对象 */
  clearObjects(scene: THREE.Scene): void {
    this.objects.forEach((obj) => {
      scene.remove(obj)
      this.disposeObject(obj)
    })
    this.objects.clear()
  }

  /** 更新对象变换 */
  updateTransform(id: string, position?: [number, number, number], rotation?: [number, number, number], scale?: [number, number, number]): boolean {
    const obj = this.objects.get(id)
    if (!obj) return false
    if (position) obj.position.set(...position)
    if (rotation) obj.rotation.set(...rotation)
    if (scale) obj.scale.set(...scale)
    return true
  }

  // ========== 装饰管理 ==========

  /** 创建网格地面 */
  createGround(scene: THREE.Scene, config?: Partial<SceneDecorationConfig>): void {
    if (config) Object.assign(this.decorationConfig, config)
    const cfg = this.decorationConfig

    // 移除旧装饰
    this.removeGround(scene)

    if (!cfg.showGrid) return

    // 极坐标网格
    this.decorations.grid = new THREE.PolarGridHelper(
      cfg.gridSize,
      cfg.gridDivisions,
      24,
      128,
      '#334466',
      '#223355'
    )
    scene.add(this.decorations.grid)

    // 实体地面（接收阴影）
    const groundGeo = new THREE.PlaneGeometry(cfg.gridSize * 2, cfg.gridSize * 2)
    const groundMat = new THREE.MeshStandardMaterial({
      color: '#1a1a2e',
      roughness: 0.85,
      metalness: 0.1,
    })
    this.decorations.ground = new THREE.Mesh(groundGeo, groundMat)
    this.decorations.ground.rotation.x = -Math.PI / 2
    this.decorations.ground.position.y = -0.05
    this.decorations.ground.receiveShadow = true
    this.decorations.ground.name = 'ground-plane'
    scene.add(this.decorations.ground)
  }

  /** 移除地面装饰 */
  private removeGround(scene: THREE.Scene): void {
    if (this.decorations.grid) {
      scene.remove(this.decorations.grid)
      this.decorations.grid = null
    }
    if (this.decorations.ground) {
      scene.remove(this.decorations.ground)
      this.decorations.ground.geometry?.dispose()
      ;(this.decorations.ground.material as THREE.Material)?.dispose()
      this.decorations.ground = null
    }
  }

  /** 创建粒子场 */
  createParticleField(scene: THREE.Scene, config?: Partial<SceneDecorationConfig>): void {
    if (config) Object.assign(this.decorationConfig, config)
    const cfg = this.decorationConfig

    // 移除旧粒子
    this.removeParticles(scene)

    if (!cfg.showParticles) return

    const count = cfg.particleCount
    const positions = new Float32Array(count * 3)
    const colors = new Float32Array(count * 3)

    for (let i = 0; i < count; i++) {
      const theta = Math.random() * Math.PI * 2
      const phi = Math.acos(2 * Math.random() - 1)
      const r = 6 + Math.random() * cfg.particleRadius

      positions[i * 3] = Math.cos(theta) * Math.sin(phi) * r
      positions[i * 3 + 1] = Math.sin(phi) * r * 0.4 + 2
      positions[i * 3 + 2] = Math.cos(phi) * r

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

    this.decorations.particles = new THREE.Points(geo, mat)
    this.decorations.particles.name = 'particle-field'
    scene.add(this.decorations.particles)
  }

  /** 移除粒子 */
  private removeParticles(scene: THREE.Scene): void {
    if (this.decorations.particles) {
      scene.remove(this.decorations.particles)
      this.decorations.particles.geometry?.dispose()
      ;(this.decorations.particles.material as THREE.Material)?.dispose()
      this.decorations.particles = null
    }
  }

  /** 更新网格可见性 */
  setGridVisible(scene: THREE.Scene, visible: boolean): void {
    this.decorationConfig.showGrid = visible
    if (visible) {
      this.createGround(scene)
    } else {
      this.removeGround(scene)
    }
  }

  /** 更新粒子可见性 */
  setParticlesVisible(scene: THREE.Scene, visible: boolean): void {
    this.decorationConfig.showParticles = visible
    if (visible) {
      this.createParticleField(scene)
    } else {
      this.removeParticles(scene)
    }
  }

  /** 更新全局线框模式 */
  setWireframeMode(scene: THREE.Scene, enabled: boolean): void {
    scene.traverse((obj) => {
      if (obj instanceof THREE.Mesh) {
        const materials = Array.isArray(obj.material) ? obj.material : [obj.material]
        materials.forEach((mat) => {
          if (mat instanceof THREE.MeshStandardMaterial || mat instanceof THREE.MeshPhongMaterial || mat instanceof THREE.MeshLambertMaterial) {
            mat.wireframe = enabled
          }
        })
      }
    })
  }

  // ========== 对象更新（每帧调用） ==========

  /** 更新指定对象的动画（delta: 秒, elapsed: 秒） */
  animateObject(id: string, delta: number, elapsed: number, animation?: 'breathe' | 'rotate' | 'float'): void {
    const obj = this.objects.get(id)
    if (!obj) return

    switch (animation) {
      case 'breathe':
        obj.scale.setScalar(1 + Math.sin(elapsed * 1.5) * 0.08)
        break
      case 'rotate':
        obj.rotation.y += delta * 0.3
        obj.rotation.x += delta * 0.15
        break
      case 'float':
        obj.position.y += Math.sin(elapsed * 2) * delta * 0.3
        break
    }
  }

  /** 更新粒子旋转 */
  updateParticles(delta: number): void {
    if (this.decorations.particles) {
      this.decorations.particles.rotation.y += delta * 0.05
      this.decorations.particles.rotation.x += delta * 0.02
    }
  }

  // ========== 场景销毁 ==========

  /** 清理整个场景 */
  dispose(scene: THREE.Scene): void {
    this.clearObjects(scene)
    this.removeGround(scene)
    this.removeParticles(scene)

    // 移除光照
    const allLights = [this.lights.ambient, this.lights.hemisphere, this.lights.sun, this.lights.fill, this.lights.rim]
    allLights.forEach((light) => {
      if (light) scene.remove(light)
    })
    this.lights = { ambient: null, hemisphere: null, sun: null, fill: null, rim: null }
  }

  // ========== 私有方法 ==========

  /** 根据几何体类型创建对应的 BufferGeometry */
  private createGeometry(type: SceneObjectDescriptor['type'], params?: Record<string, number>): THREE.BufferGeometry {
    const p = params || {}
    switch (type) {
      case 'box':
        return new THREE.BoxGeometry(p.width || 1, p.height || 1, p.depth || 1, p.segments || 1, p.segments || 1, p.segments || 1)
      case 'sphere':
        return new THREE.SphereGeometry(p.radius || 1, p.segments || 32, p.rings || 16)
      case 'cylinder':
        return new THREE.CylinderGeometry(p.radiusTop || 0.5, p.radiusBottom || 0.5, p.height || 1, p.segments || 32)
      case 'plane':
        return new THREE.PlaneGeometry(p.width || 1, p.height || 1)
      case 'cone':
        return new THREE.ConeGeometry(p.radius || 0.5, p.height || 1, p.segments || 32)
      case 'torus':
        return new THREE.TorusGeometry(p.radius || 1, p.tube || 0.3, p.radialSegments || 16, p.tubularSegments || 64)
      case 'icosahedron':
        return new THREE.IcosahedronGeometry(p.radius || 1, p.detail || 1)
      case 'dodecahedron':
        return new THREE.DodecahedronGeometry(p.radius || 1, p.detail || 0)
      case 'octahedron':
        return new THREE.OctahedronGeometry(p.radius || 1, p.detail || 0)
      case 'torusKnot':
        return new THREE.TorusKnotGeometry(p.radius || 1, p.tube || 0.2, p.tubularSegments || 64, p.radialSegments || 8)
      default:
        return new THREE.BoxGeometry(1, 1, 1)
    }
  }

  /** 从描述符创建 Object3D */
  private createObjectFromDescriptor(desc: SceneObjectDescriptor): THREE.Object3D {
    // 组类型
    if (desc.type === 'group') {
      const group = new THREE.Group()
      if (desc.children) {
        desc.children.forEach((child) => {
          const childObj = this.createObjectFromDescriptor(child)
          group.add(childObj)
        })
      }
      return group
    }

    // 模型占位类型
    if (desc.type === 'model') {
      // 模型未加载时显示占位
      const placeholder = new THREE.Mesh(
        new THREE.BoxGeometry(1, 1, 1),
        new THREE.MeshStandardMaterial({ color: '#ffaa00', wireframe: true, opacity: 0.5, transparent: true })
      )
      placeholder.userData.isPlaceholder = true
      placeholder.userData.modelUrl = desc.params?.url
      return placeholder
    }

    // 普通几何体
    const geometry = this.createGeometry(desc.type, desc.params)
    const material = this.createMaterial(desc)
    const mesh = new THREE.Mesh(geometry, material)

    // 设置变换
    mesh.position.set(...desc.position)
    if (desc.rotation) mesh.rotation.set(...desc.rotation)
    if (desc.scale) mesh.scale.set(...desc.scale)
    if (desc.castShadow !== undefined) mesh.castShadow = desc.castShadow
    if (desc.receiveShadow !== undefined) mesh.receiveShadow = desc.receiveShadow

    return mesh
  }

  /** 创建材质 */
  private createMaterial(desc: SceneObjectDescriptor): THREE.MeshStandardMaterial {
    const m = desc.material || {}
    return new THREE.MeshStandardMaterial({
      color: desc.color || '#4a6cf7',
      roughness: m.roughness ?? 0.3,
      metalness: m.metalness ?? 0.5,
      emissive: m.emissive || undefined,
      emissiveIntensity: m.emissiveIntensity ?? 0,
      wireframe: m.wireframe ?? false,
      opacity: m.opacity ?? 1,
      transparent: m.transparent ?? false,
    })
  }

  /** 递归释放对象资源 */
  private disposeObject(obj: THREE.Object3D): void {
    obj.traverse((child) => {
      if (child instanceof THREE.Mesh) {
        child.geometry?.dispose()
        if (Array.isArray(child.material)) {
          child.material.forEach((m) => m.dispose())
        } else {
          child.material?.dispose()
        }
      }
      if (child instanceof THREE.Points) {
        child.geometry?.dispose()
        ;(child.material as THREE.Material)?.dispose()
      }
    })
  }
}
