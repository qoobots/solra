<template>
  <div class="profile-view">
    <div class="profile-card">
      <div class="avatar-placeholder"></div>
      <h2>{{ user.displayName }}</h2>
      <p class="bio">{{ user.bio }}</p>
      <div class="stats">
        <div class="stat"><strong>{{ user.faithLevel }}</strong><span>信仰等级</span></div>
        <div class="stat"><strong>{{ user.spaceCount }}</strong><span>创建空间</span></div>
      </div>
      <el-divider />
      <el-menu :default-active="tab" class="tab-menu" @select="tab = $event as string">
        <el-menu-item index="achievements">成就</el-menu-item>
        <el-menu-item index="spaces">我的空间</el-menu-item>
        <el-menu-item index="settings">设置</el-menu-item>
      </el-menu>
      <div v-if="tab === 'achievements'" class="tab-content">
        <p class="placeholder-text">成就将在H3原型中展示</p>
      </div>
      <div v-else class="tab-content">
        <p class="placeholder-text">此功能开发中</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const tab = ref('achievements')
const user = ref({
  displayName: '探索者',
  bio: '这个人很懒，什么都没写',
  faithLevel: 12,
  spaceCount: 0,
})
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
}
.avatar-placeholder {
  width: 96px; height: 96px;
  background: linear-gradient(135deg, var(--solra-accent), #a29bfe);
  border-radius: 50%;
  margin: 0 auto;
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
</style>
