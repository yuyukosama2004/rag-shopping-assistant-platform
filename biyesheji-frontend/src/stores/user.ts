import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, login as loginApi, logout as logoutApi } from '../api/user'

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
    const info = (await getUserInfo()).data.data
    const userInfo = { id: data.userId, username: data.username, nickname: data.nickname, role: info.role }
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
    user.value = userInfo
    return data
  }

  function logout() {
    logoutApi().catch(() => undefined)
    clearSession()
    window.location.href = '/login'
  }

  function clearSession() {
    token.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  return { token, user, isLoggedIn, login, logout, clearSession }
})
