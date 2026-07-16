import axios, { type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

export const apiBaseURL = import.meta.env.VITE_API_BASE_URL || ''
const request = axios.create({ baseURL: apiBaseURL, timeout: 30000 })

let refreshPromise: Promise<void> | null = null

export class ApiRequestError extends Error {
  readonly status: number
  readonly businessCode?: number

  constructor(message: string, status = 0, businessCode?: number) {
    super(message)
    this.name = 'ApiRequestError'
    this.status = status
    this.businessCode = businessCode
  }
}

function clearSession() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
}

function loginPath() {
  return router.currentRoute.value.path.startsWith('/merchant') ? '/merchant/login' : '/login'
}

function handleUnauthorized() {
  clearSession()
  const target = loginPath()
  if (router.currentRoute.value.path !== target) {
    ElMessage.error('登录已失效，请重新登录')
    router.push(target)
  }
}

export function getErrorMessage(error: unknown, fallback = '请求失败，请稍后重试') {
  if (error instanceof ApiRequestError) return error.message
  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') return '请求超时，请稍后重试'
    return error.response?.data?.message || (error.response ? fallback : '网络连接失败，请检查网络后重试')
  }
  if (error instanceof Error && error.name === 'AbortError') return '请求已取消'
  return error instanceof Error && error.message ? error.message : fallback
}

export async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) throw new ApiRequestError('缺少刷新令牌', 401)

  const response = await axios.post(`${apiBaseURL}/api/user/refresh`, null, {
    headers: { Authorization: `Bearer ${refreshToken}` },
    timeout: 30000,
  })
  const data = response.data?.data
  if (response.data?.code !== 200 || !data?.accessToken || !data?.refreshToken) {
    throw new ApiRequestError(response.data?.message || '刷新登录状态失败', 401, response.data?.code)
  }
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('refreshToken', data.refreshToken)
  const currentUser = (() => { try { return JSON.parse(localStorage.getItem('userInfo') || 'null') } catch { return null } })()
  localStorage.setItem('userInfo', JSON.stringify({
    id: data.userId,
    username: data.username,
    nickname: data.nickname,
    role: currentUser?.role,
  }))
}

async function refreshSession() {
  refreshPromise ||= refreshAccessToken().finally(() => { refreshPromise = null })
  return refreshPromise
}

request.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token && !config.headers.Authorization) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  response => {
    if (response.data?.code !== undefined && response.data.code !== 200) {
      return Promise.reject(new ApiRequestError(response.data.message || '请求失败', response.status, response.data.code))
    }
    return response
  },
  async error => {
    const status = error.response?.status
    const config = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
    const isRefreshRequest = config?.url?.includes('/api/user/refresh')

    if (status === 401 && config && !config._retry && !isRefreshRequest && localStorage.getItem('refreshToken')) {
      config._retry = true
      try {
        await refreshSession()
        config.headers.Authorization = `Bearer ${localStorage.getItem('accessToken')}`
        return request(config)
      } catch {
        // Fall through to a clean sign-out.
      }
    }

    const message = getErrorMessage(error)
    if (status === 401) handleUnauthorized()
    else if (status === 429) ElMessage.warning(message)
    else ElMessage.error(message)
    return Promise.reject(error)
  },
)

export async function authenticatedFetch(path: string, init: RequestInit = {}, timeoutMs = 30000): Promise<Response> {
  let retried = false
  while (true) {
    const controller = new AbortController()
    let timedOut = false
    const forwardAbort = () => controller.abort(init.signal?.reason)
    if (init.signal?.aborted) forwardAbort()
    else init.signal?.addEventListener('abort', forwardAbort, { once: true })
    const timeout = window.setTimeout(() => { timedOut = true; controller.abort() }, timeoutMs)
    try {
      const headers = new Headers(init.headers)
      const token = localStorage.getItem('accessToken')
      if (token && !headers.has('Authorization')) headers.set('Authorization', `Bearer ${token}`)
      const response = await fetch(`${apiBaseURL}${path}`, { ...init, headers, signal: controller.signal })
      if (response.status === 401 && !retried && localStorage.getItem('refreshToken')) {
        try { await refreshSession() } catch {
          handleUnauthorized()
          throw new ApiRequestError('登录已失效，请重新登录', 401)
        }
        retried = true
        continue
      }
      if (response.status === 401) {
        handleUnauthorized()
        throw new ApiRequestError('登录已失效，请重新登录', 401)
      }
      if (!response.ok) {
        let message = `请求失败（HTTP ${response.status}）`
        try {
          const body = await response.clone().json()
          if (body?.message) message = body.message
        } catch {
          // Keep the status-based message for non-JSON responses.
        }
        throw new ApiRequestError(message, response.status)
      }
      return response
    } catch (error) {
      if (timedOut) throw new ApiRequestError('请求超时，请稍后重试')
      throw error
    } finally {
      window.clearTimeout(timeout)
      init.signal?.removeEventListener('abort', forwardAbort)
    }
  }
}

export default request
