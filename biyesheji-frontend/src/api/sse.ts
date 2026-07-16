import { ApiRequestError, authenticatedFetch } from './request'

interface StreamOptions {
  signal?: AbortSignal
  connectTimeoutMs?: number
  idleTimeoutMs?: number
}

async function readWithTimeout(reader: ReadableStreamDefaultReader<Uint8Array>, timeoutMs: number, signal?: AbortSignal) {
  if (signal?.aborted) throw new DOMException('Aborted', 'AbortError')
  return new Promise<ReadableStreamReadResult<Uint8Array>>((resolve, reject) => {
    const abort = () => reject(new DOMException('Aborted', 'AbortError'))
    signal?.addEventListener('abort', abort, { once: true })
    const timeout = window.setTimeout(() => {
      reader.cancel().catch(() => undefined)
      reject(new ApiRequestError('AI 响应等待超时，请重试'))
    }, timeoutMs)
    reader.read().then(resolve, reject).finally(() => {
      window.clearTimeout(timeout)
      signal?.removeEventListener('abort', abort)
    })
  })
}

export async function streamSse(path: string, onData: (data: string) => void, options: StreamOptions = {}) {
  const response = await authenticatedFetch(path, {
    headers: { Accept: 'text/event-stream' },
    signal: options.signal,
  }, options.connectTimeoutMs ?? 15000)
  if (!response.body) throw new ApiRequestError('浏览器未提供流式响应')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  try {
    while (true) {
      const { done, value } = await readWithTimeout(reader, options.idleTimeoutMs ?? 60000, options.signal)
      if (done) break
      buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''
      for (const event of events) {
        const data = event.split('\n')
          .filter(line => line.startsWith('data:'))
          .map(line => line.slice(5).trimStart())
          .join('\n')
        if (!data) continue
        if (data === '[DONE]') return
        onData(data)
      }
    }
  } finally {
    reader.releaseLock()
  }
}
