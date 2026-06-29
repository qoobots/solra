<script setup lang="ts">
import { onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'

onMounted(async () => {
  try {
    const info = await invoke('get_system_info')
    console.log('Solra Desktop 已启动:', info)
  } catch (e) {
    console.warn('Tauri 后端未连接（浏览器开发模式）')
  }
})
</script>

<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<style lang="scss">
.app-container {
  width: 100%;
  min-height: 100vh;
}
</style>
