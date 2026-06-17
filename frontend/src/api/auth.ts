import { apiRequest } from '../lib/api'
import type { AuthResponse } from '../types/api'

export interface Credentials {
  username: string
  password: string
}

export function loginRequest(credentials: Credentials): Promise<AuthResponse> {
  return apiRequest<AuthResponse>('/auth/login', { method: 'POST', body: credentials, auth: false })
}

export function registerRequest(credentials: Credentials): Promise<void> {
  return apiRequest<void>('/auth/register', { method: 'POST', body: credentials, auth: false })
}
