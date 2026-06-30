<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { invoke } from '@tauri-apps/api/core'
import api from '@/api'

interface StoreItem {
  id: string
  name: string
  description: string
  price: number
  currency: string
  category: string
  thumbnail: string
}

const items = ref<StoreItem[]>([])
const activeCategory = ref('all')
const loading = ref(false)

const categories = [
  { key: 'all', label: '全部' },
  { key: 'avatar', label: '虚拟人' },
  { key: 'scene', label: '场景模板' },
  { key: 'prop', label: '道具' },
  { key: 'effect', label: '特效' },
]

async function fetchItems(category?: string) {
  loading.value = true
  try {
    // 优先 HTTP API
    const params: Record<string, string> = {}
    if (category && category !== 'all') params.category = category
    const res = await api.get('/api/store/v1/items', { params }) as any
    const data = (res.items || res.data || []).map(mapItem)
    items.value = data
  } catch {
    // 回退到 Tauri IPC
    try {
      const cat = activeCategory.value !== 'all' ? activeCategory.value : undefined
      const data = await invoke<any[]>('get_store_items', { category: cat })
      items.value = data.map((i: any) => ({
        id: i.id,
        name: i.name,
        description: i.description,
        price: i.price,
        currency: i.currency,
        category: i.category,
        thumbnail: i.thumbnail_url,
      }))
    } catch (e) {
      console.error('加载商城失败:', e)
      items.value = []
    }
  } finally {
    loading.value = false
  }
}

function mapItem(raw: any): StoreItem {
  return {
    id: raw.itemId || raw.id || '',
    name: raw.name || raw.title || '',
    description: raw.description || '',
    price: raw.price || 0,
    currency: raw.currency || 'CNY',
    category: raw.category || '',
    thumbnail: raw.thumbnailUrl || raw.imageUrl || '',
  }
}

watch(activeCategory, (cat) => {
  fetchItems(cat === 'all' ? undefined : cat)
})

onMounted(() => {
  fetchItems()
})
</script>

<template>
  <div class="store-view">
    <header class="page-header">
      <h1>商城</h1>
    </header>

    <!-- 分类标签 -->
    <div class="category-bar">
      <button
        v-for="cat in categories"
        :key="cat.key"
        :class="['category-tab', { active: activeCategory === cat.key }]"
        @click="activeCategory = cat.key"
      >
        {{ cat.label }}
      </button>
    </div>

    <!-- 商品列表 -->
    <div class="store-content">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="items.length === 0" class="empty-state">
        <div class="empty-icon">🛒</div>
        <p>商城即将上线</p>
        <p class="sub">更多虚拟人、场景模板和道具即将推出</p>
      </div>
      <div v-else class="item-grid">
        <div v-for="item in items" :key="item.id" class="item-card">
          <div class="item-thumbnail">
            <img :src="item.thumbnail" :alt="item.name" />
          </div>
          <div class="item-info">
            <h3>{{ item.name }}</h3>
            <p>{{ item.description }}</p>
            <div class="item-footer">
              <span class="price">{{ item.price }} {{ item.currency }}</span>
              <button class="buy-btn">购买</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.store-view {
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

.category-bar {
  display: flex;
  gap: 8px;
  padding: 16px 32px;
  border-bottom: 1px solid #21262d;

  .category-tab {
    background: none;
    border: 1px solid #30363d;
    color: #8b949e;
    padding: 6px 16px;
    border-radius: 20px;
    cursor: pointer;
    font-size: 13px;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      background: #1f6feb;
      border-color: #1f6feb;
      color: #fff;
    }
  }
}

.store-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 32px;
}

.empty-state {
  text-align: center;
  padding: 80px 0;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  p {
    font-size: 16px;
    margin: 0 0 8px;
  }

  .sub {
    font-size: 14px;
    color: #8b949e;
  }
}

.item-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.item-card {
  background: var(--solra-bg-secondary, #161b22);
  border: 1px solid var(--solra-border, #30363d);
  border-radius: 12px;
  overflow: hidden;
  transition: border-color 0.2s;

  &:hover {
    border-color: #58a6ff;
  }

  .item-thumbnail {
    width: 100%;
    height: 150px;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .item-info {
    padding: 14px;

    h3 {
      margin: 0 0 6px;
      font-size: 15px;
    }

    p {
      margin: 0 0 12px;
      font-size: 12px;
      color: #8b949e;
    }

    .item-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .price {
        font-size: 14px;
        font-weight: 600;
        color: #58a6ff;
      }

      .buy-btn {
        background: #238636;
        border: none;
        color: #fff;
        padding: 4px 14px;
        border-radius: 6px;
        cursor: pointer;
        font-size: 12px;

        &:hover {
          background: #2ea043;
        }
      }
    }
  }
}

.loading {
  text-align: center;
  padding: 60px;
  color: #8b949e;
}
</style>
