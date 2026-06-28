import apiClient from './client';
import type { Comment, CommentRequest } from '../types/Comment';

const base = (alertId: string) => `/api/v1/alerts/${alertId}/comments`;

export const commentService = {
  list: async (alertId: string): Promise<Comment[]> =>
    (await apiClient.get<Comment[]>(base(alertId))).data,

  create: async (alertId: string, request: CommentRequest): Promise<Comment> =>
    (await apiClient.post<Comment>(base(alertId), request)).data,
};
