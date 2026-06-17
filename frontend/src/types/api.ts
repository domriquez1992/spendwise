// TypeScript mirrors of the backend DTOs (see the Java records under com.domriquez.spendwise).
// BigDecimal serialises to a JSON number, LocalDate to "YYYY-MM-DD", and Instant to an ISO string.

export const CATEGORIES = [
  'FOOD',
  'TRANSPORT',
  'HOUSING',
  'ENTERTAINMENT',
  'HEALTH',
  'UTILITIES',
  'OTHER',
] as const

export type Category = (typeof CATEGORIES)[number]

export interface AuthResponse {
  token: string
  tokenType: string
  username: string
}

export interface ExpenseResponse {
  id: number
  description: string
  amount: number
  category: Category
  date: string
  createdAt: string
}

export interface ExpenseRequest {
  description: string
  amount: number
  category: Category
  date: string
}

export interface CategorySummary {
  category: Category
  total: number
}

export interface SummaryResponse {
  categories: CategorySummary[]
  grandTotal: number
}

export interface NotificationResponse {
  id: number
  message: string
  createdAt: string
}

// The subset of Spring Data's Page<T> envelope that the UI relies on.
export interface Page<T> {
  content: T[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  numberOfElements: number
}
