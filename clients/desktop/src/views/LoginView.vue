<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  loading.value = true
  // TODO: 调用认证 API
  setTimeout(() => {
    loading.value = false
    router.push('/')
  }, 1000)
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <h1>登录索拉</h1>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名/手机号" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" native-type="submit" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="register-link">
        还没有账号？<router-link to="/login">立即注册</router-link>
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
  width: 380px;
  padding: 40px;
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;

  h1 {
    text-align: center;
    margin-bottom: 32px;
    color: #58a6ff;
  }

  .register-link {
    text-align: center;
    font-size: 14px;
    color: #8b949e;

    a {
      color: #58a6ff;
    }
  }
}
</style>
