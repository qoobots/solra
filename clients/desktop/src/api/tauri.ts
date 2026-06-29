/**
 * Tauri IPC 调用封装
 * 前端通过 invoke() 调用 Rust 后端的 Tauri Commands
 */
import { invoke } from '@tauri-apps/api/core'

// ---- 空间相关 ----

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

export async function getSpaces(page = 1, pageSize = 20): Promise<SpaceSummary[]> {
  return invoke('get_spaces', { page, pageSize })
}

export async function enterSpace(spaceId: string): Promise<void> {
  return invoke('enter_space', { spaceId })
}

export async function exitSpace(spaceId: string): Promise<void> {
  return invoke('exit_space', { spaceId })
}

// ---- 虚拟人相关 ----

export async function startConversation(avatarId: string, spaceId: string): Promise<{ id: string; status: string }> {
  return invoke('start_conversation', { avatarId, spaceId })
}

export async function sendMessage(conversationId: string, message: string): Promise<string> {
  return invoke('send_message', { conversationId, message })
}

export async function stopConversation(conversationId: string): Promise<void> {
  return invoke('stop_conversation', { conversationId })
}

// ---- 渲染器相关 ----

export async function initRenderer(width: number, height: number): Promise<{ initialized: boolean; fps: number; gpu_backend: string }> {
  return invoke('init_renderer', { width, height })
}

export async function getFps(): Promise<number> {
  return invoke('get_fps')
}

// ---- 系统相关 ----

export interface SystemInfo {
  os: string
  arch: string
  cpu_cores: number
  total_memory_gb: number
  gpu_name: string
  core_sdk_loaded: boolean
  core_sdk_version: string
}

export async function getSystemInfo(): Promise<SystemInfo> {
  return invoke('get_system_info')
}

export async function getCoreVersion(): Promise<string> {
  return invoke('get_core_version')
}
