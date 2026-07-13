import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, Loader2, AlertCircle, FileSearch, Filter } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import apiClient from '../api/client';

interface AuditEntry {
  id: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details: string;
  ipAddress: string;
  timestamp: string;
}

const ACTION_COLORS: Record<string, string> = {
  LOGIN_SUCCESS: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
  LOGIN_FAILED: 'bg-red-500/15 text-red-300 border-red-500/30',
  LOGOUT: 'bg-slate-700/50 text-slate-300 border-slate-600',
  PASSWORD_CHANGED: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  MFA_ENABLED: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
  MFA_DISABLED: 'bg-red-500/15 text-red-300 border-red-500/30',
  ALERT_CREATED: 'bg-violet-500/15 text-violet-300 border-violet-500/30',
  ALERT_ASSIGNED: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
  ALERT_RESOLVED: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
  ALERT_TRANSFERRED: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
};

export function AuditLogPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [logs, setLogs] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterUser, setFilterUser] = useState('');
  const [filterAction, setFilterAction] = useState('');
  const [limit, setLimit] = useState(100);

  const load = async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { limit };
      if (filterUser) params.username = filterUser;
      if (filterAction) params.action = filterAction;
      const r = await apiClient.get<AuditEntry[]>('/api/v1/audit/logs', { params });
      setLogs(r.data);
    } catch {
      setError('Failed to load audit logs. You may not have permission.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const uniqueActions = Array.from(new Set(logs.map((l) => l.action))).sort();
  const formatTime = (iso: string) => new Date(iso).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
            <span className="text-slate-600">/</span>
            <span className="text-sm text-slate-400">Audit</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/dashboard" className="text-sm text-slate-300 hover:text-slate-100 px-3 py-1.5 rounded-lg hover:bg-slate-800 transition">← Back</Link>
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

      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="flex items-center gap-2 mb-6">
          <FileSearch className="w-6 h-6 text-amber-400" />
          <h2 className="text-2xl font-semibold">Audit trail</h2>
        </div>

        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-4 mb-4 flex items-center gap-3 flex-wrap">
          <Filter className="w-4 h-4 text-slate-400" />
          <input
            type="text"
            value={filterUser}
            onChange={(e) => setFilterUser(e.target.value)}
            placeholder="Filter by username..."
            className="px-3 py-1.5 text-sm bg-slate-800/50 border border-slate-700 rounded-lg text-slate-200"
          />
          <select
            value={filterAction}
            onChange={(e) => setFilterAction(e.target.value)}
            className="px-3 py-1.5 text-sm bg-slate-800/50 border border-slate-700 rounded-lg text-slate-200"
          >
            <option value="">All actions</option>
            {uniqueActions.map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
          <select
            value={limit}
            onChange={(e) => setLimit(parseInt(e.target.value))}
            className="px-3 py-1.5 text-sm bg-slate-800/50 border border-slate-700 rounded-lg text-slate-200"
          >
            <option value="50">Last 50</option>
            <option value="100">Last 100</option>
            <option value="250">Last 250</option>
            <option value="500">Last 500</option>
          </select>
          <button
            onClick={load}
            className="px-3 py-1.5 text-sm bg-sky-500/10 hover:bg-sky-500/20 text-sky-300 border border-sky-500/30 rounded-lg transition"
          >
            Apply
          </button>
        </div>

        {loading && <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-sky-400" /></div>}

        {error && (
          <div className="flex items-start gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
            <AlertCircle className="w-5 h-5 text-red-400" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        {!loading && !error && logs.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <FileSearch className="w-16 h-16 mx-auto mb-3 opacity-30" />
            <p>No audit entries found.</p>
          </div>
        )}

        {!loading && logs.length > 0 && (
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-slate-900/50 text-xs uppercase tracking-wide text-slate-400">
                <tr>
                  <th className="px-4 py-3 font-medium text-left">Time</th>
                  <th className="px-4 py-3 font-medium text-left">User</th>
                  <th className="px-4 py-3 font-medium text-left">Action</th>
                  <th className="px-4 py-3 font-medium text-left">Resource</th>
                  <th className="px-4 py-3 font-medium text-left">Details</th>
                  <th className="px-4 py-3 font-medium text-left">IP</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((l) => (
                  <tr key={l.id} className="border-t border-slate-800 hover:bg-slate-900/30">
                    <td className="px-4 py-3 text-xs text-slate-400 whitespace-nowrap">{formatTime(l.timestamp)}</td>
                    <td className="px-4 py-3 font-mono text-slate-200">{l.username || '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 text-xs font-medium rounded border ${ACTION_COLORS[l.action] || 'bg-slate-700/50 text-slate-300 border-slate-600'}`}>
                        {l.action}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-400">
                      {l.resourceType ? `${l.resourceType}${l.resourceId ? ' · ' + l.resourceId.slice(0, 20) + '...' : ''}` : '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-300 max-w-md truncate">{l.details || '—'}</td>
                    <td className="px-4 py-3 text-xs font-mono text-slate-500">{l.ipAddress || '—'}</td>
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
