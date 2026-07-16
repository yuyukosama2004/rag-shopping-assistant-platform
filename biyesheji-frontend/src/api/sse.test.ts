import { describe, expect, it, vi } from 'vitest'

const authenticatedFetch = vi.hoisted(() => vi.fn())
vi.mock('./request', () => ({
  authenticatedFetch,
  ApiRequestError: class ApiRequestError extends Error {},
}))

import { streamSse } from './sse'

function streamResponse(...chunks: string[]) {
  const encoder = new TextEncoder()
  return new Response(new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk))
      controller.close()
    },
  }))
}

describe('SSE client', () => {
  it('parses split events, multiline data, and the done marker', async () => {
    authenticatedFetch.mockResolvedValue(streamResponse(
      'data: 第一行\ndata: 第二行\n\n',
      'data: 第三段\n\ndata: [DONE]\n\n',
      'data: 不应读取\n\n',
    ))
    const messages: string[] = []

    await streamSse('/api/order/ai/chat?query=test', data => messages.push(data))

    expect(messages).toEqual(['第一行\n第二行', '第三段'])
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/order/ai/chat?query=test', {
      headers: { Accept: 'text/event-stream' },
      signal: undefined,
    }, 15000)
  })

  it('preserves UTF-8 characters split across network chunks', async () => {
    const bytes = new TextEncoder().encode('data: 手机推荐\n\n')
    authenticatedFetch.mockResolvedValue(new Response(new ReadableStream({
      start(controller) {
        controller.enqueue(bytes.slice(0, 10))
        controller.enqueue(bytes.slice(10))
        controller.close()
      },
    })))
    const messages: string[] = []

    await streamSse('/stream', data => messages.push(data))

    expect(messages).toEqual(['手机推荐'])
  })
})
