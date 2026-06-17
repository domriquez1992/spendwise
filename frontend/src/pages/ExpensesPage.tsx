import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useCreateExpense, useDeleteExpense, useExpenses, useUpdateExpense } from '../api/expenses'
import type { ExpenseRequest, ExpenseResponse } from '../types/api'
import { ExpenseTable } from '../components/ExpenseTable'
import { ExpenseFormDialog } from '../components/ExpenseFormDialog'
import { Button } from '../components/ui'
import { EmptyState, ErrorState, LoadingState } from '../components/states'

interface DialogState {
  open: boolean
  mode: 'create' | 'edit'
  target?: ExpenseResponse
}

export function ExpensesPage() {
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState<DialogState>({ open: false, mode: 'create' })

  const { data, isPending, isError, error, refetch, isFetching } = useExpenses(page)
  const create = useCreateExpense()
  const update = useUpdateExpense()
  const remove = useDeleteExpense()

  // If the current page becomes empty after a deletion (and it isn't the first page), step back.
  useEffect(() => {
    if (data && data.content.length === 0 && page > 0) {
      setPage((p) => Math.max(0, p - 1))
    }
  }, [data, page])

  function openCreate() {
    setDialog({ open: true, mode: 'create' })
  }

  function openEdit(expense: ExpenseResponse) {
    setDialog({ open: true, mode: 'edit', target: expense })
  }

  function closeDialog() {
    setDialog((d) => ({ ...d, open: false }))
  }

  async function handleSubmit(values: ExpenseRequest) {
    if (dialog.mode === 'edit' && dialog.target) {
      await update.mutateAsync({ id: dialog.target.id, body: values })
    } else {
      await create.mutateAsync(values)
    }
    closeDialog()
  }

  function handleDelete(expense: ExpenseResponse) {
    if (window.confirm(`Delete "${expense.description}"?`)) {
      remove.mutate(expense.id)
    }
  }

  let content: ReactNode
  if (isPending) {
    content = <LoadingState label="Loading expenses…" />
  } else if (isError || !data) {
    content = <ErrorState error={error} onRetry={() => void refetch()} />
  } else if (data.content.length === 0) {
    content = (
      <EmptyState title="No expenses yet">
        Add your first expense to start tracking your spending.
      </EmptyState>
    )
  } else {
    content = (
      <>
        <ExpenseTable
          expenses={data.content}
          onEdit={openEdit}
          onDelete={handleDelete}
          busy={remove.isPending}
        />
        <div className="mt-4 flex items-center justify-between gap-4">
          <p className="text-sm text-muted">
            Page {data.number + 1} of {Math.max(1, data.totalPages)}
          </p>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={data.first || isFetching}
            >
              Previous
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPage((p) => p + 1)}
              disabled={data.last || isFetching}
            >
              Next
            </Button>
          </div>
        </div>
      </>
    )
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-semibold text-ink">Expenses</h1>
          {data ? (
            <p className="mt-1 text-sm text-muted">
              {data.totalElements} {data.totalElements === 1 ? 'entry' : 'entries'}
            </p>
          ) : null}
        </div>
        <Button onClick={openCreate}>Add expense</Button>
      </div>

      {content}

      <ExpenseFormDialog
        open={dialog.open}
        mode={dialog.mode}
        initial={dialog.target}
        onClose={closeDialog}
        onSubmit={handleSubmit}
      />
    </div>
  )
}
