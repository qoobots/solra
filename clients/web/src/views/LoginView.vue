<template>
  <div class="login-view">
    <div class="login-card">
      <h1>{{ isRegister ? '创建 Solra 账户' : '欢迎回到 Solra' }}</h1>
      <p class="subtitle">{{ isRegister ? '开始探索无限虚拟空间' : '登录你的账户以继续探索虚拟空间' }}</p>

      <el-form class="login-form" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item v-if="isRegister" label="用户名">
          <el-input v-model="displayName" placeholder="你的显示名称" size="large" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="email" placeholder="your@email.com" size="large" type="email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="••••••••" size="large" show-password />
        </el-form-item>
        <p v-if="error" class="error-msg">{{ error }}</p>
        <el-button
          type="primary"
          size="large"
          class="btn-submit"
          :loading="authStore.loading"
          @click="handleSubmit"
        >
          {{ isRegister ? '注册' : '登录' }}
        </el-button>
      </el-form>

      <div class="divider"><span>或</span></div>
      <div class="social-login">
        <el-button size="large" class="btn-social" @click="handleOAuth('wechat')">微信登录</el-button>
        <el-button size="large" class="btn-social" @click="handleOAuth('apple')">Apple 登录</el-button>
      </div>
      <p class="signup-hint">
        {{ isRegister ? '已有账号？' : '还没有账号？' }}
        <a href="#" @click.prevent="isRegister = !isRegister; error = ''">
          {{ isRegister ? '登录' : '注册' }}
        </a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isRegister = ref(false)
const email = ref('')
const password = ref('')
const displayName = ref('')
const error = ref('')

async function handleSubmit() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = '请填写邮箱和密码'
    return
  }
  if (isRegister.value && !displayName.value) {
    error.value = '请填写用户名'
    return
  }

  let success: boolean
  if (isRegister.value) {
    success = await authStore.register({
      email: email.value,
      password: password.value,
      displayName: displayName.value,
    })
  } else {
    success = await authStore.login(email.value, password.value)
  }

  if (success) {
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } else {
    error.value = authStore.error || '操作失败，请重试'
  }
}

async function handleOAuth(provider: string) {
  error.value = 'OAuth 登录需要客户端支持，请使用邮箱登录'
}
</script>

<style lang="scss" scoped>
.login-view {
  display: flex; align-items: center; justify-content: center;
  min-height: 100vh; padding: 24px;
}

.login-card {
  width: 100%; max-width: 400px;
  background: var(--solra-bg-card);
  border: 1px solid var(--solra-border);
  border-radius: 20px;
  padding: 40px;

  h1 { font-size: 28px; text-align: center; margin-bottom: 8px; }
  .subtitle { color: var(--solra-text-secondary); text-align: center; margin-bottom: 32px; font-size: 14px; }
}

.login-form { margin-bottom: 24px; }
.btn-submit { width: 100%; }
.error-msg {
  color: var(--solra-danger);
  font-size: 13px;
  margin-bottom: 12px;
  text-align: center;
}
.divider {
  display: flex; align-items: center; gap: 16px; margin: 24px 0;
  color: var(--solra-text-secondary); font-size: 13px;
  &::before, &::after { content: ''; flex: 1; height: 1px; background: var(--solra-border); }
}
.social-login { display: flex; gap: 12px; }
.btn-social { flex: 1; }
.signup-hint { text-align: center; margin-top: 24px; font-size: 14px; color: var(--solra-text-secondary); }
</style>
