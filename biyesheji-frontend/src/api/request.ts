import axios, { type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const baseURL = import.meta.env.VITE_API_BASE_URL || ''
const request = axios.create({ baseURL, timeout: 30000 })

let refreshPromise: Promise<void> | null = null

function clearSession() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) throw new Error('Missing refresh token')

  const response = await axios.post(`${baseURL}/api/user/refresh`, null, {
    headers: { Authorization: `Bearer ${refreshToken}` },
    timeout: 30000,
  })
  const data = response.data?.data
  if (response.data?.code !== 200 || !data?.accessToken || !data?.refreshToken) {
    throw new Error(response.data?.message || '刷新登录状态失败')
  }
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('refreshToken', data.refreshToken)
  localStorage.setItem('userInfo', JSON.stringify({
    id: data.userId,
    username: data.username,
    nickname: data.nickname,
  }))
}

request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    if (response.data?.code !== undefined && response.data.code !== 200) {
      return Promise.reject(new Error(response.data.message || '请求失败'))
    }
    return response
  },
  async error => {
    const status = error.response?.status
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const isRefreshRequest = config?.url?.includes('/api/user/refresh')

    if (status === 401 && config && !config._retry && !isRefreshRequest && localStorage.getItem('refreshToken')) {
      config._retry = true
      refreshPromise ||= refreshAccessToken().finally(() => { refreshPromise = null })
      try {
        await refreshPromise
        config.headers.Authorization = `Bearer ${localStorage.getItem('accessToken')}`
        return request(config)
      } catch {
        // Fall through to a clean sign-out.
      }
    }

    const message = error.response?.data?.message || error.message || '请求失败'
    if (status === 401) {
      clearSession()
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.error('登录已失效，请重新登录')
        router.push('/login')
      }
    } else if (status === 429) {
      ElMessage.warning(message)
    } else {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  },
)

export default request
