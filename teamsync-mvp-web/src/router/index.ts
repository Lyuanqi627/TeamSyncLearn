import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/member/Layout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/member/Dashboard.vue'),
        meta: { title: '我的看板' }
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/member/Schedule.vue'),
        meta: { title: '日程管理' }
      },
      {
        path: 'upload',
        name: 'Upload',
        component: () => import('@/views/member/Upload.vue'),
        meta: { title: '成果上传' }
      },
      {
        path: 'summary',
        name: 'Summary',
        component: () => import('@/views/member/Summary.vue'),
        meta: { title: '学习总结' }
      },
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('@/views/member/AiAssistant.vue'),
        meta: { title: 'AI助手' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/member/Profile.vue'),
        meta: { title: '我的' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    redirect: '/admin/teamboard',
    children: [
      {
        path: 'teamboard',
        name: 'TeamBoard',
        component: () => import('@/views/admin/TeamBoard.vue'),
        meta: { title: '团队看板' }
      },
      {
        path: 'wordcloud',
        name: 'WordCloud',
        component: () => import('@/views/admin/WordCloud.vue'),
        meta: { title: '词云分析' }
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresSuperAdmin: true }
      },
      {
        path: 'member/:id',
        name: 'MemberDetail',
        component: () => import('@/views/admin/MemberDetail.vue'),
        meta: { title: '成员详情' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) ? `${to.meta.title} - TeamSync Learn` : 'TeamSync Learn'

  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

  if (to.meta.requiresAuth && !token) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && !['ADMIN', 'SUPER_ADMIN'].includes(userInfo.role)) {
    next('/dashboard')
    return
  }

  if (to.meta.requiresSuperAdmin && userInfo.role !== 'SUPER_ADMIN') {
    next('/dashboard')
    return
  }

  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }

  next()
})

export default router
