import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Button } from './ui'
import { cn } from '../lib/cn'

function navClass({ isActive }: { isActive: boolean }): string {
  return cn(
    'rounded-lg px-3 py-2 text-sm font-medium transition-colors',
    isActive ? 'bg-brand-wash text-brand-dark' : 'text-muted hover:bg-canvas hover:text-ink',
  )
}

export function Layout() {
  const { username, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-dvh">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-5xl items-center justify-between gap-4 px-4 py-3">
          <div className="flex items-center gap-6">
            <span className="font-display text-lg font-semibold text-ink">
              Spend<span className="text-brand">wise</span>
            </span>
            <nav className="flex items-center gap-1">
              <NavLink to="/" end className={navClass}>
                Expenses
              </NavLink>
              <NavLink to="/summary" className={navClass}>
                Summary
              </NavLink>
            </nav>
          </div>
          <div className="flex items-center gap-3">
            {username ? (
              <span className="hidden text-sm text-muted sm:inline">{username}</span>
            ) : null}
            <Button variant="secondary" size="sm" onClick={handleLogout}>
              Sign out
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
