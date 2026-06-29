import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref<any>(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const username = computed(() => userInfo.value?.username || '')

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

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function fetchUserInfo() {
    const res: any = await getUserInfo()
    if (res.code === 200) {
      userInfo.value = res.data
    }
  }

  return { token, userInfo, isLoggedIn, isAdmin, username, login, logout, fetchUserInfo }
})
