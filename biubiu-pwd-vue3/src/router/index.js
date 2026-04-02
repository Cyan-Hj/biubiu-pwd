import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'players',
        name: 'Players',
        component: () => import('@/views/Players.vue'),
        meta: { roles: ['ADMIN', 'CUSTOMER_SERVICE'] }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue')
      },
      {
        path: 'finance',
        name: 'Finance',
        component: () => import('@/views/Finance.vue')
      },
      {
        path: 'withdrawals',
        name: 'Withdrawals',
        component: () => import('@/views/Withdrawals.vue'),
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'boss',
        name: 'Boss',
        component: () => import('@/views/Boss.vue'),
        meta: { roles: ['ADMIN', 'CUSTOMER_SERVICE'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = sessionStorage.getItem('token')

  if (to.meta.public) {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  if (to.meta.roles && !to.meta.roles.includes(userStore.userInfo?.role)) {
    next('/dashboard')
    return
  }

  next()
})

export default router
