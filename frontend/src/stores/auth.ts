import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, getUserInfo as getUserInfoApi } from '@/api/user'
import type { User } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<User | null>(JSON.parse(localStorage.getItem('user') || 'null'))

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (user: User) => {
    userInfo.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  const login = async (username: string, password: string) => {
    const res = await loginApi({ username, password })
    if (res.data?.token) {
      setToken(res.data.token)
      await fetchUserInfo()
      return true
    }
    return false
  }

  const register = async (username: string, password: string) => {
    const res = await registerApi({ username, password })
    if (res.data?.token) {
      setToken(res.data.token)
      await fetchUserInfo()
      return true
    }
    return false
  }

  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfoApi()
      if (res.data) {
        setUserInfo(res.data)
      }
    } catch (error) {
      console.error('Failed to fetch user info:', error)
    }
  }

  const logout = () => {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const isLoggedIn = () => !!token.value

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    login,
    register,
    fetchUserInfo,
    logout,
    isLoggedIn
  }
})
