import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiRequest, getToken, setToken } from './api'

function jsonResponse(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function headerValue(init: RequestInit | undefined, name: string): string | undefined {
  const headers = (init?.headers ?? {}) as Record<string, string>
  return headers[name]
}

describe('apiRequest', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('attaches the bearer token to authenticated requests', async () => {
    setToken('tok-123')
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(() => Promise.resolve(jsonResponse(200, { ok: true })))

    await apiRequest('/expenses')

    const [, init] = fetchMock.mock.calls[0]
    expect(headerValue(init, 'Authorization')).toBe('Bearer tok-123')
  })

  it('omits the token when auth is disabled', async () => {
    setToken('tok-123')
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockImplementation(() => Promise.resolve(jsonResponse(200, {})))

    await apiRequest('/auth/login', { method: 'POST', body: {}, auth: false })

    const [, init] = fetchMock.mock.calls[0]
    expect(headerValue(init, 'Authorization')).toBeUndefined()
  })

  it('parses a ProblemDetail body into a typed ApiError with field errors', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(() =>
      Promise.resolve(
        jsonResponse(400, {
          title: 'Bad Request',
          status: 400,
          detail: 'Validation failed',
          errors: { amount: 'must be greater than 0' },
        }),
      ),
    )

    try {
      await apiRequest('/expenses', { method: 'POST', body: {} })
      expect.unreachable('apiRequest should have thrown')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      const apiErr = err as ApiError
      expect(apiErr.status).toBe(400)
      expect(apiErr.fieldErrors.amount).toBe('must be greater than 0')
    }
  })

  it('clears the token and emits an event on 401', async () => {
    setToken('expired')
    vi.spyOn(globalThis, 'fetch').mockImplementation(() =>
      Promise.resolve(jsonResponse(401, { title: 'Unauthorized' })),
    )
    const onUnauthorized = vi.fn()
    window.addEventListener('auth:unauthorized', onUnauthorized)

    await expect(apiRequest('/expenses')).rejects.toBeInstanceOf(ApiError)
    expect(getToken()).toBeNull()
    expect(onUnauthorized).toHaveBeenCalledOnce()

    window.removeEventListener('auth:unauthorized', onUnauthorized)
  })

  it('returns undefined for a 204 No Content response', async () => {
    setToken('tok')
    vi.spyOn(globalThis, 'fetch').mockImplementation(() =>
      Promise.resolve(new Response(null, { status: 204 })),
    )

    const result = await apiRequest('/expenses/1', { method: 'DELETE' })
    expect(result).toBeUndefined()
  })
})
