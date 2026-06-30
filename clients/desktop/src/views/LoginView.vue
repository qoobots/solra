<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isRegister = ref(false)
const username = ref('')
const password = ref('')
const displayName = ref('')
const errorMsg = ref('')

async function handleSubmit() {
  errorMsg.value = ''
  if (!username.value || !password.value) {
    errorMsg.value = '请填写邮箱和密码'
    return
  }
  if (isRegister.value && !displayName.value) {
    errorMsg.value = '请填写用户名'
    return
  }

  let success: boolean
  if (isRegister.value) {
    success = await authStore.register({
      email: username.value,
      password: password.value,
      displayName: displayName.value,
    })
  } else {
    success = await authStore.login(username.value, password.value)
  }

  if (success) {
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } else {
    errorMsg.value = authStore.error || '操作失败，请重试'
  }
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <h1>{{ isRegister ? '创建索拉账户' : '登录索拉' }}</h1>
      <p class="subtitle">{{ isRegister ? '开始探索无限虚拟空间' : '进入你的 3D 虚拟世界' }}</p>

      <el-form @submit.prevent="handleSubmit" label-position="top">
        <el-form-item v-if="isRegister" label="用户名">
          <el-input v-model="displayName" placeholder="你的显示名称" size="large" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="username" placeholder="your@email.com" size="large" type="email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="••••••••" size="large" show-password />
        </el-form-item>
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="authStore.loading"
            native-type="submit"
            style="width: 100%"
          >
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p class="toggle-link">
        {{ isRegister ? '已有账号？' : '还没有账号？' }}
        <a href="#" @click.prevent="isRegister = !isRegister; errorMsg = ''">
          {{ isRegister ? '立即登录' : '立即注册' }}
        </a>
      </p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-view {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--solra-bg-primary, #0d1117);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;

  h1 {
    text-align: center;
    margin-bottom: 8px;
    color: #58a6ff;
    font-size: 24px;
  }

  .subtitle {
    text-align: center;
    color: #8b949e;
    font-size: 14px;
    margin-bottom: 28px;
  }

  .error-msg {
    color: #ff7675;
    font-size: 13px;
    margin-bottom: 12px;
    text-align: center;
  }

  .toggle-link {
    text-align: center;
    font-size: 14px;
    color: #8b949e;
    margin-top: 16px;

    a {
      color: #58a6ff;
      text-decoration: none;
      &:hover { text-decoration: underline; }
    }
  }
}
</style>
