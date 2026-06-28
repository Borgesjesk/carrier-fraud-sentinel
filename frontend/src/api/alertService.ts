import apiClient from './client';
import type { Alert } from '../types/Alert';

const BASE = '/api/v1/transactions';

export const alertService = {
  getAll: async (): Promise<Alert[]> =>
    (await apiClient.get<Alert[]>(`${BASE}/alerts`)).data,

  getById: async (alertId: string): Promise<Alert> =>
    (await apiClient.get<Alert>(`${BASE}/alerts/${alertId}`)).data,

  accept: async (alertId: string, assignee: string): Promise<Alert> =>
    (await apiClient.put<Alert>(`${BASE}/alerts/${alertId}/accept`, { assignee })).data,

  investigate: async (alertId: string): Promise<Alert> =>
    (await apiClient.put<Alert>(`${BASE}/alerts/${alertId}/investigate`, {})).data,

  resolve: async (alertId: string, resolution: string): Promise<Alert> =>
    (await apiClient.put<Alert>(`${BASE}/alerts/${alertId}/resolve`, { resolution })).data,

  escalate: async (alertId: string, reason: string): Promise<Alert> =>
    (await apiClient.put<Alert>(`${BASE}/alerts/${alertId}/escalate`, { reason })).data,
};
