import { createContext, useContext } from 'react'

export interface AuthState {
  username: string | null
  isAuthenticated: boolean
  login: (token: string, username: string) => void
  logout: () => void
}

export const AuthContext = createContext<AuthState | undefined>(undefined)

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
