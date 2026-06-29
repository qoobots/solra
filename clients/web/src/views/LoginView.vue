<template>
  <div class="login-view">
    <div class="login-card">
      <h1>欢迎回到 Solra</h1>
      <p class="subtitle">登录你的账户以继续探索虚拟空间</p>
      <el-form class="login-form" label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="email" placeholder="your@email.com" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="password" type="password" placeholder="••••••••" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="btn-submit" @click="handleLogin">登录</el-button>
      </el-form>
      <div class="divider"><span>或</span></div>
      <div class="social-login">
        <el-button size="large" class="btn-social">微信登录</el-button>
        <el-button size="large" class="btn-social">Apple 登录</el-button>
      </div>
      <p class="signup-hint">还没有账号？<router-link to="/login">注册</router-link></p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const email = ref('')
const password = ref('')

function handleLogin() {
  console.log('Login:', email.value)
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
.divider {
  display: flex; align-items: center; gap: 16px; margin: 24px 0;
  color: var(--solra-text-secondary); font-size: 13px;
  &::before, &::after { content: ''; flex: 1; height: 1px; background: var(--solra-border); }
}
.social-login { display: flex; gap: 12px; }
.btn-social { flex: 1; }
.signup-hint { text-align: center; margin-top: 24px; font-size: 14px; color: var(--solra-text-secondary); }
</style>
