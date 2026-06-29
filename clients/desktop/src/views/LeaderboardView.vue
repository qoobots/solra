<script setup lang="ts">
import { ref } from 'vue'

interface LeaderEntry {
  rank: number
  spaceId: string
  name: string
  author: string
  score: number
  trend: 'up' | 'down' | 'same'
}

const period = ref<'daily' | 'weekly' | 'monthly'>('weekly')
const entries = ref<LeaderEntry[]>([])
const loading = ref(false)

const periods = [
  { key: 'daily' as const, label: '日榜' },
  { key: 'weekly' as const, label: '周榜' },
  { key: 'monthly' as const, label: '月榜' },
]
</script>

<template>
  <div class="leaderboard-view">
    <header class="page-header">
      <h1>排行榜</h1>
    </header>

    <div class="period-bar">
      <button
        v-for="p in periods"
        :key="p.key"
        :class="['period-btn', { active: period === p.key }]"
        @click="period = p.key"
      >
        {{ p.label }}
      </button>
    </div>

    <div class="board-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="entries.length === 0" class="empty-state">
        <div class="empty-icon">🏆</div>
        <p>排行榜即将上线</p>
        <p class="sub">空间热度排行将在此展示</p>
      </div>
      <div v-else class="rank-list">
        <div
          v-for="entry in entries"
          :key="entry.spaceId"
          class="rank-item"
        >
          <div :class="['rank-number', `rank-${entry.rank}`]">
            {{ entry.rank }}
          </div>
          <div class="rank-info">
            <h3>{{ entry.name }}</h3>
            <span class="author">by {{ entry.author }}</span>
          </div>
          <div class="rank-score">
            <span class="score">{{ entry.score.toLocaleString() }}</span>
            <span :class="['trend', entry.trend]">
              {{ entry.trend === 'up' ? '↑' : entry.trend === 'down' ? '↓' : '–' }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.leaderboard-view {
  min-height: 100vh;
  background: var(--solra-bg-primary, #0d1117);
  color: var(--solra-text-primary, #e6edf3);
}

.page-header {
  padding: 16px 32px;
  border-bottom: 1px solid var(--solra-border, #30363d);

  h1 {
    margin: 0;
    font-size: 20px;
  }
}

.period-bar {
  display: flex;
  gap: 8px;
  padding: 16px 32px;
  border-bottom: 1px solid #21262d;

  .period-btn {
    background: none;
    border: 1px solid #30363d;
    color: #8b949e;
    padding: 6px 20px;
    border-radius: 20px;
    cursor: pointer;
    font-size: 13px;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      background: #1f6feb;
      border-color: #1f6feb;
      color: #fff;
    }
  }
}

.board-content {
  max-width: 700px;
  margin: 0 auto;
  padding: 24px 32px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  p {
    font-size: 16px;
    margin: 0 0 8px;
  }

  .sub {
    font-size: 14px;
    color: #8b949e;
  }
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 10px;

  .rank-number {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 700;
    background: #21262d;
    color: #8b949e;
    flex-shrink: 0;

    &.rank-1 { background: #f0c040; color: #000; }
    &.rank-2 { background: #b0b0b0; color: #000; }
    &.rank-3 { background: #c08040; color: #000; }
  }

  .rank-info {
    flex: 1;

    h3 {
      margin: 0 0 4px;
      font-size: 15px;
    }

    .author {
      font-size: 12px;
      color: #8b949e;
    }
  }

  .rank-score {
    display: flex;
    align-items: center;
    gap: 6px;

    .score {
      font-size: 14px;
      font-weight: 600;
    }

    .trend {
      font-size: 12px;

      &.up { color: #3fb950; }
      &.down { color: #f85149; }
      &.same { color: #8b949e; }
    }
  }
}

.loading {
  text-align: center;
  padding: 60px;
  color: #8b949e;
}
</style>
