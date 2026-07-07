import axios, { AxiosError } from 'axios';
import type { ProblemDetail } from '../types/ProblemDetail';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor: auto-refresh on 401 then retry, dispatch event if refresh fails
let isRefreshing = false;
let pendingRequests: Array<() => void> = [];

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ProblemDetail>) => {
    const originalRequest = error.config as any;

    if (error.response?.status !== 401 || originalRequest._retry) {
      if (error.response?.status === 401) {
        window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      }
      return Promise.reject(error);
    }

    // Never refresh on the auth endpoints themselves
    if (originalRequest.url?.includes('/api/v1/auth/')) {
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    if (isRefreshing) {
      // Wait for the in-flight refresh
      return new Promise((resolve) => {
        pendingRequests.push(() => resolve(apiClient(originalRequest)));
      });
    }

    isRefreshing = true;

    try {
      await apiClient.post('/api/v1/auth/refresh');
      pendingRequests.forEach((cb) => cb());
      pendingRequests = [];
      return apiClient(originalRequest);
    } catch (refreshError) {
      pendingRequests = [];
      window.dispatchEvent(new CustomEvent('auth:unauthorized'));
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default apiClient;
