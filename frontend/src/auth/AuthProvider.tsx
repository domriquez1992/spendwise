import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { AuthContext } from './AuthContext'
import type { AuthState } from './AuthContext'
import { clearToken, getToken, setToken } from '../lib/api'

const USERNAME_KEY = 'spendwise.username'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [username, setUsername] = useState<string | null>(() =>
    getToken() ? localStorage.getItem(USERNAME_KEY) : null,
  )

  const logout = useCallback(() => {
    clearToken()
    localStorage.removeItem(USERNAME_KEY)
    setUsername(null)
    queryClient.clear()
  }, [queryClient])

  const login = useCallback((token: string, name: string) => {
    setToken(token)
    localStorage.setItem(USERNAME_KEY, name)
    setUsername(name)
  }, [])

  // The api client dispatches this when an authenticated request is rejected with 401.
  useEffect(() => {
    const handler = () => logout()
    window.addEventListener('auth:unauthorized', handler)
    return () => window.removeEventListener('auth:unauthorized', handler)
  }, [logout])

  const value = useMemo<AuthState>(
    () => ({ username, isAuthenticated: username !== null, login, logout }),
    [username, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
