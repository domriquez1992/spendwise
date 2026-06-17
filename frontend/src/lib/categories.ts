import type { Category } from '../types/api'

// Human-readable labels and the per-category colour (a CSS variable defined in index.css), used by
// badges and the spending-breakdown bars.
export const CATEGORY_LABELS: Record<Category, string> = {
  FOOD: 'Food',
  TRANSPORT: 'Transport',
  HOUSING: 'Housing',
  ENTERTAINMENT: 'Entertainment',
  HEALTH: 'Health',
  UTILITIES: 'Utilities',
  OTHER: 'Other',
}

export const CATEGORY_COLORS: Record<Category, string> = {
  FOOD: 'var(--color-cat-food)',
  TRANSPORT: 'var(--color-cat-transport)',
  HOUSING: 'var(--color-cat-housing)',
  ENTERTAINMENT: 'var(--color-cat-entertainment)',
  HEALTH: 'var(--color-cat-health)',
  UTILITIES: 'var(--color-cat-utilities)',
  OTHER: 'var(--color-cat-other)',
}
