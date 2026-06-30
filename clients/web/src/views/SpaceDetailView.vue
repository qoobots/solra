<template>
  <div class="detail-view">
    <header class="detail-header">
      <el-button @click="$router.back()">← 返回</el-button>
      <h2>{{ space?.title || '加载中...' }}</h2>
      <el-button type="primary">进入空间</el-button>
    </header>

    <div v-if="loading" class="loading-state">
      <p>正在加载空间详情...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <el-button @click="spaceStore.fetchSpaceDetail(route.params.spaceId as string)">重试</el-button>
    </div>

    <div class="detail-body" v-else-if="space">
      <div class="detail-main">
        <div class="detail-hero">
          <img v-if="space.thumbnailUrl" :src="space.thumbnailUrl" :alt="space.title" />
          <div v-else class="hero-placeholder">
            <span>🌌</span>
          </div>
        </div>
        <div class="detail-tags" v-if="space.tags.length">
          <span v-for="tag in space.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <p class="detail-desc">{{ space.description }}</p>
        <div class="detail-meta">
          <span>🟢 {{ space.onlineCount }} 在线</span>
          <span>👁️ {{ space.totalVisits }} 次访问</span>
          <span>⭐ 信仰分 {{ space.faithScore }}</span>
          <span v-if="space.creator">创作者: {{ space.creator.displayName }}</span>
        </div>
        <div class="detail-actions">
          <el-button type="primary" size="large" disabled>进入空间（需要桌面客户端）</el-button>
          <el-button size="large">分享</el-button>
        </div>
      </div>
      <aside class="detail-sidebar">
        <div class="participants-card">
          <h3>在线用户 ({{ space.participants?.length || 0 }})</h3>
          <div v-if="space.participants?.length" class="participant-list">
            <div v-for="p in space.participants" :key="p.userId" class="participant-item">
              <div class="participant-avatar">
                <img v-if="p.avatarUrl" :src="p.avatarUrl" :alt="p.displayName" />
                <span v-else>👤</span>
              </div>
              <span class="participant-name">{{ p.displayName }}</span>
              <span class="online-dot" :class="{ online: p.isOnline }"></span>
            </div>
          </div>
          <p v-else class="placeholder-text">暂无在线用户</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const spaceStore = useSpaceStore()
const { currentSpace: space, loading, error } = storeToRefs(spaceStore)

onMounted(async () => {
  const spaceId = route.params.spaceId as string
  await spaceStore.fetchSpaceDetail(spaceId)
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
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  img { width: 100%; height: 100%; object-fit: cover; }
  .hero-placeholder { font-size: 80px; }
}

.detail-tags {
  display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px;
  .tag {
    padding: 4px 12px;
    background: rgba(108, 92, 231, 0.2);
    color: var(--solra-accent);
    border-radius: 12px;
    font-size: 13px;
  }
}

.detail-desc { font-size: 16px; line-height: 1.6; color: var(--solra-text-secondary); margin-bottom: 16px; }
.detail-meta {
  display: flex; gap: 24px; color: var(--solra-text-secondary); font-size: 14px;
  flex-wrap: wrap;
}

.detail-actions {
  display: flex; gap: 12px; margin-top: 20px;
}

.loading-state, .error-state {
  text-align: center; padding: 60px 20px; color: var(--solra-text-secondary);
}

.detail-sidebar {
  .participants-card {
    background: var(--solra-bg-card);
    border: 1px solid var(--solra-border);
    border-radius: 12px;
    padding: 20px;
    h3 { margin-bottom: 16px; }
  }
}

.participant-list {
  display: flex; flex-direction: column; gap: 10px;
}
.participant-item {
  display: flex; align-items: center; gap: 10px;
  .participant-avatar {
    width: 32px; height: 32px; border-radius: 50%;
    background: var(--solra-bg-secondary);
    display: flex; align-items: center; justify-content: center;
    overflow: hidden;
    img { width: 100%; height: 100%; object-fit: cover; }
  }
  .participant-name { flex: 1; font-size: 14px; }
  .online-dot {
    width: 8px; height: 8px; border-radius: 50%;
    background: var(--solra-text-secondary);
    &.online { background: var(--solra-success); }
  }
}

.placeholder-text { color: var(--solra-text-secondary); font-size: 14px; }
</style>
