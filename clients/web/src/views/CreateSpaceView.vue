<template>
  <div class="create-view">
    <div class="create-card">
      <h1>创建新空间</h1>
      <el-form label-position="top">
        <el-form-item label="空间名称">
          <el-input v-model="title" placeholder="给你的空间起个名字" size="large" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="空间描述">
          <el-input v-model="description" type="textarea" :rows="4" placeholder="描述你的空间..." maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-select-v2 v-model="tags" :options="tagOptions" multiple placeholder="选择标签" style="width:100%" />
        </el-form-item>
        <el-form-item label="可见性">
          <el-radio-group v-model="visibility">
            <el-radio value="PUBLIC">公开</el-radio>
            <el-radio value="FRIENDS_ONLY">仅好友可见</el-radio>
            <el-radio value="PRIVATE">私密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-button type="primary" size="large" class="btn-submit">创建空间</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const title = ref('')
const description = ref('')
const tags = ref<string[]>([])
const visibility = ref('PUBLIC')

const tagOptions = [
  { label: '社交', value: '社交' }, { label: '游戏', value: '游戏' },
  { label: '音乐', value: '音乐' }, { label: '艺术', value: '艺术' },
  { label: '技术', value: '技术' }, { label: '学习', value: '学习' },
  { label: '冥想', value: '冥想' }, { label: '演出', value: '演出' },
]
</script>

<style lang="scss" scoped>
.create-view {
  display: flex; align-items: center; justify-content: center;
  min-height: 100vh; padding: 24px;
}
.create-card {
  width: 100%; max-width: 560px;
  background: var(--solra-bg-card);
  border: 1px solid var(--solra-border);
  border-radius: 20px;
  padding: 40px;
  h1 { font-size: 28px; margin-bottom: 32px; text-align: center; }
}
.btn-submit { width: 100%; margin-top: 16px; }
</style>
