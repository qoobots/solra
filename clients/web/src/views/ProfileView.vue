<template>
  <div class="profile-view">
    <div class="profile-card">
      <div class="avatar-placeholder">
        <img v-if="user?.avatarUrl" :src="user.avatarUrl" :alt="user.displayName" />
      </div>
      <h2>{{ user?.displayName || '探索者' }}</h2>
      <p class="bio">{{ user?.bio || '这个人很懒，什么都没写' }}</p>
      <p class="email" v-if="user?.email">{{ user.email }}</p>
      <div class="stats">
        <div class="stat"><strong>{{ user?.faithLevel ?? 0 }}</strong><span>信仰等级</span></div>
        <div class="stat"><strong>{{ user?.spaceCount ?? 0 }}</strong><span>创建空间</span></div>
      </div>
      <el-divider />
      <el-menu :default-active="tab" class="tab-menu" @select="tab = $event as string">
        <el-menu-item index="achievements">成就</el-menu-item>
        <el-menu-item index="spaces">我的空间</el-menu-item>
        <el-menu-item index="settings">设置</el-menu-item>
      </el-menu>
      <div v-if="tab === 'achievements'" class="tab-content">
        <p class="placeholder-text">成就系统将在后续版本中展示</p>
      </div>
      <div v-else-if="tab === 'spaces'" class="tab-content">
        <div v-if="mySpaces.length > 0" class="my-spaces-list">
          <div v-for="s in mySpaces" :key="s.spaceId" class="my-space-item" @click="router.push(`/spaces/${s.spaceId}`)">
            <span class="my-space-title">{{ s.title }}</span>
            <span class="my-space-meta">{{ s.onlineCount }} 在线</span>
          </div>
        </div>
        <p v-else class="placeholder-text">你还没有创建任何空间</p>
        <el-button v-if="mySpaces.length === 0" type="primary" @click="router.push('/create')">创建第一个空间</el-button>
      </div>
      <div v-else-if="tab === 'settings'" class="tab-content">
        <p class="placeholder-text">设置功能开发中</p>
        <el-button type="danger" plain @click="handleLogout" style="margin-top:16px">退出登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useSpaceStore, type SpaceItem } from '@/stores/useSpaceStore'
import { storeToRefs } from 'pinia'

const router = useRouter()
const authStore = useAuthStore()
const spaceStore = useSpaceStore()
const { user } = storeToRefs(authStore)

const tab = ref('achievements')
const mySpaces = ref<SpaceItem[]>([])

onMounted(async () => {
  await authStore.fetchProfile()
  // 获取用户创建的空间（通过推荐接口获取，后续可替换为专门的用户空间接口）
  try {
    const items = await spaceStore.fetchRecommendations()
    mySpaces.value = items.filter(
      s => s.creator?.userId === user.value?.userId
    )
  } catch {
    // 静默处理
  }
})

function handleLogout() {
  authStore.logout()
  router.push('/')
}
</script>

<style lang="scss" scoped>
.profile-view {
  display: flex; justify-content: center; padding: 40px 24px; min-height: 100vh;
}
.profile-card {
  width: 100%; max-width: 480px;
  background: var(--solra-bg-card);
  border: 1px solid var(--solra-border);
  border-radius: 20px;
  padding: 40px;
  text-align: center;
  h2 { margin-top: 16px; margin-bottom: 8px; }
  .bio { color: var(--solra-text-secondary); font-size: 14px; }
  .email { color: var(--solra-text-secondary); font-size: 13px; margin-top: 4px; }
}
.avatar-placeholder {
  width: 96px; height: 96px;
  background: linear-gradient(135deg, var(--solra-accent), #a29bfe);
  border-radius: 50%;
  margin: 0 auto;
  overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  img { width: 100%; height: 100%; object-fit: cover; }
}
.stats {
  display: flex; justify-content: center; gap: 40px; margin-top: 24px;
  .stat { display: flex; flex-direction: column; }
  .stat strong { font-size: 24px; color: var(--solra-accent); }
  .stat span { font-size: 12px; color: var(--solra-text-secondary); }
}
.tab-menu { border: none; background: transparent; }
.tab-content { padding: 20px 0; min-height: 120px; }
.placeholder-text { color: var(--solra-text-secondary); font-size: 14px; }

.my-spaces-list {
  display: flex; flex-direction: column; gap: 8px; text-align: left;
  .my-space-item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 10px 14px;
    background: var(--solra-bg-secondary);
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.2s;
    &:hover { background: var(--solra-border); }
  }
  .my-space-title { font-size: 14px; }
  .my-space-meta { font-size: 12px; color: var(--solra-text-secondary); }
}
</style>
