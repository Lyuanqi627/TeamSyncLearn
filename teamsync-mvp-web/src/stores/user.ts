import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo, logout as logoutApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => ['ADMIN', 'SUPER_ADMIN'].includes(userInfo.value?.role))
  const isSuperAdmin = computed(() => userInfo.value?.role === 'SUPER_ADMIN')
  const roleLabel = computed(() =>
    ({ SUPER_ADMIN: '超级管理员', ADMIN: '管理员', MEMBER: '成员' } as Record<string, string>)[userInfo.value?.role] || '成员')
  const username = computed(() => userInfo.value?.username || '')
  const displayName = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  const avatarUrl = computed(() => {
    const a = userInfo.value?.avatar
    if (!a) return ''
    if (a.startsWith('http') || a.startsWith('/uploads/')) return a
    return '/uploads/' + a
  })

  async function login(credentials: { username: string; password?: string }) {
    const res: any = await loginApi(credentials)
    if (res.code === 200) {
      token.value = res.data.token
      userInfo.value = {
        userId: res.data.userId,
        username: res.data.username,
        role: res.data.role,
        avatar: res.data.avatar
      }
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // 忽略：服务端会话可能已失效或网络异常；无论如何都要清本地状态
    }
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function fetchUserInfo() {
    const res: any = await getUserInfo()
    if (res.code === 200) {
      // /api/user/info 返回的是 SysUser 实体(id 字段),统一规范为 userId,并持久化到 localStorage
      userInfo.value = {
        userId: res.data.id ?? res.data.userId,
        username: res.data.username,
        role: res.data.role,
        avatar: res.data.avatar,
        nickname: res.data.nickname,
        bio: res.data.bio,
        gender: res.data.gender,
        age: res.data.age,
        address: res.data.address
      }
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  return { token, userInfo, isLoggedIn, isAdmin, isSuperAdmin, roleLabel, username, displayName, avatarUrl, login, logout, fetchUserInfo }
})
