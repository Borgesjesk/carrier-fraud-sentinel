import apiClient from './client';
import type { Alert } from '../types/Alert';
import type { ComplaintRequest } from '../types/Complaint';

const BASE = '/api/v1/complaints';

export const complaintService = {
  myComplaints: async (): Promise<Alert[]> =>
    (await apiClient.get<Alert[]>(`${BASE}/mine`)).data,

  submit: async (request: ComplaintRequest, documents: File[], categories: string[]): Promise<Alert> => {
    const formData = new FormData();
    formData.append(
      'complaint',
      new Blob([JSON.stringify(request)], { type: 'application/json' })
    );
    documents.forEach((file) => formData.append('documents', file));
    categories.forEach((category) => formData.append('categories', category));

    const response = await apiClient.post<Alert>(BASE, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },
};
