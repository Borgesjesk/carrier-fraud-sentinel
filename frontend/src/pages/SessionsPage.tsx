import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, Loader2, AlertCircle, CheckCircle2, Ban, Key } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import apiClient from '../api/client';

interface Session {
  tokenId: string;
  issuedAt: string;
  expiresAt: string;
  valid: boolean;
}

export function SessionsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const r = await apiClient.get<Session[]>('/api/v1/profile/sessions');
      setSessions(r.data);
    } catch {
      setError('Failed to load sessions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleRevoke = async (tokenId: string) => {
    if (!window.confirm('Revoke this session?')) return;
    setBusy(tokenId);
    try {
      await apiClient.post(`/api/v1/profile/sessions/${tokenId}/revoke`);
      setMessage('Session revoked');
      load();
    } catch {
      setError('Failed to revoke session');
    } finally {
      setBusy(null);
    }
  };

  const formatDate = (iso: string) => new Date(iso).toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });

  const validSessions = sessions.filter((s) => s.valid);
  const revokedSessions = sessions.filter((s) => !s.valid);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
            <span className="text-slate-600">/</span>
            <span className="text-sm text-slate-400">Sessions</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/settings/profile" className="text-sm text-slate-300 hover:text-slate-100 px-3 py-1.5 rounded-lg hover:bg-slate-800 transition">← Profile</Link>
            <div className="text-right">
              <div className="text-sm font-medium">{user?.username}</div>
              <div className="text-xs text-slate-400">{user?.role}</div>
            </div>
            <button onClick={() => { logout(); navigate('/login'); }} className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition">
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-8">
        <div className="flex items-center gap-2 mb-6">
          <Key className="w-6 h-6 text-sky-400" />
          <h2 className="text-2xl font-semibold">Active sessions</h2>
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

        {!loading && (
          <>
            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 mb-4">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">
                Active ({validSessions.length})
              </h3>
              {validSessions.length === 0 ? (
                <p className="text-sm text-slate-500">No active sessions.</p>
              ) : (
                <div className="space-y-2">
                  {validSessions.map((s) => (
                    <div key={s.tokenId} className="flex items-center justify-between p-3 bg-slate-800/30 border border-slate-800 rounded-lg">
                      <div>
                        <div className="text-xs font-mono text-slate-400">{s.tokenId.slice(0, 16)}...</div>
                        <div className="text-xs text-slate-500 mt-1">
                          Issued {formatDate(s.issuedAt)} · Expires {formatDate(s.expiresAt)}
                        </div>
                      </div>
                      <button
                        onClick={() => handleRevoke(s.tokenId)}
                        disabled={busy === s.tokenId}
                        className="flex items-center gap-1 px-3 py-1.5 text-xs bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30 rounded transition disabled:opacity-30"
                      >
                        {busy === s.tokenId ? <Loader2 className="w-3 h-3 animate-spin" /> : <Ban className="w-3 h-3" />}
                        <span>Revoke</span>
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {revokedSessions.length > 0 && (
              <div className="bg-slate-900/30 border border-slate-800 rounded-xl p-6">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500 mb-4">
                  Revoked ({revokedSessions.length})
                </h3>
                <div className="space-y-2">
                  {revokedSessions.slice(0, 5).map((s) => (
                    <div key={s.tokenId} className="p-3 opacity-50">
                      <div className="text-xs font-mono text-slate-500">{s.tokenId.slice(0, 16)}...</div>
                      <div className="text-xs text-slate-600 mt-1">Issued {formatDate(s.issuedAt)}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
