<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const profile = authStore.userProfile
const displayName = ref(profile?.displayName || '')
const editing = ref(false)

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function toggleEdit() {
  editing.value = !editing.value
}

function saveProfile() {
  // TODO: 调用 API 保存个人资料
  editing.value = false
}
</script>

<template>
  <div class="profile-view">
    <header class="page-header">
      <h1>我的</h1>
    </header>

    <div class="profile-content">
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="avatar-section">
          <div class="avatar-placeholder">
            {{ profile?.displayName?.charAt(0) || 'U' }}
          </div>
          <div class="user-details">
            <h2 v-if="!editing">{{ profile?.displayName || '未登录' }}</h2>
            <input
              v-else
              v-model="displayName"
              class="inline-input"
              placeholder="输入昵称"
            />
            <p class="username">@{{ profile?.username || 'guest' }}</p>
            <span class="tier-badge">{{ profile?.subscriptionTier || 'Free' }}</span>
          </div>
          <div class="edit-actions">
            <button v-if="!editing" class="action-btn" @click="toggleEdit">编辑</button>
            <template v-else>
              <button class="action-btn primary" @click="saveProfile">保存</button>
              <button class="action-btn" @click="toggleEdit">取消</button>
            </template>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-value">0</span>
          <span class="stat-label">创建的空间</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">0</span>
          <span class="stat-label">收藏</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">0</span>
          <span class="stat-label">关注</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">0</span>
          <span class="stat-label">粉丝</span>
        </div>
      </div>

      <!-- 功能菜单 -->
      <div class="menu-list">
        <div class="menu-item" v-for="item in menuItems" :key="item.label">
          <span>{{ item.icon }} {{ item.label }}</span>
          <span class="arrow">›</span>
        </div>
      </div>

      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.profile-view {
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

.profile-content {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px 16px;
}

.profile-card {
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 16px;

  .avatar-section {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .avatar-placeholder {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, #58a6ff, #1f6feb);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    font-weight: 700;
    color: #fff;
    flex-shrink: 0;
  }

  .user-details {
    flex: 1;

    h2 {
      margin: 0 0 4px;
      font-size: 18px;
    }

    .username {
      margin: 0 0 6px;
      font-size: 13px;
      color: #8b949e;
    }

    .tier-badge {
      font-size: 11px;
      background: rgba(88, 166, 255, 0.15);
      color: #58a6ff;
      padding: 2px 8px;
      border-radius: 10px;
    }

    .inline-input {
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 4px 8px;
      color: #e6edf3;
      font-size: 16px;
      width: 160px;
      outline: none;

      &:focus {
        border-color: #58a6ff;
      }
    }
  }

  .edit-actions {
    display: flex;
    gap: 8px;

    .action-btn {
      background: #21262d;
      border: 1px solid #30363d;
      color: #c9d1d9;
      padding: 6px 14px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;

      &:hover {
        background: #30363d;
      }

      &.primary {
        background: #238636;
        border-color: #238636;
        color: #fff;

        &:hover {
          background: #2ea043;
        }
      }
    }
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .stat-item {
    background: var(--solra-bg-secondary, #161b22);
    border: 1px solid var(--solra-border, #30363d);
    border-radius: 8px;
    padding: 12px;
    text-align: center;

    .stat-value {
      display: block;
      font-size: 20px;
      font-weight: 700;
    }

    .stat-label {
      display: block;
      font-size: 12px;
      color: #8b949e;
      margin-top: 4px;
    }
  }
}

.menu-list {
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 16px;

  .menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 14px 16px;
    cursor: pointer;
    font-size: 14px;
    border-bottom: 1px solid #21262d;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: rgba(88, 166, 255, 0.05);
    }

    .arrow {
      color: #484f58;
      font-size: 18px;
    }
  }
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: transparent;
  border: 1px solid #30363d;
  border-radius: 8px;
  color: #f85149;
  font-size: 14px;
  cursor: pointer;

  &:hover {
    background: rgba(248, 81, 73, 0.1);
  }
}
</style>

<script lang="ts">
const menuItems = [
  { icon: '👤', label: '账号与安全' },
  { icon: '🎨', label: '外观设置' },
  { icon: '🔔', label: '通知设置' },
  { icon: '💾', label: '缓存管理' },
  { icon: '❓', label: '帮助与反馈' },
  { icon: 'ℹ️', label: '关于索拉' },
]
</script>
