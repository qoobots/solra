<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { invoke } from '@tauri-apps/api/core'
import api from '@/api'

const router = useRouter()

const spaceName = ref('')
const description = ref('')
const category = ref('social')
const isPublic = ref(true)
const loading = ref(false)
const errorMsg = ref('')

const categories = [
  { value: 'social', label: '社交' },
  { value: 'game', label: '游戏' },
  { value: 'exhibition', label: '展览' },
  { value: 'education', label: '教育' },
  { value: 'music', label: '音乐' },
]

async function handleCreate() {
  if (!spaceName.value.trim()) return
  loading.value = true
  errorMsg.value = ''

  try {
    // 优先使用 HTTP API 创建空间
    const res = await api.post('/api/spc/v1/spaces', {
      title: spaceName.value.trim(),
      description: description.value.trim(),
      category: category.value,
      visibility: isPublic.value ? 'PUBLIC' : 'PRIVATE',
    }) as any

    const spaceId = res.spaceId || res.id || ''
    if (spaceId) {
      router.push(`/spaces/${spaceId}`)
    } else {
      router.push('/')
    }
  } catch {
    // HTTP API 不可用时回退到 Tauri IPC
    try {
      const res = await invoke<{ id: string; name: string }>('create_space', {
        request: {
          name: spaceName.value.trim(),
          description: description.value.trim(),
          category: category.value,
          is_public: isPublic.value,
        },
      })
      if (res.id) {
        router.push(`/spaces/${res.id}`)
      } else {
        router.push('/')
      }
    } catch (e: any) {
      errorMsg.value = typeof e === 'string' ? e : (e?.message || '创建失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="create-space-view">
    <header class="page-header">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h1>创建新空间</h1>
    </header>

    <div class="form-container">
      <el-form label-position="top" @submit.prevent="handleCreate">
        <el-form-item label="空间名称">
          <el-input v-model="spaceName" placeholder="给你的空间起个名字" size="large" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="空间描述">
          <el-input
            v-model="description"
            type="textarea"
            placeholder="描述一下你的空间..."
            :rows="4"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="category" size="large" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="可见性">
          <el-switch v-model="isPublic" active-text="公开" inactive-text="私有" />
        </el-form-item>

        <el-form-item v-if="errorMsg">
          <div class="error-msg">{{ errorMsg }}</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width: 100%">
            创建空间
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.create-space-view {
  min-height: 100vh;
  background: var(--solra-bg-primary, #0d1117);
  color: var(--solra-text-primary, #e6edf3);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 32px;
  border-bottom: 1px solid var(--solra-border, #30363d);

  .back-btn {
    background: none;
    border: none;
    color: #58a6ff;
    cursor: pointer;
    font-size: 14px;

    &:hover {
      color: #79c0ff;
    }
  }

  h1 {
    margin: 0;
    font-size: 20px;
  }
}

.form-container {
  max-width: 600px;
  margin: 32px auto;
  padding: 32px;
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;
}

.error-msg {
  color: #f85149;
  font-size: 13px;
  padding: 8px 12px;
  background: rgba(248, 81, 73, 0.1);
  border: 1px solid rgba(248, 81, 73, 0.3);
  border-radius: 6px;
  width: 100%;
}
</style>
