import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('accessToken') || '')
  const user = ref(parseLocalUser())

function parseLocalUser() {
  try { return JSON.parse(localStorage.getItem('userInfo') || 'null') } catch { return null }
}

  const isLoggedIn = () => !!token.value

  async function login(username: string, password: string) {
    const res = await loginApi(username, password)
    const data = res.data.data
    token.value = data.accessToken
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userInfo', JSON.stringify({ id: data.userId, username: data.username, nickname: data.nickname }))
    user.value = { id: data.userId, username: data.username, nickname: data.nickname }
    return data
  }

  function logout() {
    logoutApi().catch(() => undefined)
    token.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    window.location.href = '/login'
  }

  return { token, user, isLoggedIn, login, logout }
})
