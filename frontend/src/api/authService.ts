import apiClient from './client';
import type { AuthResponse } from '../types/AuthResponse';
import type { LoginRequest } from '../types/LoginRequest';

const AUTH_BASE = '/api/v1/auth';

export type LoginResponse =
  | { mfaRequired: true }
  | AuthResponse;

export const authService = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>(`${AUTH_BASE}/login`, credentials);
    return response.data;
  },

  async loginMfa(credentials: LoginRequest, code: number): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>(
      `${AUTH_BASE}/login/mfa`,
      { ...credentials, code }
    );
    return response.data;
  },

  async me(): Promise<AuthResponse> {
    const response = await apiClient.get<AuthResponse>(`${AUTH_BASE}/me`);
    return response.data;
  },

  async logout(): Promise<void> {
    await apiClient.post(`${AUTH_BASE}/logout`);
  },
};
