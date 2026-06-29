# Solra Web Client

基于 Vue 3 + TypeScript + Vite + Element Plus 的 Web 客户端。

## 技术栈

- **Vue 3.4** Composition API + `<script setup>`
- **TypeScript 5.3** 严格模式
- **Vite 5** 开发与构建
- **Element Plus 2.5** UI 组件库
- **Vue Router 4** SPA 路由
- **Pinia 2** 状态管理
- **SCSS** 样式

## 快速开始

```bash
cd clients/web
npm install
npm run dev          # http://localhost:5173
npm run build        # 生产构建到 dist/
```

## 目录结构

```
src/
├── api/           # HTTP 客户端 + API 接口封装
├── router/        # 路由定义 (7 routes)
├── views/         # 页面组件
│   ├── HomeView.vue          # 发现 / Feed 流
│   ├── LoginView.vue         # 登录
│   ├── SpaceDetailView.vue   # 空间详情
│   ├── CreateSpaceView.vue   # 创建空间
│   ├── ProfileView.vue       # 个人主页
│   ├── StoreView.vue         # 商城
│   ├── LeaderboardView.vue   # 排行榜
│   └── InboxView.vue         # 消息中心
├── styles/        # 全局样式 + CSS 变量
├── App.vue
└── main.ts
```

## 主题

参见 `src/styles/global.scss` — Solra 暗色主题 CSS 变量体系。
