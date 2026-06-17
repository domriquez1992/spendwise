import { useQuery } from '@tanstack/react-query'
import { apiRequest } from '../lib/api'
import type { NotificationResponse } from '../types/api'

export function useNotifications() {
  return useQuery({
    queryKey: ['notifications'],
    queryFn: () => apiRequest<NotificationResponse[]>('/notifications'),
  })
}
