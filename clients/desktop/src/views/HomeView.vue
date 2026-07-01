<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useSpaceStore } from '@/stores/useSpaceStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'
import { invoke } from '@tauri-apps/api/core'

const spaceStore = useSpaceStore()
const authStore = useAuthStore()
const router = useRouter()
const searchQuery = ref('')
const activeCategory = ref('全部')
const showCreateDialog = ref(false)
const newSpace = ref({ name: '', description: '', category: '休闲', isPublic: true })
const creating = ref(false)

const categories = ['全部', '休闲', '社交', '教育', '音乐', '游戏', '艺术', '自然', '科幻']

const filteredSpaces = computed(() => {
  let list = spaceStore.spaces
  if (activeCategory.value !== '全部') {
    list = list.filter(s => s.category === activeCategory.value)
  }
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.trim().toLowerCase()
    list = list.filter(s =>
      s.title.toLowerCase().includes(q) ||
      s.description.toLowerCase().includes(q) ||
      s.tags.some(t => t.toLowerCase().includes(q))
    )
  }
  return list
})

const featuredSpaces = computed(() =>
  spaceStore.spaces.filter(s => s.onlineCount > 5000)
)

onMounted(async () => {
  await spaceStore.fetchSpaces()
  if (authStore.isAuthenticated) {
    await authStore.fetchProfile()
  }
  try {
    const sysInfo = await invoke<any>('get_system_info')
    console.log('System info:', sysInfo)
  } catch { /* silent */ }
})

function goToSpace(spaceId: string) {
  router.push(`/spaces/${spaceId}`)
}

async function handleCreateSpace() {
  if (!newSpace.value.name.trim()) return
  creating.value = true
  try {
    const result = await invoke<any>('create_space', {
      request: {
        name: newSpace.value.name.trim(),
        description: newSpace.value.description.trim(),
        category: newSpace.value.category,
        is_public: newSpace.value.isPublic,
      },
    })
    showCreateDialog.value = false
    newSpace.value = { name: '', description: '', category: '休闲', isPublic: true }
    await spaceStore.fetchSpaces()
    if (result?.id) {
      router.push(`/spaces/${result.id}`)
    }
  } catch (e) {
    console.error('创建空间失败:', e)
  } finally {
    creating.value = false
  }
}

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
</script>

<template>
  <div class="home-view">
    <!-- 顶部导航 -->
    <header class="app-header">
      <h1 class="logo" @click="router.push('/')">索拉 Solra</h1>
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
      <!-- 精选推荐横幅（热门空间） -->
      <section v-if="featuredSpaces.length > 0" class="featured-section">
        <h2>🔥 热门推荐</h2>
        <div class="featured-grid">
          <div
            v-for="space in featuredSpaces.slice(0, 4)"
            :key="space.spaceId"
            class="featured-card"
            @click="goToSpace(space.spaceId)"
          >
            <div class="featured-thumb">
              <img v-if="space.thumbnailUrl" :src="space.thumbnailUrl" :alt="space.title" />
              <div v-else class="thumb-placeholder">🌌</div>
              <div class="featured-overlay">
                <span class="online-badge">🟢 {{ formatCount(space.onlineCount) }} 在线</span>
              </div>
            </div>
            <div class="featured-info">
              <h3>{{ space.title }}</h3>
              <p class="creator">by {{ space.creatorName }}</p>
              <div class="featured-tags" v-if="space.tags.length">
                <span v-for="tag in space.tags.slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 分类筛选 -->
      <div class="category-bar">
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-btn"
          :class="{ active: activeCategory === cat }"
          @click="activeCategory = cat"
        >
          {{ cat }}
        </button>
      </div>

      <!-- 空间列表头部 -->
      <div class="feed-header">
        <h2>{{ activeCategory === '全部' ? '发现空间' : activeCategory }}</h2>
        <div class="header-right">
          <span class="result-count" v-if="filteredSpaces.length">
            {{ filteredSpaces.length }} 个空间
          </span>
          <button class="btn-create" @click="showCreateDialog = true">+ 创建空间</button>
        </div>
      </div>

      <!-- 加载骨架屏 -->
      <div v-if="spaceStore.loading && spaceStore.spaces.length === 0" class="skeleton-grid">
        <div v-for="n in 6" :key="n" class="skeleton-card">
          <div class="skeleton-thumb" />
          <div class="skeleton-body">
            <div class="skeleton-line w-60" />
            <div class="skeleton-line w-90" />
            <div class="skeleton-line w-40" />
          </div>
        </div>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="spaceStore.error && spaceStore.spaces.length === 0" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ spaceStore.error }}</p>
        <el-button type="primary" @click="spaceStore.fetchSpaces()">重试</el-button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredSpaces.length === 0 && !spaceStore.loading" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p v-if="searchQuery">没有找到匹配 "{{ searchQuery }}" 的空间</p>
        <p v-else>该分类暂无空间，快去创建第一个吧！</p>
        <el-button v-if="searchQuery" @click="searchQuery = ''">清除搜索</el-button>
        <el-button v-else type="primary" @click="showCreateDialog = true">创建空间</el-button>
      </div>

      <!-- 空间卡片网格 -->
      <div v-else class="space-grid">
        <div
          v-for="space in filteredSpaces"
          :key="space.spaceId"
          class="space-card"
          @click="goToSpace(space.spaceId)"
        >
          <div class="card-thumbnail">
            <img v-if="space.thumbnailUrl" :src="space.thumbnailUrl" :alt="space.title" />
            <div v-else class="thumb-placeholder">
              <span>{{ space.category === '音乐' ? '🎵' : space.category === '游戏' ? '🎮' : '🌌' }}</span>
            </div>
            <div class="card-badge" v-if="space.onlineCount > 1000">
              🔥 热门
            </div>
          </div>
          <div class="card-info">
            <h3>{{ space.title }}</h3>
            <p>{{ space.description }}</p>
            <div class="card-meta">
              <span class="creator-name">{{ space.creatorName }}</span>
              <span class="online-indicator">🟢 {{ formatCount(space.onlineCount) }} 在线</span>
              <span v-if="space.likeCount" class="like-count">❤️ {{ space.likeCount }}</span>
            </div>
            <div class="card-tags" v-if="space.tags.length">
              <span v-for="tag in space.tags.slice(0, 3)" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 创建空间对话框 -->
    <el-dialog v-model="showCreateDialog" title="创建新空间" width="480px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="空间名称" required>
          <el-input v-model="newSpace.name" placeholder="给你的空间取个名字" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="newSpace.description"
            type="textarea"
            placeholder="简单描述一下你的空间..."
            :rows="3"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newSpace.category" style="width: 100%">
            <el-option v-for="cat in categories.filter(c => c !== '全部')" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="newSpace.isPublic">
            <el-radio :value="true">公开 — 所有人可见</el-radio>
            <el-radio :value="false">私有 — 仅受邀可见</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" :disabled="!newSpace.name.trim()" @click="handleCreateSpace">
          创建
        </el-button>
      </template>
    </el-dialog>
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
  position: sticky;
  top: 0;
  z-index: 50;
  background: var(--solra-bg-primary, #0d1117);
  backdrop-filter: blur(12px);

  .logo {
    font-size: 22px;
    font-weight: 700;
    color: #58a6ff;
    margin: 0;
    white-space: nowrap;
    cursor: pointer;
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
  padding: 24px 32px 64px;
}

/* 精选推荐 */
.featured-section {
  margin-bottom: 32px;
  h2 { font-size: 18px; margin: 0 0 16px; }
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.featured-card {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
  &:hover { transform: translateY(-4px); }

  .featured-thumb {
    width: 100%;
    height: 180px;
    background: linear-gradient(135deg, #1a1a2e, #16213e);
    img { width: 100%; height: 100%; object-fit: cover; }
    .thumb-placeholder {
      width: 100%; height: 100%;
      display: flex; align-items: center; justify-content: center;
      font-size: 40px;
    }
    .featured-overlay {
      position: absolute; bottom: 0; left: 0; right: 0;
      padding: 8px 12px;
      background: linear-gradient(transparent, rgba(0,0,0,0.7));
      .online-badge { font-size: 12px; color: #3fb950; }
    }
  }

  .featured-info {
    padding: 10px 12px;
    background: var(--solra-bg-secondary, #161b22);
    h3 { margin: 0 0 4px; font-size: 14px; }
    .creator { margin: 0 0 6px; font-size: 12px; color: #8b949e; }
    .featured-tags { display: flex; gap: 4px; .tag { font-size: 10px; color: #58a6ff; background: rgba(88,166,255,0.1); padding: 1px 6px; border-radius: 8px; } }
  }
}

/* 分类筛选 */
.category-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-btn {
  padding: 6px 16px;
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 20px;
  background: transparent;
  color: #8b949e;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover { border-color: #58a6ff; color: #58a6ff; }
  &.active {
    background: #58a6ff;
    color: #fff;
    border-color: #58a6ff;
  }
}

.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 { font-size: 20px; margin: 0; }

  .header-right {
    display: flex; align-items: center; gap: 16px;
    .result-count { font-size: 13px; color: #8b949e; }
  }

  .btn-create {
    padding: 8px 20px;
    background: #58a6ff;
    color: #fff;
    border-radius: 8px;
    border: none;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    &:hover { background: #79b8ff; }
  }
}

/* 骨架屏 */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: var(--solra-bg-secondary, #161b22);
  border-radius: 12px;
  overflow: hidden;
  .skeleton-thumb { height: 160px; background: #21262d; animation: pulse 1.5s infinite; }
  .skeleton-body { padding: 16px; display: flex; flex-direction: column; gap: 10px; }
  .skeleton-line { height: 14px; background: #21262d; border-radius: 6px; animation: pulse 1.5s infinite; }
  .w-60 { width: 60%; }
  .w-90 { width: 90%; }
  .w-40 { width: 40%; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 空状态 & 错误状态 */
.empty-state, .error-state {
  text-align: center;
  padding: 80px 20px;
  .empty-icon, .error-icon { font-size: 48px; margin-bottom: 16px; }
  p { color: #8b949e; margin: 0 0 20px; font-size: 15px; }
}

.error-state p { color: #ff7675; }

/* 空间卡片网格 */
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
    position: relative;
    width: 100%;
    height: 160px;
    overflow: hidden;
    background: linear-gradient(135deg, #1a1a2e, #16213e);

    img { width: 100%; height: 100%; object-fit: cover; }

    .thumb-placeholder {
      width: 100%; height: 100%;
      display: flex; align-items: center; justify-content: center;
      font-size: 48px;
    }

    .card-badge {
      position: absolute; top: 8px; right: 8px;
      background: rgba(255,100,50,0.9);
      color: #fff;
      font-size: 11px;
      padding: 2px 8px;
      border-radius: 10px;
    }
  }

  .card-info {
    padding: 16px;

    h3 { margin: 0 0 8px; font-size: 16px; }
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
      flex-wrap: wrap;
      .creator-name { color: #8b949e; }
      .online-indicator { color: #3fb950; }
      .like-count { color: #ff7675; }
    }

    .card-tags {
      display: flex; gap: 6px; flex-wrap: wrap;
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

.loading { text-align: center; padding: 60px; color: #8b949e; }
</style>
