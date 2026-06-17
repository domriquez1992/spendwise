import type { ExpenseResponse } from '../types/api'
import { CategoryBadge } from './CategoryBadge'
import { Button } from './ui'
import { formatDate, formatMoney } from '../lib/format'

interface ExpenseTableProps {
  expenses: ExpenseResponse[]
  onEdit: (expense: ExpenseResponse) => void
  onDelete: (expense: ExpenseResponse) => void
  busy?: boolean
}

export function ExpenseTable({ expenses, onEdit, onDelete, busy = false }: ExpenseTableProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-line">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-line bg-sunken/60 text-left text-xs uppercase tracking-wide text-muted">
            <th className="px-4 py-3 font-medium">Description</th>
            <th className="px-4 py-3 font-medium">Category</th>
            <th className="px-4 py-3 font-medium">Date</th>
            <th className="px-4 py-3 text-right font-medium">Amount</th>
            <th className="px-4 py-3">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {expenses.map((expense) => (
            <tr key={expense.id} className="border-b border-line last:border-b-0 hover:bg-canvas">
              <td className="px-4 py-3 font-medium text-ink">{expense.description}</td>
              <td className="px-4 py-3">
                <CategoryBadge category={expense.category} />
              </td>
              <td className="whitespace-nowrap px-4 py-3 text-muted">{formatDate(expense.date)}</td>
              <td className="tnum whitespace-nowrap px-4 py-3 text-right font-medium text-ink">
                {formatMoney(expense.amount)}
              </td>
              <td className="px-4 py-3">
                <div className="flex justify-end gap-1">
                  <Button variant="ghost" size="sm" onClick={() => onEdit(expense)} disabled={busy}>
                    Edit
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => onDelete(expense)}
                    disabled={busy}
                    className="text-alert hover:bg-alert-soft hover:text-alert"
                  >
                    Delete
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
