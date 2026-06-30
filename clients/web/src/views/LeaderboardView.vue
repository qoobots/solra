<template>
  <div class="leaderboard-view">
    <header class="lb-header">
      <h2>排行榜</h2>
      <el-select v-model="boardType" class="lb-select" placeholder="选择榜单">
        <el-option v-for="opt in boardOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </header>

    <div v-if="loading" class="loading-state">
      <p>正在加载排行榜...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <el-button @click="leaderboardStore.fetchLeaderboard()">重试</el-button>
    </div>

    <div v-else-if="currentEntries.length === 0" class="empty-state">
      <p>暂无排行数据</p>
    </div>

    <div v-else class="lb-list">
      <div v-for="entry in currentEntries" :key="entry.rank" class="lb-entry" :class="{ 'top-three': entry.rank <= 3 }">
        <span class="rank">{{ rankIcon(entry.rank) }}</span>
        <span class="name">{{ entry.user?.displayName }}</span>
        <span class="score">{{ entry.score }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useLeaderboardStore, type LeaderboardType } from '@/stores/useLeaderboardStore'
import { storeToRefs } from 'pinia'

const leaderboardStore = useLeaderboardStore()
const { boardType, currentEntries, loading, error } = storeToRefs(leaderboardStore)

const boardOptions = [
  { label: '信仰等级', value: 'FAITH_LEVEL' as LeaderboardType },
  { label: '空间创建数', value: 'SPACE_COUNT' as LeaderboardType },
  { label: '社交得分', value: 'SOCIAL_SCORE' as LeaderboardType },
]

onMounted(() => {
  leaderboardStore.fetchLeaderboard()
})

watch(boardType, (newType) => {
  leaderboardStore.fetchLeaderboard(newType)
})

function rankIcon(rank: number): string {
  if (rank === 1) return '🥇'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return `#${rank}`
}
</script>

<style lang="scss" scoped>
.leaderboard-view { max-width: 640px; margin: 0 auto; padding: 24px; min-height: 100vh; }
.lb-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.lb-select { width: 180px; }

.lb-entry {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px;
  background: var(--solra-bg-card);
  border: 1px solid var(--solra-border);
  border-radius: 12px;
  margin-bottom: 8px;
  &.top-three { border-color: var(--solra-accent); }
  .rank { font-weight: 700; font-size: 18px; color: var(--solra-accent); width: 40px; }
  .name { flex: 1; font-size: 16px; }
  .score { font-weight: 600; color: var(--solra-accent); }
}

.loading-state, .error-state, .empty-state {
  text-align: center; padding: 60px 20px; color: var(--solra-text-secondary); font-size: 16px;
}
</style>
