<template>
  <div class="store-view">
    <header class="store-header">
      <h2>虚拟商城</h2>
    </header>
    <div class="tab-bar">
      <el-button
        v-for="cat in categories"
        :key="cat.key"
        :type="selectedCategory === cat.key ? 'primary' : 'default'"
        @click="selectedCategory = cat.key"
      >
        {{ cat.label }}
      </el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <p>正在加载商品...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <el-button @click="storeStore.fetchItems()">重试</el-button>
    </div>

    <div v-else-if="filteredItems.length === 0" class="empty-state">
      <p>该分类暂无商品</p>
    </div>

    <div v-else class="item-grid">
      <div v-for="item in filteredItems" :key="item.itemId" class="item-card">
        <div class="item-icon">{{ item.icon || categoryIcon(item.category) }}</div>
        <h4>{{ item.name }}</h4>
        <p class="item-price">¥{{ item.price }}</p>
        <p class="item-desc" v-if="item.description">{{ item.description }}</p>
        <el-button
          size="small"
          :type="item.isOwned ? 'success' : 'primary'"
          :disabled="item.isOwned"
          @click="handlePurchase(item.itemId)"
        >
          {{ item.isOwned ? '已拥有' : '购买' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useStoreStore } from '@/stores/useStoreStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'

const storeStore = useStoreStore()
const authStore = useAuthStore()
const { items, filteredItems, selectedCategory, loading, error, categories } = storeToRefs(storeStore)

onMounted(async () => {
  await storeStore.fetchItems()
  await storeStore.fetchSubscriptions()
})

async function handlePurchase(itemId: string) {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    return
  }
  const success = await storeStore.purchaseItem(itemId)
  if (success) {
    ElMessage.success('购买成功！')
  } else {
    ElMessage.error(storeStore.error || '购买失败')
  }
}

function categoryIcon(category: string): string {
  const icons: Record<string, string> = {
    AVATAR_SKIN: '👤',
    SPACE_DECORATION: '🌟',
    EMOTE: '🫶',
    EFFECT: '✨',
    SUBSCRIPTION: '👑',
  }
  return icons[category] || '📦'
}
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
  .item-price { color: var(--solra-accent); font-weight: 600; margin-bottom: 4px; }
  .item-desc { font-size: 12px; color: var(--solra-text-secondary); margin-bottom: 12px; }
}

.loading-state, .error-state, .empty-state {
  text-align: center; padding: 60px 20px; color: var(--solra-text-secondary); font-size: 16px;
}
</style>
