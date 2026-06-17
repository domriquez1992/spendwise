// Minimal typed fetch client. Adds the bearer token, serialises JSON, and turns RFC-7807
// ProblemDetail error bodies into a typed ApiError (including field-level validation messages).

const BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
const TOKEN_KEY = 'spendwise.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ProblemDetail

  constructor(status: number, problem?: ProblemDetail) {
    super(problem?.detail ?? problem?.title ?? `Request failed (${status})`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }

  /** Field-level validation messages keyed by field name, if the server returned any. */
  get fieldErrors(): Record<string, string> {
    return this.problem?.errors ?? {}
  }
}

type QueryValue = string | number | undefined

interface RequestOptions {
  method?: string
  body?: unknown
  query?: Record<string, QueryValue>
  auth?: boolean
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const params = new URLSearchParams()
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== '') params.set(key, String(value))
    }
  }
  const qs = params.toString()
  return `${BASE}${path}${qs ? `?${qs}` : ''}`
}

async function readProblem(res: Response): Promise<ProblemDetail | undefined> {
  try {
    const text = await res.text()
    return text ? (JSON.parse(text) as ProblemDetail) : undefined
  } catch {
    return undefined
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, auth = true } = options

  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(buildUrl(path, query), {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  // An authenticated request rejected with 401 means the session is no longer valid.
  if (res.status === 401 && auth) {
    clearToken()
    window.dispatchEvent(new Event('auth:unauthorized'))
  }

  if (!res.ok) {
    throw new ApiError(res.status, await readProblem(res))
  }

  if (res.status === 204) return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}
