export interface AuthResponse {
  username: string;
  role: Role;
}

export type Role = 'ADMIN' | 'ANALYST' | 'COMPLIANCE';
