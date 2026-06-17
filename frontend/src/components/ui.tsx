import type { ComponentPropsWithRef, ReactNode } from 'react'
import { cn } from '../lib/cn'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'md' | 'sm'

const buttonBase =
  'inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors ' +
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 ' +
  'disabled:opacity-50 disabled:pointer-events-none'

const buttonVariants: Record<ButtonVariant, string> = {
  primary: 'bg-brand text-white hover:bg-brand-dark',
  secondary: 'bg-surface text-ink border border-line hover:bg-canvas',
  ghost: 'text-muted hover:text-ink hover:bg-canvas',
  danger: 'bg-alert text-white hover:brightness-95',
}

const buttonSizes: Record<ButtonSize, string> = {
  md: 'h-10 px-4 text-sm',
  sm: 'h-8 px-3 text-xs',
}

interface ButtonProps extends ComponentPropsWithRef<'button'> {
  variant?: ButtonVariant
  size?: ButtonSize
}

export function Button({ variant = 'primary', size = 'md', className, type = 'button', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(buttonBase, buttonVariants[variant], buttonSizes[size], className)}
      {...props}
    />
  )
}

export function Input({ className, ...props }: ComponentPropsWithRef<'input'>) {
  return (
    <input
      className={cn(
        'h-10 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink',
        'placeholder:text-muted/60 transition-colors',
        'focus-visible:outline-none focus-visible:border-brand focus-visible:ring-2 focus-visible:ring-brand/30',
        'aria-[invalid=true]:border-alert aria-[invalid=true]:ring-alert/30',
        className,
      )}
      {...props}
    />
  )
}

export function Select({ className, children, ...props }: ComponentPropsWithRef<'select'>) {
  return (
    <select
      className={cn(
        'h-10 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink',
        'focus-visible:outline-none focus-visible:border-brand focus-visible:ring-2 focus-visible:ring-brand/30',
        'aria-[invalid=true]:border-alert',
        className,
      )}
      {...props}
    >
      {children}
    </select>
  )
}

interface FieldProps {
  label: string
  htmlFor?: string
  error?: string
  children: ReactNode
}

export function Field({ label, htmlFor, error, children }: FieldProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-ink">
        {label}
      </label>
      {children}
      {error ? <p className="text-xs text-alert">{error}</p> : null}
    </div>
  )
}

export function Card({ className, ...props }: ComponentPropsWithRef<'div'>) {
  return <div className={cn('rounded-xl border border-line bg-surface', className)} {...props} />
}

export function Badge({ className, ...props }: ComponentPropsWithRef<'span'>) {
  return (
    <span
      className={cn('inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium', className)}
      {...props}
    />
  )
}

export function Spinner({ className }: { className?: string }) {
  return (
    <svg className={cn('animate-spin', className)} viewBox="0 0 24 24" fill="none" width="20" height="20" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" className="opacity-20" />
      <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  )
}
