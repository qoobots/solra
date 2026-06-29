<template>
  <div class="detail-view">
    <header class="detail-header">
      <el-button @click="$router.back()">← 返回</el-button>
      <h2>{{ space?.title || '加载中...' }}</h2>
      <el-button type="primary">进入空间</el-button>
    </header>

    <div class="detail-body" v-if="space">
      <div class="detail-main">
        <div class="detail-hero"></div>
        <p class="detail-desc">{{ space.description }}</p>
        <div class="detail-meta">
          <span>{{ space.onlineCount }} 在线</span>
          <span>创作者: {{ space.creator?.displayName }}</span>
        </div>
      </div>
      <aside class="detail-sidebar">
        <div class="participants-card">
          <h3>在线用户</h3>
          <p class="placeholder-text">空间加载后将显示在线用户</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const space = ref<any>(null)

onMounted(() => {
  const spaceId = route.params.spaceId as string
  space.value = {
    spaceId,
    title: '示例空间',
    description: '这是一个虚拟空间的详细页面，将展示空间的完整信息。',
    onlineCount: 42,
    creator: { displayName: 'Alice' },
  }
})
</script>

<style lang="scss" scoped>
.detail-view { min-height: 100vh; }
.detail-header {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 24px;
  background: var(--solra-bg-secondary);
  border-bottom: 1px solid var(--solra-border);
  h2 { flex: 1; font-size: 20px; }
}
.detail-body {
  display: grid; grid-template-columns: 1fr 320px; gap: 24px;
  max-width: 1200px; margin: 24px auto; padding: 0 24px;
}
.detail-hero {
  height: 360px;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  border-radius: 16px;
  margin-bottom: 20px;
}
.detail-desc { font-size: 16px; line-height: 1.6; color: var(--solra-text-secondary); margin-bottom: 16px; }
.detail-meta { display: flex; gap: 24px; color: var(--solra-text-secondary); font-size: 14px; }

.detail-sidebar {
  .participants-card {
    background: var(--solra-bg-card);
    border: 1px solid var(--solra-border);
    border-radius: 12px;
    padding: 20px;
    h3 { margin-bottom: 12px; }
  }
}
.placeholder-text { color: var(--solra-text-secondary); font-size: 14px; }
</style>
