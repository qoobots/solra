import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export interface StoreItem {
  itemId: string
  name: string
  description: string
  price: number
  currency: string
  category: string
  icon: string
  previewUrl: string
  isOwned: boolean
  createdAt: string
}

export interface SubscriptionPlan {
  planId: string
  name: string
  price: number
  currency: string
  interval: 'MONTHLY' | 'YEARLY'
  features: string[]
}

export const useStoreStore = defineStore('store', () => {
  const items = ref<StoreItem[]>([])
  const subscriptions = ref<SubscriptionPlan[]>([])
  const selectedCategory = ref('all')
  const loading = ref(false)
  const error = ref<string | null>(null)

  const categories = [
    { key: 'all', label: '全部' },
    { key: 'AVATAR_SKIN', label: '虚拟人皮肤' },
    { key: 'SPACE_DECORATION', label: '空间装饰' },
    { key: 'EMOTE', label: '表情动作' },
    { key: 'EFFECT', label: '特效' },
    { key: 'SUBSCRIPTION', label: '订阅' },
  ]

  const filteredItems = computed(() => {
    if (selectedCategory.value === 'all') return items.value
    return items.value.filter(i => i.category === selectedCategory.value)
  })

  async function fetchItems(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      const res = await api.get('/api/mon/v1/items', {
        params: { pageSize: 100 },
      }) as any
      items.value = (res.items || res.data || []).map(mapItem)
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '加载商城商品失败'
    } finally {
      loading.value = false
    }
  }

  async function fetchSubscriptions(): Promise<void> {
    try {
      const res = await api.get('/api/mon/v1/subscriptions') as any
      subscriptions.value = (res.subscriptions || res.data || []).map((s: any) => ({
        planId: s.planId || s.id || '',
        name: s.name || '',
        price: s.price || 0,
        currency: s.currency || 'CNY',
        interval: s.interval || 'MONTHLY',
        features: s.features || [],
      }))
    } catch (e: any) {
      console.error('Failed to fetch subscriptions:', e)
    }
  }

  async function purchaseItem(itemId: string): Promise<boolean> {
    loading.value = true
    error.value = null
    try {
      await api.post('/api/mon/v1/orders', { itemId })
      const item = items.value.find(i => i.itemId === itemId)
      if (item) item.isOwned = true
      return true
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '购买失败'
      return false
    } finally {
      loading.value = false
    }
  }

  function mapItem(raw: any): StoreItem {
    return {
      itemId: raw.itemId || raw.id || '',
      name: raw.name || '',
      description: raw.description || '',
      price: raw.price || 0,
      currency: raw.currency || 'CNY',
      category: raw.category || 'AVATAR_SKIN',
      icon: raw.icon || '',
      previewUrl: raw.previewUrl || '',
      isOwned: raw.isOwned ?? false,
      createdAt: raw.createdAt || '',
    }
  }

  return {
    items,
    subscriptions,
    selectedCategory,
    loading,
    error,
    categories,
    filteredItems,
    fetchItems,
    fetchSubscriptions,
    purchaseItem,
  }
})
