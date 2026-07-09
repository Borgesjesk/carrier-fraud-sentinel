import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authService } from '../api/authService';
import type { AuthResponse } from '../types/AuthResponse';
import type { LoginRequest } from '../types/LoginRequest';

type AuthStatus = 'unknown' | 'authenticated' | 'unauthenticated';

interface AuthContextValue {
  status: AuthStatus;
  user: AuthResponse | null;
  login: (credentials: LoginRequest) => Promise<{ mfaRequired: boolean }>;
  loginMfa: (credentials: LoginRequest, code: number) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('unknown');
  const [user, setUser] = useState<AuthResponse | null>(null);

  // Bootstrap: on mount, ask backend "am I authenticated?"
  useEffect(() => {
    authService.me()
      .then((authResponse) => {
        setUser(authResponse);
        setStatus('authenticated');
      })
      .catch(() => {
        setUser(null);
        setStatus('unauthenticated');
      });
  }, []);

  // Listen to global 401 events from axios interceptor
  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
      setStatus('unauthenticated');
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (credentials: LoginRequest): Promise<{ mfaRequired: boolean }> => {
    const response = await authService.login(credentials);
    if ('mfaRequired' in response) {
      return { mfaRequired: true };
    }
    setUser(response);
    setStatus('authenticated');
    return { mfaRequired: false };
  };

  const loginMfa = async (credentials: LoginRequest, code: number): Promise<void> => {
    const authResponse = await authService.loginMfa(credentials, code);
    setUser(authResponse);
    setStatus('authenticated');
  };

  const logout = async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
      setStatus('unauthenticated');
    }
  };

  return (
    <AuthContext.Provider value={{ status, user, login, loginMfa, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
