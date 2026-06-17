import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import type { SubmitHandler } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Button, Field, Input, Select } from './ui'
import { ApiError } from '../lib/api'
import { todayIso } from '../lib/format'
import { CATEGORIES } from '../types/api'
import type { ExpenseRequest, ExpenseResponse } from '../types/api'
import { CATEGORY_LABELS } from '../lib/categories'

// All fields are strings in the form (easiest to bind to native inputs); the amount is parsed to a
// number when we build the ExpenseRequest. Validation mirrors the backend bean-validation rules.
const schema = z.object({
  description: z
    .string()
    .trim()
    .min(1, 'Description is required')
    .max(255, 'Keep it under 255 characters'),
  amount: z
    .string()
    .min(1, 'Amount is required')
    .refine((v) => /^\d+(\.\d{1,2})?$/.test(v), 'Enter a valid amount, e.g. 12.50')
    .refine((v) => Number(v) > 0, 'Amount must be greater than 0'),
  category: z.enum(CATEGORIES),
  date: z
    .string()
    .min(1, 'Date is required')
    .refine((v) => v <= todayIso(), 'Date cannot be in the future'),
})

type ExpenseFormValues = z.infer<typeof schema>

interface ExpenseFormDialogProps {
  open: boolean
  mode: 'create' | 'edit'
  initial?: ExpenseResponse
  onClose: () => void
  onSubmit: (values: ExpenseRequest) => Promise<void>
}

function defaultsFor(initial?: ExpenseResponse): ExpenseFormValues {
  if (initial) {
    return {
      description: initial.description,
      amount: String(initial.amount),
      category: initial.category,
      date: initial.date,
    }
  }
  return { description: '', amount: '', category: 'FOOD', date: todayIso() }
}

export function ExpenseFormDialog({
  open,
  mode,
  initial,
  onClose,
  onSubmit,
}: ExpenseFormDialogProps) {
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ExpenseFormValues>({
    resolver: zodResolver(schema),
    defaultValues: defaultsFor(initial),
  })

  // Reset the form whenever the dialog opens or switches between create/edit targets.
  useEffect(() => {
    if (open) reset(defaultsFor(initial))
  }, [open, initial, reset])

  // Close on Escape for keyboard users.
  useEffect(() => {
    if (!open) return
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [open, onClose])

  if (!open) return null

  const submit: SubmitHandler<ExpenseFormValues> = async (data) => {
    const payload: ExpenseRequest = {
      description: data.description.trim(),
      amount: Number(data.amount),
      category: data.category,
      date: data.date,
    }
    try {
      await onSubmit(payload)
    } catch (err) {
      if (err instanceof ApiError) {
        const fields = err.fieldErrors
        let mapped = false
        for (const key of ['description', 'amount', 'category', 'date'] as const) {
          if (fields[key]) {
            setError(key, { message: fields[key] })
            mapped = true
          }
        }
        if (!mapped) setError('root', { message: err.message })
      } else {
        setError('root', { message: 'Something went wrong. Please try again.' })
      }
    }
  }

  const titleId = 'expense-dialog-title'

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/40 p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-md rounded-xl border border-line bg-surface p-6 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id={titleId} className="font-display text-lg font-semibold text-ink">
          {mode === 'edit' ? 'Edit expense' : 'New expense'}
        </h2>

        <form onSubmit={handleSubmit(submit)} className="mt-4 space-y-4" noValidate>
          <Field label="Description" htmlFor="description" error={errors.description?.message}>
            <Input
              id="description"
              autoFocus
              placeholder="e.g. Groceries"
              aria-invalid={errors.description ? true : undefined}
              {...register('description')}
            />
          </Field>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Amount" htmlFor="amount" error={errors.amount?.message}>
              <Input
                id="amount"
                inputMode="decimal"
                placeholder="0.00"
                aria-invalid={errors.amount ? true : undefined}
                {...register('amount')}
              />
            </Field>

            <Field label="Date" htmlFor="date" error={errors.date?.message}>
              <Input
                id="date"
                type="date"
                max={todayIso()}
                aria-invalid={errors.date ? true : undefined}
                {...register('date')}
              />
            </Field>
          </div>

          <Field label="Category" htmlFor="category" error={errors.category?.message}>
            <Select id="category" {...register('category')}>
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {CATEGORY_LABELS[category]}
                </option>
              ))}
            </Select>
          </Field>

          {errors.root ? <p className="text-sm text-alert">{errors.root.message}</p> : null}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : mode === 'edit' ? 'Save changes' : 'Add expense'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
