<template>
  <div class="home-view">
    <header class="app-header">
      <h1 class="logo">Solra</h1>
      <nav class="main-nav">
        <router-link to="/">发现</router-link>
        <router-link to="/store">商城</router-link>
        <router-link to="/leaderboard">排行榜</router-link>
      </nav>
      <div class="header-actions">
        <template v-if="isAuthenticated">
          <router-link to="/inbox">消息</router-link>
          <router-link to="/profile">{{ userDisplayName }}</router-link>
        </template>
        <router-link v-else to="/login" class="btn-login">登录</router-link>
      </div>
    </header>

    <main class="feed-container">
      <div class="feed-header">
        <h2>探索空间</h2>
        <div class="feed-actions">
          <el-input
            v-model="searchQuery"
            placeholder="搜索空间..."
            class="search-input"
            clearable
            @keyup.enter="handleSearch"
            @clear="spaceStore.fetchSpaces(true)"
          >
            <template #prefix>
              <span>🔍</span>
            </template>
          </el-input>
          <router-link to="/create" class="btn-create">+ 创建空间</router-link>
        </div>
      </div>

      <div v-if="loading && spaces.length === 0" class="loading-state">
        <p>正在加载空间...</p>
      </div>

      <div v-else-if="error" class="error-state">
        <p>{{ error }}</p>
        <el-button @click="spaceStore.fetchSpaces(true)">重试</el-button>
      </div>

      <div v-else-if="spaces.length === 0" class="empty-state">
        <p>暂无空间，<router-link to="/create">创建第一个空间</router-link></p>
      </div>

      <div v-else class="space-grid">
        <div v-for="space in spaces" :key="space.spaceId" class="space-card" @click="enterSpace(space.spaceId)">
          <div class="card-thumb">
            <img :src="space.thumbnailUrl" :alt="space.title" loading="lazy" />
            <span class="card-online">{{ space.onlineCount }} 在线</span>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ space.title }}</h3>
            <p class="card-creator">by {{ space.creator?.displayName }}</p>
            <div class="card-tags">
              <span v-for="tag in space.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const spaceStore = useSpaceStore()
const authStore = useAuthStore()
const { isAuthenticated, userDisplayName } = storeToRefs(authStore)
const { spaces, loading, error } = storeToRefs(spaceStore)

const searchQuery = ref('')

onMounted(async () => {
  await spaceStore.fetchSpaces(true)
  if (authStore.isAuthenticated) {
    await authStore.fetchProfile()
  }
})

function enterSpace(spaceId: string) {
  router.push(`/spaces/${spaceId}`)
}

async function handleSearch() {
  if (searchQuery.value.trim()) {
    await spaceStore.searchSpaces(searchQuery.value.trim())
  } else {
    await spaceStore.fetchSpaces(true)
  }
}
</script>

<style lang="scss" scoped>
.home-view {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  background: var(--solra-bg-secondary);
  border-bottom: 1px solid var(--solra-border);

  .logo { font-size: 24px; color: var(--solra-accent); margin-right: 32px; }
  .main-nav { display: flex; gap: 20px; flex: 1; }
  .header-actions { display: flex; gap: 16px; align-items: center; }
  .btn-login {
    padding: 6px 16px;
    background: var(--solra-accent);
    color: #fff;
    border-radius: 8px;
  }
}

.feed-container { max-width: 1200px; margin: 0 auto; padding: 24px; }
.feed-header {
  display: flex; justify-content: space-between; align-items: center;
  flex-wrap: wrap; gap: 12px;
  margin-bottom: 24px;
  h2 { font-size: 28px; }
  .feed-actions {
    display: flex; align-items: center; gap: 12px;
  }
  .search-input {
    width: 260px;
  }
  .btn-create {
    padding: 10px 24px;
    background: var(--solra-accent);
    color: #fff;
    border-radius: 12px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.loading-state, .error-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--solra-text-secondary);
  font-size: 16px;
}

.space-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.space-card {
  background: var(--solra-bg-card);
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid var(--solra-border);

  &:hover { transform: translateY(-4px); box-shadow: 0 8px 30px rgba(108, 92, 231, 0.3); }

  .card-thumb {
    height: 160px;
    background: linear-gradient(135deg, var(--solra-accent), #a29bfe);
    position: relative;
    img { width: 100%; height: 100%; object-fit: cover; }
    .card-online {
      position: absolute; top: 10px; right: 10px;
      background: rgba(0,0,0,0.6); color: #fff; padding: 4px 10px; border-radius: 20px; font-size: 13px;
    }
  }

  .card-body { padding: 16px; }
  .card-title { font-size: 18px; margin-bottom: 4px; }
  .card-creator { color: var(--solra-text-secondary); font-size: 14px; margin-bottom: 10px; }
  .card-tags { display: flex; gap: 6px; flex-wrap: wrap; }
  .tag {
    padding: 2px 10px;
    background: rgba(108, 92, 231, 0.2);
    color: var(--solra-accent);
    border-radius: 12px;
    font-size: 12px;
  }
}
</style>
