import axios from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authenticatedFetch } from './request'

describe('authenticated fetch', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('shares the token contract and retries once after refresh', async () => {
    localStorage.setItem('accessToken', 'expired-access')
    localStorage.setItem('refreshToken', 'valid-refresh')
    localStorage.setItem('userInfo', JSON.stringify({ role: 0 }))
    vi.spyOn(axios, 'post').mockResolvedValue({
      data: { code: 200, data: { accessToken: 'new-access', refreshToken: 'new-refresh', userId: 7, username: 'buyer', nickname: '买家' } },
    })
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response('ok', { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    const response = await authenticatedFetch('/api/protected')

    expect(await response.text()).toBe('ok')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect((fetchMock.mock.calls[0][1].headers as Headers).get('Authorization')).toBe('Bearer expired-access')
    expect((fetchMock.mock.calls[1][1].headers as Headers).get('Authorization')).toBe('Bearer new-access')
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh')
  })

  it('turns connection aborts into a stable timeout message', async () => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn((_url: string, init: RequestInit) => new Promise((_resolve, reject) => {
      init.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
    })))

    const rejection = expect(authenticatedFetch('/api/slow', {}, 1000)).rejects.toThrow('请求超时，请稍后重试')
    await vi.advanceTimersByTimeAsync(1000)

    await rejection
  })
})
