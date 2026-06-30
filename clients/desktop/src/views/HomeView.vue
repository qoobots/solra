<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'
import { invoke } from '@tauri-apps/api/core'

const spaceStore = useSpaceStore()
const authStore = useAuthStore()
const router = useRouter()
const searchQuery = ref('')

onMounted(async () => {
  await spaceStore.fetchSpaces()
  if (authStore.isAuthenticated) {
    await authStore.fetchProfile()
  }
  // 获取系统信息展示在标题栏
  try {
    const sysInfo = await invoke<any>('get_system_info')
    console.log('System info:', sysInfo)
  } catch {
    // 静默处理
  }
})

function goToSpace(spaceId: string) {
  router.push(`/spaces/${spaceId}`)
}
</script>

<template>
  <div class="home-view">
    <header class="app-header">
      <h1 class="logo">索拉 Solra</h1>
      <div class="search-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索空间..."
          size="small"
          clearable
          class="search-input"
        >
          <template #prefix><span>🔍</span></template>
        </el-input>
      </div>
      <nav class="nav-links">
        <router-link to="/store">商城</router-link>
        <router-link to="/leaderboard">排行榜</router-link>
        <router-link to="/inbox">消息</router-link>
        <router-link to="/profile">{{ authStore.userDisplayName || '我的' }}</router-link>
      </nav>
    </header>

    <main class="feed-container">
      <div class="feed-header">
        <h2>发现空间</h2>
        <router-link to="/create" class="btn-create">+ 创建空间</router-link>
      </div>

      <div v-if="spaceStore.loading && spaceStore.spaces.length === 0" class="loading">
        加载中...
      </div>

      <div v-else-if="spaceStore.error" class="error-state">
        <p>{{ spaceStore.error }}</p>
        <el-button @click="spaceStore.fetchSpaces(true)">重试</el-button>
      </div>

      <div v-else class="space-grid">
        <div
          v-for="space in spaceStore.spaces"
          :key="space.spaceId"
          class="space-card"
          @click="goToSpace(space.spaceId)"
        >
          <div class="card-thumbnail">
            <img v-if="space.thumbnailUrl" :src="space.thumbnailUrl" :alt="space.title" />
            <div v-else class="thumb-placeholder">
              <span>🌌</span>
            </div>
          </div>
          <div class="card-info">
            <h3>{{ space.title }}</h3>
            <p>{{ space.description }}</p>
            <div class="card-meta">
              <span>{{ space.creatorName }}</span>
              <span>🟢 {{ space.onlineCount }} 在线</span>
              <span v-if="space.likeCount">❤️ {{ space.likeCount }}</span>
            </div>
            <div class="card-tags" v-if="space.tags.length">
              <span v-for="tag in space.tags.slice(0, 3)" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style lang="scss" scoped>
.home-view {
  min-height: 100vh;
  background: var(--solra-bg-primary, #0d1117);
  color: var(--solra-text-primary, #e6edf3);
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 32px;
  border-bottom: 1px solid var(--solra-border, #30363d);
  gap: 16px;

  .logo {
    font-size: 22px;
    font-weight: 700;
    color: #58a6ff;
    margin: 0;
    white-space: nowrap;
  }

  .search-bar {
    flex: 1;
    max-width: 360px;
    .search-input { width: 100%; }
  }

  .nav-links {
    display: flex;
    gap: 20px;

    a {
      color: #8b949e;
      text-decoration: none;
      font-size: 14px;
      white-space: nowrap;

      &:hover { color: #58a6ff; }
    }
  }
}

.feed-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 32px;

  .feed-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h2 { font-size: 20px; margin: 0; }

    .btn-create {
      padding: 8px 20px;
      background: #58a6ff;
      color: #fff;
      border-radius: 8px;
      text-decoration: none;
      font-size: 14px;
      font-weight: 600;

      &:hover { background: #79b8ff; }
    }
  }
}

.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.space-card {
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s;

  &:hover {
    border-color: #58a6ff;
    transform: translateY(-2px);
  }

  .card-thumbnail {
    width: 100%;
    height: 160px;
    overflow: hidden;
    background: linear-gradient(135deg, #1a1a2e, #16213e);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .thumb-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 48px;
    }
  }

  .card-info {
    padding: 16px;

    h3 {
      margin: 0 0 8px;
      font-size: 16px;
    }

    p {
      margin: 0 0 12px;
      font-size: 13px;
      color: #8b949e;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-meta {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: #6e7681;
      margin-bottom: 8px;
    }

    .card-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;

      .tag {
        font-size: 11px;
        color: #58a6ff;
        background: rgba(88, 166, 255, 0.1);
        padding: 2px 8px;
        border-radius: 10px;
      }
    }
  }
}

.loading {
  text-align: center;
  padding: 60px;
  color: #8b949e;
}

.error-state {
  text-align: center;
  padding: 60px;
  color: #ff7675;
}
</style>
