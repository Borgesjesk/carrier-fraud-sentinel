import apiClient from './client';
import type { Note, NoteRequest } from '../types/Note';

const base = (alertId: string) => `/api/v1/alerts/${alertId}/notes`;

export const noteService = {
  list: async (alertId: string): Promise<Note[]> =>
    (await apiClient.get<Note[]>(base(alertId))).data,

  create: async (alertId: string, request: NoteRequest): Promise<Note> =>
    (await apiClient.post<Note>(base(alertId), request)).data,
};
