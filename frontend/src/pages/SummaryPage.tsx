import type { ReactNode } from 'react'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { useSummary } from '../api/expenses'
import type { CategorySummary } from '../types/api'
import { CATEGORY_COLORS, CATEGORY_LABELS } from '../lib/categories'
import { formatMoney } from '../lib/format'
import { Card } from '../components/ui'
import { EmptyState, ErrorState, LoadingState } from '../components/states'

export function SummaryPage() {
  const { data, isPending, isError, error, refetch } = useSummary()

  let content: ReactNode
  if (isPending) {
    content = <LoadingState label="Loading summary…" />
  } else if (isError || !data) {
    content = <ErrorState error={error} onRetry={() => void refetch()} />
  } else if (data.grandTotal <= 0 || data.categories.length === 0) {
    content = (
      <EmptyState title="Nothing to summarise yet">
        Once you add expenses, you&apos;ll see a breakdown of your spending here.
      </EmptyState>
    )
  } else {
    const sorted = [...data.categories]
      .filter((c) => c.total > 0)
      .sort((a, b) => b.total - a.total)
    const max = Math.max(...sorted.map((c) => c.total))
    const chartData = sorted.map((c) => ({
      name: CATEGORY_LABELS[c.category],
      value: c.total,
      color: CATEGORY_COLORS[c.category],
    }))

    content = (
      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="relative p-6 lg:col-span-2">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={70}
                  outerRadius={100}
                  paddingAngle={2}
                  stroke="none"
                >
                  {chartData.map((d) => (
                    <Cell key={d.name} fill={d.color} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatMoney(Number(value))} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
            <span className="text-xs uppercase tracking-wide text-muted">Total</span>
            <span className="tnum text-2xl font-semibold text-ink">
              {formatMoney(data.grandTotal)}
            </span>
          </div>
        </Card>

        <Card className="p-6 lg:col-span-3">
          <h2 className="text-sm font-medium text-muted">Breakdown by category</h2>
          <ul className="mt-4 space-y-4">
            {sorted.map((c) => (
              <BreakdownRow key={c.category} item={c} max={max} grandTotal={data.grandTotal} />
            ))}
          </ul>
        </Card>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-6">
        <h1 className="font-display text-2xl font-semibold text-ink">Summary</h1>
        <p className="mt-1 text-sm text-muted">Where your money goes, by category.</p>
      </div>
      {content}
    </div>
  )
}

function BreakdownRow({
  item,
  max,
  grandTotal,
}: {
  item: CategorySummary
  max: number
  grandTotal: number
}) {
  const widthPct = max > 0 ? Math.round((item.total / max) * 100) : 0
  const sharePct = grandTotal > 0 ? Math.round((item.total / grandTotal) * 100) : 0
  return (
    <li>
      <div className="flex items-baseline justify-between gap-4">
        <span className="text-sm font-medium text-ink">{CATEGORY_LABELS[item.category]}</span>
        <span className="tnum text-sm text-ink">
          {formatMoney(item.total)} <span className="text-muted">· {sharePct}%</span>
        </span>
      </div>
      <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-sunken">
        <div
          className="h-full rounded-full"
          style={{ width: `${widthPct}%`, backgroundColor: CATEGORY_COLORS[item.category] }}
        />
      </div>
    </li>
  )
}
