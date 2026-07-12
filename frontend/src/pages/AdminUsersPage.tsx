import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, Users, Loader2, ShieldOff, RotateCcw, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import apiClient from '../api/client';

interface AdminUser {
  username: string;
  email: string;
  role: string;
  mfaEnabled: boolean;
  knownIpsCount: number;
  createdAt: string;
}

const ROLE_STYLES: Record<string, string> = {
  ADMIN: 'bg-red-500/15 text-red-300 border-red-500/30',
  COMPLIANCE: 'bg-violet-500/15 text-violet-300 border-violet-500/30',
  ANALYST: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
  CLIENT: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
};

export function AdminUsersPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const r = await apiClient.get<AdminUser[]>('/api/v1/profile/admin/users');
      setUsers(r.data);
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { detail?: string } } };
      setError(errObj?.response?.data?.detail || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleDisableMfa = async (username: string) => {
    if (!window.confirm(`Disable MFA for ${username}?`)) return;
    setBusy(username + '-mfa');
    setError(null);
    try {
      await apiClient.post(`/api/v1/profile/admin/users/${username}/mfa/disable`);
      setMessage(`MFA disabled for ${username}`);
      load();
    } catch {
      setError(`Failed to disable MFA for ${username}`);
    } finally {
      setBusy(null);
    }
  };

  const handleResetIps = async (username: string) => {
    if (!window.confirm(`Reset all known IPs for ${username}?`)) return;
    setBusy(username + '-ips');
    setError(null);
    try {
      await apiClient.post(`/api/v1/profile/admin/users/${username}/reset-ips`);
      setMessage(`IPs cleared for ${username}`);
      load();
    } catch {
      setError(`Failed to reset IPs for ${username}`);
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
            <span className="text-slate-600">/</span>
            <span className="text-sm text-slate-400">Admin: Users</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/dashboard" className="text-sm text-slate-300 hover:text-slate-100 px-3 py-1.5 rounded-lg hover:bg-slate-800 transition">← Back</Link>
            <div className="text-right">
              <div className="text-sm font-medium">{user?.username}</div>
              <div className="text-xs text-slate-400">{user?.role}</div>
            </div>
            <button
              onClick={() => { logout(); navigate('/login'); }}
              className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex items-center gap-2 mb-6">
          <Users className="w-6 h-6 text-red-400" />
          <h2 className="text-2xl font-semibold">User management</h2>
        </div>

        {loading && <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-sky-400" /></div>}

        {message && (
          <div className="flex items-start gap-2 p-3 mb-4 bg-emerald-500/10 border border-emerald-500/30 rounded-lg">
            <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5" />
            <p className="text-sm text-emerald-300">{message}</p>
          </div>
        )}

        {error && (
          <div className="flex items-start gap-2 p-3 mb-4 bg-red-500/10 border border-red-500/30 rounded-lg">
            <AlertCircle className="w-5 h-5 text-red-400 mt-0.5" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        {!loading && users.length > 0 && (
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-slate-900/50 text-xs uppercase tracking-wide text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium text-left">Username</th>
                  <th className="px-4 py-3 font-medium text-left">Email</th>
                  <th className="px-4 py-3 font-medium text-left">Role</th>
                  <th className="px-4 py-3 font-medium text-left">MFA</th>
                  <th className="px-4 py-3 font-medium text-left">Known IPs</th>
                  <th className="px-4 py-3 font-medium text-left">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.username} className="border-t border-slate-800 hover:bg-slate-900/30">
                    <td className="px-4 py-3 font-mono text-slate-200">{u.username}</td>
                    <td className="px-4 py-3 text-slate-400 text-xs">{u.email || '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 text-xs font-medium rounded border ${ROLE_STYLES[u.role] || ROLE_STYLES.CLIENT}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={u.mfaEnabled ? 'text-emerald-300 text-xs' : 'text-slate-500 text-xs'}>
                        {u.mfaEnabled ? '✓ Enabled' : 'Off'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-300 text-xs">{u.knownIpsCount}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        {u.mfaEnabled && (
                          <button
                            onClick={() => handleDisableMfa(u.username)}
                            disabled={busy === u.username + '-mfa'}
                            className="flex items-center gap-1 px-2 py-1 text-xs bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30 rounded transition disabled:opacity-30"
                            title="Disable MFA"
                          >
                            {busy === u.username + '-mfa' ? <Loader2 className="w-3 h-3 animate-spin" /> : <ShieldOff className="w-3 h-3" />}
                            <span>MFA</span>
                          </button>
                        )}
                        {u.knownIpsCount > 0 && (
                          <button
                            onClick={() => handleResetIps(u.username)}
                            disabled={busy === u.username + '-ips'}
                            className="flex items-center gap-1 px-2 py-1 text-xs bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/30 rounded transition disabled:opacity-30"
                            title="Reset known IPs"
                          >
                            {busy === u.username + '-ips' ? <Loader2 className="w-3 h-3 animate-spin" /> : <RotateCcw className="w-3 h-3" />}
                            <span>IPs</span>
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
