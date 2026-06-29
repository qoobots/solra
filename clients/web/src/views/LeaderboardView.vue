<template>
  <div class="leaderboard-view">
    <header class="lb-header">
      <h2>排行榜</h2>
      <el-select v-model="boardType" class="lb-select">
        <el-option label="信仰等级" value="FAITH_LEVEL" />
        <el-option label="空间创建数" value="SPACE_COUNT" />
        <el-option label="社交得分" value="SOCIAL_SCORE" />
      </el-select>
    </header>
    <div class="lb-list">
      <div v-for="entry in entries" :key="entry.rank" class="lb-entry" :class="{ 'top-three': entry.rank <= 3 }">
        <span class="rank">#{{ entry.rank }}</span>
        <span class="name">{{ entry.user?.displayName }}</span>
        <span class="score">{{ entry.score }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const boardType = ref('FAITH_LEVEL')

const leaderboards: Record<string, any[]> = {
  FAITH_LEVEL: [
    { rank: 1, user: { displayName: 'Alice' }, score: 99, faithLevel: 99 },
    { rank: 2, user: { displayName: 'Bob' }, score: 87, faithLevel: 87 },
    { rank: 3, user: { displayName: 'Charlie' }, score: 76, faithLevel: 76 },
    { rank: 4, user: { displayName: 'Diana' }, score: 65, faithLevel: 65 },
    { rank: 5, user: { displayName: 'Eve' }, score: 54, faithLevel: 54 },
  ],
  SPACE_COUNT: [
    { rank: 1, user: { displayName: 'Bob' }, score: 128 },
    { rank: 2, user: { displayName: 'Alice' }, score: 95 },
    { rank: 3, user: { displayName: 'Frank' }, score: 72 },
  ],
  SOCIAL_SCORE: [
    { rank: 1, user: { displayName: 'Diana' }, score: 1024 },
    { rank: 2, user: { displayName: 'Charlie' }, score: 888 },
    { rank: 3, user: { displayName: 'Alice' }, score: 756 },
  ],
}

const entries = computed(() => leaderboards[boardType.value] || [])
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
</style>
