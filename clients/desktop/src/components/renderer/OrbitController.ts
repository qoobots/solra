/**
 * 轨道相机控制器 — OrbitController
 *
 * 封装 THREE.OrbitControls，提供：
 *   - 鼠标拖拽旋转 / 滚轮缩放 / 右键平移
 *   - 相机速度调节
 *   - 目标点管理
 *   - 限制角度和距离
 *
 * 使用方式：
 *   const controller = new OrbitController(camera, domElement)
 *   controller.update() // 每帧调用
 *   controller.setTarget(x, y, z) // 聚焦到某点
 *   controller.dispose() // 销毁
 */

import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

export class OrbitController {
  private controls: OrbitControls
  private _speed: number = 1.0

  constructor(camera: THREE.PerspectiveCamera | THREE.OrthographicCamera, domElement: HTMLElement) {
    this.controls = new OrbitControls(camera, domElement)

    // 默认配置
    this.controls.enableDamping = true
    this.controls.dampingFactor = 0.08
    this.controls.minDistance = 2
    this.controls.maxDistance = 50
    this.controls.maxPolarAngle = Math.PI * 0.85 // 限制不能翻到底部
    this.controls.minPolarAngle = 0.1
    this.controls.target.set(0, 1.5, 0)
    this.controls.update()

    // 禁用右键菜单（用于平移）
    domElement.addEventListener('contextmenu', this.preventContextMenu)
  }

  /** 每帧更新（必须在渲染循环中调用） */
  update(): void {
    this.controls.update()
  }

  /** 设置相机聚焦目标 */
  setTarget(x: number, y: number, z: number): void {
    this.controls.target.set(x, y, z)
  }

  /** 获取当前目标点 */
  getTarget(): THREE.Vector3 {
    return this.controls.target.clone()
  }

  /** 设置/获取旋转速度 */
  get speed(): number { return this._speed }
  set speed(v: number) {
    this._speed = Math.max(0.1, Math.min(5, v))
    this.controls.rotateSpeed = 0.5 * this._speed
    this.controls.zoomSpeed = 1.2 * this._speed
    this.controls.panSpeed = 0.7 * this._speed
  }

  /** 启用/禁用 */
  set enabled(v: boolean) {
    this.controls.enabled = v
  }
  get enabled(): boolean {
    return this.controls.enabled
  }

  /** 设置距离限制 */
  setDistanceLimits(min: number, max: number): void {
    this.controls.minDistance = min
    this.controls.maxDistance = max
  }

  /** 重置到默认视角 */
  reset(cameraPosition?: [number, number, number], target?: [number, number, number]): void {
    if (target) this.controls.target.set(...target)
    if (cameraPosition && this.controls.object instanceof THREE.PerspectiveCamera) {
      this.controls.object.position.set(...cameraPosition)
      this.controls.object.lookAt(this.controls.target)
    }
    this.controls.update()
  }

  /** 聚焦到指定对象（将相机移动到能看清该对象的位置） */
  focusOn(object: THREE.Object3D, distance = 5): void {
    const box = new THREE.Box3().setFromObject(object)
    const center = new THREE.Vector3()
    box.getCenter(center)
    this.controls.target.copy(center)

    // 将相机移到目标前方
    if (this.controls.object instanceof THREE.PerspectiveCamera) {
      const offset = new THREE.Vector3(0, distance * 0.4, distance)
      this.controls.object.position.copy(center).add(offset)
    }
    this.controls.update()
  }

  /** 销毁 */
  dispose(): void {
    this.controls.dispose()
    if (this.controls.domElement) {
      this.controls.domElement.removeEventListener('contextmenu', this.preventContextMenu)
    }
  }

  private preventContextMenu = (e: Event) => {
    e.preventDefault()
  }
}
