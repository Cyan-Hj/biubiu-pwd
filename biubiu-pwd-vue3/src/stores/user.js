import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(sessionStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(sessionStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const isCustomerService = computed(() => userInfo.value?.role === 'CUSTOMER_SERVICE')
  const isPlayer = computed(() => userInfo.value?.role === 'PLAYER')

  const setToken = (newToken) => {
    token.value = newToken
    sessionStorage.setItem('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
    sessionStorage.setItem('userInfo', JSON.stringify(info))
  }

  const login = async (credentials) => {
    const res = await loginApi(credentials)
    const data = res.data || res
    setToken(data.token)
    setUserInfo(data)
    return data
  }

  const fetchUserInfo = async () => {
    const res = await getUserInfo()
    setUserInfo(res.data)
    return res.data
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    isCustomerService,
    isPlayer,
    login,
    logout,
    fetchUserInfo,
    setToken,
    setUserInfo
  }
})
