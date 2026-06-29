<script setup lang="ts">
import { onMounted } from 'vue'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useRouter } from 'vue-router'

const spaceStore = useSpaceStore()
const router = useRouter()

onMounted(async () => {
  await spaceStore.fetchSpaces()
})

function goToSpace(spaceId: string) {
  router.push(`/spaces/${spaceId}`)
}
</script>

<template>
  <div class="home-view">
    <header class="app-header">
      <h1 class="logo">索拉 Solra</h1>
      <nav class="nav-links">
        <router-link to="/store">商城</router-link>
        <router-link to="/leaderboard">排行榜</router-link>
        <router-link to="/inbox">消息</router-link>
        <router-link to="/profile">我的</router-link>
      </nav>
    </header>

    <main class="feed-container">
      <h2>发现空间</h2>
      <div v-if="spaceStore.loading" class="loading">加载中...</div>
      <div v-else class="space-grid">
        <div
          v-for="space in spaceStore.spaces"
          :key="space.id"
          class="space-card"
          @click="goToSpace(space.id)"
        >
          <div class="card-thumbnail">
            <img :src="space.thumbnail_url" :alt="space.name" />
          </div>
          <div class="card-info">
            <h3>{{ space.name }}</h3>
            <p>{{ space.description }}</p>
            <div class="card-meta">
              <span>{{ space.author_name }}</span>
              <span>{{ space.visitor_count.toLocaleString() }} 访客</span>
              <span>❤️ {{ space.like_count }}</span>
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
  padding: 16px 32px;
  border-bottom: 1px solid var(--solra-border, #30363d);

  .logo {
    font-size: 24px;
    font-weight: 700;
    color: #58a6ff;
    margin: 0;
  }

  .nav-links {
    display: flex;
    gap: 24px;

    a {
      color: #8b949e;
      text-decoration: none;
      font-size: 14px;

      &:hover {
        color: #58a6ff;
      }
    }
  }
}

.feed-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 32px;

  h2 {
    margin-bottom: 24px;
    font-size: 20px;
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

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
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
    }
  }
}

.loading {
  text-align: center;
  padding: 60px;
  color: #8b949e;
}
</style>
