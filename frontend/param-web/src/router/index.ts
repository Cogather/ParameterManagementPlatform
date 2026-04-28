import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/product' },
  {
    path: '/product',
    component: () => import('../views/product/ProductConfig.vue'),
  },
  {
    path: '/command',
    component: () => import('../views/command/CommandList.vue'),
  },
  {
    path: '/config',
    component: () => import('../views/config/ConfigHub.vue'),
  },
  {
    path: '/parameter',
    component: () => import('../views/parameter/ParameterLayout.vue'),
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

