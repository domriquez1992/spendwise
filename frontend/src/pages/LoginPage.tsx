import { useState } from 'react'
import { useForm } from 'react-hook-form'
import type { SubmitHandler } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button, Card, Field, Input } from '../components/ui'
import { useAuth } from '../auth/AuthContext'
import { loginRequest, registerRequest } from '../api/auth'
import { ApiError } from '../lib/api'

const schema = z.object({
  username: z
    .string()
    .trim()
    .min(3, 'At least 3 characters')
    .max(50, 'At most 50 characters'),
  password: z
    .string()
    .min(8, 'At least 8 characters')
    .max(100, 'At most 100 characters'),
})

type LoginFormValues = z.infer<typeof schema>

type Mode = 'login' | 'register'

function errorMessage(err: unknown, mode: Mode): string {
  if (err instanceof ApiError) {
    if (mode === 'login' && (err.status === 401 || err.status === 403)) {
      return 'Incorrect username or password.'
    }
    if (mode === 'register' && err.status === 409) {
      return 'That username is already taken.'
    }
    return err.message
  }
  return 'Could not reach the server. Please try again.'
}

export function LoginPage() {
  const [mode, setMode] = useState<Mode>('login')
  const [serverError, setServerError] = useState<string | null>(null)
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from ?? '/'

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(schema) })

  const submit: SubmitHandler<LoginFormValues> = async (data) => {
    setServerError(null)
    try {
      if (mode === 'register') {
        await registerRequest(data)
      }
      const auth = await loginRequest(data)
      login(auth.token, auth.username)
      navigate(from, { replace: true })
    } catch (err) {
      setServerError(errorMessage(err, mode))
    }
  }

  function toggleMode() {
    setMode((m) => (m === 'login' ? 'register' : 'login'))
    setServerError(null)
  }

  return (
    <div className="flex min-h-dvh items-center justify-center bg-canvas px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <span className="font-display text-3xl font-semibold text-ink">
            Spend<span className="text-brand">wise</span>
          </span>
          <p className="mt-2 text-sm text-muted">Track expenses and see where your money goes.</p>
        </div>

        <Card className="p-6">
          <h1 className="text-lg font-semibold text-ink">
            {mode === 'login' ? 'Sign in' : 'Create your account'}
          </h1>

          <form onSubmit={handleSubmit(submit)} className="mt-4 space-y-4" noValidate>
            <Field label="Username" htmlFor="username" error={errors.username?.message}>
              <Input
                id="username"
                autoComplete="username"
                autoFocus
                aria-invalid={errors.username ? true : undefined}
                {...register('username')}
              />
            </Field>

            <Field label="Password" htmlFor="password" error={errors.password?.message}>
              <Input
                id="password"
                type="password"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                aria-invalid={errors.password ? true : undefined}
                {...register('password')}
              />
            </Field>

            {serverError ? (
              <p className="rounded-lg bg-alert-soft px-3 py-2 text-sm text-alert">{serverError}</p>
            ) : null}

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting
                ? 'Please wait…'
                : mode === 'login'
                  ? 'Sign in'
                  : 'Create account'}
            </Button>
          </form>
        </Card>

        <p className="mt-6 text-center text-sm text-muted">
          {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}{' '}
          <button
            type="button"
            onClick={toggleMode}
            className="font-medium text-brand hover:underline"
          >
            {mode === 'login' ? 'Create one' : 'Sign in'}
          </button>
        </p>
      </div>
    </div>
  )
}
