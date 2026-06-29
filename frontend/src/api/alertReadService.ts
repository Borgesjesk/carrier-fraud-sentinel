import apiClient from './client';

export const alertReadService = {
  markAsRead: async (alertId: string): Promise<void> => {
    await apiClient.post(`/api/v1/alerts/${alertId}/read`);
  },

  unreadCounts: async (): Promise<Record<string, number>> => {
    return (await apiClient.get<Record<string, number>>('/api/v1/alerts/unread-counts')).data;
  },
};
