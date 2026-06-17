import { describe, expect, it } from 'vitest'
import { formatDate, formatMoney, todayIso } from './format'

describe('formatMoney', () => {
  it('formats a number with grouping and two decimals', () => {
    expect(formatMoney(1234.5)).toMatch(/1,234\.50/)
  })

  it('formats zero with two decimals', () => {
    expect(formatMoney(0)).toMatch(/0\.00/)
  })
})

describe('formatDate', () => {
  it('formats an ISO date without a timezone day-shift', () => {
    const formatted = formatDate('2024-01-01')
    expect(formatted).toMatch(/Jan/)
    expect(formatted).toMatch(/2024/)
  })
})

describe('todayIso', () => {
  it('returns a YYYY-MM-DD string', () => {
    expect(todayIso()).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })
})
