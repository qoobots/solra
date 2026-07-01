import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
      meta: { title: '索拉 — 发现空间' },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录 — 索拉' },
    },
    {
      path: '/spaces/:spaceId',
      name: 'space-detail',
      component: () => import('@/views/SpaceDetailView.vue'),
      meta: { title: '空间详情' },
    },
    {
      path: '/create',
      name: 'create-space',
      component: () => import('@/views/CreateSpaceView.vue'),
      meta: { title: '创建空间', requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { title: '我的', requiresAuth: true },
    },
    {
      path: '/store',
      name: 'store',
      component: () => import('@/views/StoreView.vue'),
      meta: { title: '商城' },
    },
    {
      path: '/leaderboard',
      name: 'leaderboard',
      component: () => import('@/views/LeaderboardView.vue'),
      meta: { title: '排行榜' },
    },
    {
      path: '/inbox',
      name: 'inbox',
      component: () => import('@/views/InboxView.vue'),
      meta: { title: '消息', requiresAuth: true },
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/SettingsView.vue'),
      meta: { title: '设置 — 索拉' },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '索拉 Solra'
  next()
})

export default router
