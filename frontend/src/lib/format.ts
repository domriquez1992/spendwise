const currency = import.meta.env.VITE_CURRENCY ?? 'USD'

const moneyFmt = new Intl.NumberFormat(undefined, {
  style: 'currency',
  currency,
})

/** Format a numeric amount as currency, e.g. 1234.5 -> "$1,234.50". */
export function formatMoney(amount: number): string {
  return moneyFmt.format(amount)
}

const dateFmt = new Intl.DateTimeFormat(undefined, {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

/** Format a "YYYY-MM-DD" date. Parsed as a local date to avoid timezone day-shifts. */
export function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number)
  return dateFmt.format(new Date(y, m - 1, d))
}

const dateTimeFmt = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
})

/** Format an ISO instant (e.g. createdAt) as a local date and time. */
export function formatDateTime(iso: string): string {
  return dateTimeFmt.format(new Date(iso))
}

/** Today's date as "YYYY-MM-DD" in local time (for date-input defaults/maximums). */
export function todayIso(): string {
  const now = new Date()
  const tzOffset = now.getTimezoneOffset() * 60_000
  return new Date(now.getTime() - tzOffset).toISOString().slice(0, 10)
}
