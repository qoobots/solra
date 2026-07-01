<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/useAuthStore'
import { useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'
import { invoke } from '@tauri-apps/api/core'
import api from '@/api'

const authStore = useAuthStore()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()

const profile = authStore.user
const displayName = ref(profile?.displayName || '')
const editing = ref(false)
const saving = ref(false)

// Bio editing
const bio = ref(profile?.bio || '')
const editingBio = ref(false)

// Format join date
const joinDate = computed(() => {
  if (!profile?.createdAt) return ''
  const d = new Date(profile.createdAt)
  return `${d.getFullYear()}年${d.getMonth() + 1}月加入`
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function goToSettings() {
  router.push('/settings')
}

function toggleEdit() {
  editing.value = !editing.value
  if (editing.value) {
    displayName.value = profile?.displayName || ''
  }
}

async function saveProfile() {
  if (!displayName.value.trim()) return
  saving.value = true

  try {
    const res = await api.post('/api/auth/v1/profile', {
      displayName: displayName.value.trim(),
    }) as any

    if (authStore.user) {
      authStore.user.displayName = res.displayName || displayName.value.trim()
    }
  } catch {
    try {
      await invoke('update_profile', {
        request: { display_name: displayName.value.trim() },
      })
      if (authStore.user) {
        authStore.user.displayName = displayName.value.trim()
      }
    } catch (e: any) {
      console.error('保存失败:', e)
    }
  } finally {
    saving.value = false
    editing.value = false
  }
}

async function saveBio() {
  saving.value = true
  try {
    await api.post('/api/auth/v1/profile', { bio: bio.value }) as any
    if (authStore.user) authStore.user.bio = bio.value
  } catch {
    // silent
  } finally {
    saving.value = false
    editingBio.value = false
  }
}
</script>

<template>
  <div class="profile-view">
    <header class="page-header">
      <h1>我的</h1>
      <div class="header-actions">
        <button class="icon-btn" @click="goToSettings" title="设置">
          ⚙️
        </button>
      </div>
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
            <p class="join-date" v-if="joinDate">{{ joinDate }}</p>
          </div>
          <div class="edit-actions">
            <button v-if="!editing" class="action-btn" @click="toggleEdit">编辑</button>
            <template v-else>
              <button class="action-btn primary" :disabled="saving" @click="saveProfile">
                {{ saving ? '保存中...' : '保存' }}
              </button>
              <button class="action-btn" @click="toggleEdit">取消</button>
            </template>
          </div>
        </div>

        <!-- Bio -->
        <div class="bio-section">
          <p v-if="!editingBio" class="bio-text">
            {{ profile?.bio || '这个人很懒，什么都没写...' }}
          </p>
          <textarea
            v-else
            v-model="bio"
            class="bio-input"
            placeholder="写一段自我介绍..."
            maxlength="200"
            rows="3"
          />
          <div class="bio-actions">
            <span class="bio-hint" v-if="editingBio">{{ bio.length }}/200</span>
            <button
              v-if="!editingBio"
              class="link-btn"
              @click="editingBio = true; bio = profile?.bio || ''"
            >编辑简介</button>
            <template v-else>
              <button class="link-btn" @click="saveBio">保存</button>
              <button class="link-btn cancel" @click="editingBio = false">取消</button>
            </template>
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-value">{{ profile?.spaceCount || 0 }}</span>
          <span class="stat-label">创建的空间</span>
        </div>
        <div class="stat-item">
          <span class="stat-value">{{ profile?.faithLevel || 0 }}</span>
          <span class="stat-label">信仰值</span>
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
        <div
          v-for="item in menuItems"
          :key="item.label"
          class="menu-item"
          @click="item.action ? item.action() : undefined"
        >
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
  background: var(--solra-bg-primary);
  color: var(--solra-text-primary);
}

.page-header {
  padding: 16px 32px;
  border-bottom: 1px solid var(--solra-border);
  display: flex;
  align-items: center;
  justify-content: space-between;

  h1 {
    margin: 0;
    font-size: 20px;
  }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  background: none;
  border: 1px solid var(--solra-border);
  border-radius: 8px;
  color: var(--solra-text-secondary);
  font-size: 18px;
  padding: 6px 10px;
  cursor: pointer;

  &:hover {
    background: var(--solra-bg-tertiary);
    color: var(--solra-text-primary);
  }
}

.profile-content {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px 16px;
}

.profile-card {
  background: var(--solra-bg-secondary);
  border: 1px solid var(--solra-border);
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
      color: var(--solra-text-secondary);
    }

    .tier-badge {
      font-size: 11px;
      background: rgba(88, 166, 255, 0.15);
      color: #58a6ff;
      padding: 2px 8px;
      border-radius: 10px;
    }

    .join-date {
      margin: 6px 0 0;
      font-size: 12px;
      color: var(--solra-text-tertiary);
    }

    .inline-input {
      background: var(--solra-bg-primary);
      border: 1px solid var(--solra-border);
      border-radius: 6px;
      padding: 4px 8px;
      color: var(--solra-text-primary);
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
      background: var(--solra-bg-tertiary);
      border: 1px solid var(--solra-border);
      color: var(--solra-text-primary);
      padding: 6px 14px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;

      &:hover {
        background: var(--solra-border);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
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

.bio-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--solra-border-light);
}

.bio-text {
  margin: 0;
  font-size: 13px;
  color: var(--solra-text-secondary);
  line-height: 1.5;
}

.bio-input {
  width: 100%;
  background: var(--solra-bg-primary);
  border: 1px solid var(--solra-border);
  border-radius: 6px;
  padding: 8px 10px;
  color: var(--solra-text-primary);
  font-size: 13px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;

  &:focus {
    border-color: #58a6ff;
  }
}

.bio-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 6px;
}

.bio-hint {
  font-size: 11px;
  color: var(--solra-text-tertiary);
}

.link-btn {
  background: none;
  border: none;
  color: var(--solra-brand);
  font-size: 12px;
  cursor: pointer;
  padding: 2px 4px;

  &:hover {
    text-decoration: underline;
  }

  &.cancel {
    color: var(--solra-text-tertiary);
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  .stat-item {
    background: var(--solra-bg-secondary);
    border: 1px solid var(--solra-border);
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
      color: var(--solra-text-secondary);
      margin-top: 4px;
    }
  }
}

.menu-list {
  background: var(--solra-bg-secondary);
  border: 1px solid var(--solra-border);
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
    border-bottom: 1px solid var(--solra-border-light);

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: rgba(88, 166, 255, 0.05);
    }

    .arrow {
      color: var(--solra-text-tertiary);
      font-size: 18px;
    }
  }
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: transparent;
  border: 1px solid var(--solra-border);
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
function goToSettings() {
  const router = (window as any).__vue_app__?.config?.globalProperties?.$router
  if (router) router.push('/settings')
}

const menuItems = [
  { icon: '👤', label: '账号与安全', action: undefined },
  { icon: '🎨', label: '外观设置', action: undefined },
  { icon: '🔔', label: '通知设置', action: undefined },
  { icon: '💾', label: '缓存管理', action: undefined },
  { icon: '❓', label: '帮助与反馈', action: undefined },
  { icon: 'ℹ️', label: '关于索拉', action: undefined },
]
</script>
