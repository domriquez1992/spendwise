import { Badge } from './ui'
import { CATEGORY_COLORS, CATEGORY_LABELS } from '../lib/categories'
import type { Category } from '../types/api'

/** A small colour-coded chip naming an expense category. */
export function CategoryBadge({ category }: { category: Category }) {
  const color = CATEGORY_COLORS[category]
  return (
    <Badge className="border bg-surface text-ink" style={{ borderColor: color }}>
      <span
        className="h-1.5 w-1.5 rounded-full"
        style={{ backgroundColor: color }}
        aria-hidden="true"
      />
      {CATEGORY_LABELS[category]}
    </Badge>
  )
}
