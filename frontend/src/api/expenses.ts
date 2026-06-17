import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../lib/api'
import type { ExpenseRequest, ExpenseResponse, Page, SummaryResponse } from '../types/api'

const expenseKeys = {
  all: ['expenses'] as const,
  list: (page: number, size: number) => ['expenses', { page, size }] as const,
}

const summaryKeys = {
  all: ['summary'] as const,
  range: (from?: string, to?: string) => ['summary', { from, to }] as const,
}

function invalidateExpenseData(queryClient: ReturnType<typeof useQueryClient>): void {
  void queryClient.invalidateQueries({ queryKey: expenseKeys.all })
  void queryClient.invalidateQueries({ queryKey: summaryKeys.all })
}

export function useExpenses(page: number, size = 10) {
  return useQuery({
    queryKey: expenseKeys.list(page, size),
    queryFn: () => apiRequest<Page<ExpenseResponse>>('/expenses', { query: { page, size } }),
    placeholderData: keepPreviousData,
  })
}

export function useSummary(from?: string, to?: string) {
  return useQuery({
    queryKey: summaryKeys.range(from, to),
    queryFn: () => apiRequest<SummaryResponse>('/expenses/summary', { query: { from, to } }),
  })
}

export function useCreateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ExpenseRequest) =>
      apiRequest<ExpenseResponse>('/expenses', { method: 'POST', body }),
    onSuccess: () => invalidateExpenseData(queryClient),
  })
}

export function useUpdateExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: number; body: ExpenseRequest }) =>
      apiRequest<ExpenseResponse>(`/expenses/${id}`, { method: 'PUT', body }),
    onSuccess: () => invalidateExpenseData(queryClient),
  })
}

export function useDeleteExpense() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => apiRequest<void>(`/expenses/${id}`, { method: 'DELETE' }),
    onSuccess: () => invalidateExpenseData(queryClient),
  })
}
