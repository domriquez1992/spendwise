import type { ReactNode } from 'react'
import { Spinner } from './ui'
import { ApiError } from '../lib/api'

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-muted">
      <Spinner />
      <span className="text-sm">{label}</span>
    </div>
  )
}

function messageFor(error: unknown): string {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Something went wrong'
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div className="rounded-xl border border-alert/30 bg-alert-soft px-4 py-6 text-center">
      <p className="text-sm font-medium text-ink">Couldn’t load this</p>
      <p className="mt-1 text-sm text-muted">{messageFor(error)}</p>
      {onRetry ? (
        <button onClick={onRetry} className="mt-3 text-sm font-medium text-brand hover:underline">
          Try again
        </button>
      ) : null}
    </div>
  )
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-line px-4 py-12 text-center">
      <p className="text-sm font-medium text-ink">{title}</p>
      {children ? <div className="mt-1 text-sm text-muted">{children}</div> : null}
    </div>
  )
}
