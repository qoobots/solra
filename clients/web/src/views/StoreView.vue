<template>
  <div class="store-view">
    <header class="store-header">
      <h2>虚拟商城</h2>
    </header>
    <div class="tab-bar">
      <el-button v-for="cat in categories" :key="cat.key" :type="selectedCat === cat.key ? 'primary' : 'default'" @click="selectedCat = cat.key">{{ cat.label }}</el-button>
    </div>
    <div class="item-grid">
      <div v-for="item in filteredItems" :key="item.itemId" class="item-card">
        <div class="item-icon">{{ item.icon }}</div>
        <h4>{{ item.name }}</h4>
        <p class="item-price">¥{{ item.price }}</p>
        <el-button size="small" type="primary">购买</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const selectedCat = ref('all')
const categories = [
  { key: 'all', label: '全部' },
  { key: 'AVATAR_SKIN', label: '虚拟人皮肤' },
  { key: 'SPACE_DECORATION', label: '空间装饰' },
  { key: 'EMOTE', label: '表情动作' },
  { key: 'EFFECT', label: '特效' },
  { key: 'SUBSCRIPTION', label: '订阅' },
]

const items = ref([
  { itemId: '1', name: '霓虹皮肤', price: 12.99, category: 'AVATAR_SKIN', icon: '👤' },
  { itemId: '2', name: '星空穹顶', price: 29.99, category: 'SPACE_DECORATION', icon: '🌟' },
  { itemId: '3', name: '比心手势', price: 6.99, category: 'EMOTE', icon: '🫶' },
  { itemId: '4', name: '粒子飘落', price: 9.99, category: 'EFFECT', icon: '✨' },
  { itemId: '5', name: '创作者订阅', price: 49.99, category: 'SUBSCRIPTION', icon: '👑' },
])

const filteredItems = computed(() =>
  selectedCat.value === 'all' ? items.value : items.value.filter(i => i.category === selectedCat.value)
)
</script>

<style lang="scss" scoped>
.store-view { max-width: 1200px; margin: 0 auto; padding: 24px; min-height: 100vh; }
.store-header { margin-bottom: 20px; h2 { font-size: 28px; } }
.tab-bar { display: flex; gap: 8px; margin-bottom: 24px; flex-wrap: wrap; }
.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.item-card {
  background: var(--solra-bg-card);
  border: 1px solid var(--solra-border);
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  .item-icon { font-size: 40px; margin-bottom: 8px; }
  h4 { margin-bottom: 4px; }
  .item-price { color: var(--solra-accent); font-weight: 600; margin-bottom: 12px; }
}
</style>
