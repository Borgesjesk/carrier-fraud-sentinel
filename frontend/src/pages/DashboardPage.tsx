import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, AlertCircle, Loader2, Shield, Zap, BarChart3, Download, User, Users } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { alertService } from '../api/alertService';
import { alertReadService } from '../api/alertReadService';
import { AlertTriangle } from 'lucide-react';
import { MessageCircle } from 'lucide-react';
import { useRef } from 'react';
import { Toast } from '../components/Toast';
import { NotificationBell } from '../components/NotificationBell';
import type { Alert, Severity } from '../types/Alert';


function downloadCSV(alerts: Alert[]) {
  const headers = ['Alert ID', 'Carrier', 'Reporter', 'Severity', 'Status', 'Department', 'Rule', 'Risk Score', 'Created'];
  const rows = alerts.map((a) => [
    a.alertId,
    a.carrierName,
    a.createdBy || '',
    a.severity,
    a.status,
    a.assignedDepartment,
    a.triggeredRules,
    Math.min(100, (a.riskScore / 3) * 100).toFixed(0) + '%',
    new Date(a.createdDate).toLocaleString('en-GB'),
  ]);
  const csv = [headers, ...rows].map((r) => r.map((c) => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `fraudsentinel-alerts-${new Date().toISOString().slice(0, 10)}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

const SEVERITY_STYLES: Record<Severity, string> = {
  LOW: 'bg-slate-700/50 text-slate-300 border-slate-600',
  MEDIUM: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  HIGH: 'bg-orange-500/15 text-orange-300 border-orange-500/30',
  CRITICAL: 'bg-red-500/15 text-red-300 border-red-500/30',
};

const STATUS_STYLES: Record<string, string> = {
  UNASSIGNED: 'bg-slate-700/50 text-slate-300 border-slate-600/50',
  ASSIGNED: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
  ACCEPTED: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
  IN_PROGRESS: 'bg-violet-500/15 text-violet-300 border-violet-500/30',
  RESOLVED: 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30',
  ESCALATED: 'bg-red-500/15 text-red-300 border-red-500/30',
};

export function DashboardPage() {
  const { user, logout } = useAuth();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [unreadCounts, setUnreadCounts] = useState<Record<string, number>>({});
  const [filter, setFilter] = useState<'all' | 'mine' | 'unassigned' | 'stale'>('all');
  const [severityFilter, setSeverityFilter] = useState<string | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkAction, setBulkAction] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const previousCountsRef = useRef<Record<string, number>>({});
  const isFirstLoadRef = useRef(true);

  useEffect(() => {
      let cancelled = false;

      const load = () => {
        alertService.getAll()
          .then((data) => { if (!cancelled) setAlerts(data); })
          .catch(() => { if (!cancelled) setError('Failed to load alerts. Please refresh.'); })
          .finally(() => { if (!cancelled) setIsLoading(false); });

        alertReadService.unreadCounts()
                .then((counts) => {
                  if (cancelled) return;

                  if (!isFirstLoadRef.current) {
                    const totalNew = Object.entries(counts).reduce((acc, [alertId, count]) => {
                      const previous = previousCountsRef.current[alertId] ?? 0;
                      return acc + Math.max(0, count - previous);
                    }, 0);

                    if (totalNew > 0) {
                      setToastMessage(`${totalNew} new comment${totalNew > 1 ? 's' : ''} received`);
                    }
                  }

                  previousCountsRef.current = counts;
                  isFirstLoadRef.current = false;
                  setUnreadCounts(counts);
                })
                .catch(() => {});
      };

      load();
      const intervalId = setInterval(load, 10_000);

      return () => {
        cancelled = true;
        clearInterval(intervalId);
      };
    }, []);

  const filteredAlerts = alerts.filter((alert) => {
      if (filter === 'mine' && alert.assignedTo !== user?.username) return false;
      if (filter === 'unassigned' && alert.status !== 'UNASSIGNED') return false;
      if (filter === 'stale' && !alert.isStale) return false;
      if (severityFilter && alert.severity !== severityFilter) return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const carrier = alert.carrierName?.toLowerCase() || '';
        const reporter = alert.createdBy?.toLowerCase() || '';
        if (!carrier.includes(q) && !reporter.includes(q)) return false;
      }
      return true;
  });

  const stats = {
    total: alerts.length,
    critical: alerts.filter((a) => a.severity === 'CRITICAL').length,
    high: alerts.filter((a) => a.severity === 'HIGH').length,
    medium: alerts.filter((a) => a.severity === "MEDIUM").length,
    unassigned: alerts.filter((a) => a.status === "UNASSIGNED").length,
    stale: alerts.filter((a) => a.isStale === true).length,
  };

  const formatDate = (iso: string) => {
    return new Date(iso).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  };

const toggleSelect = (id: string) => {
    const next = new Set(selectedIds);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelectedIds(next);
  };

  const selectAll = () => {
    if (selectedIds.size === filteredAlerts.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredAlerts.map((a) => a.alertId)));
    }
  };

  const handleBulkResolve = async () => {
    const resolution = prompt('Resolution summary for all selected:');
    if (!resolution) return;
    setBulkAction('resolve');
    try {
      await Promise.all(Array.from(selectedIds).map((id) => alertService.resolve(id, resolution)));
      setSelectedIds(new Set());
      loadAlerts();
    } finally {
      setBulkAction(null);
    }
  };

  const handleBulkAccept = async () => {
    if (!user?.username) return;
    setBulkAction('accept');
    try {
      await Promise.all(Array.from(selectedIds).map((id) => alertService.accept(id, user.username)));
      setSelectedIds(new Set());
      loadAlerts();
    } finally {
      setBulkAction(null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {toastMessage && (
              <Toast
                message={toastMessage}
                onClose={() => setToastMessage(null)}
                onClick={() => {
                  const alertWithMostUnread = Object.entries(unreadCounts)
                    .filter(([, count]) => count > 0)
                    .sort(([, a], [, b]) => b - a)[0];
                  if (alertWithMostUnread) {
                    navigate(`/alerts/${alertWithMostUnread[0]}`);
                  }
                }}
              />
            )}
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
          </div>

          <div className="flex items-center gap-4">
            <div className="text-right">
              <div className="text-sm font-medium text-slate-200">{user?.username}</div>
              <div className="text-xs text-slate-400">{user?.role}</div>
            </div>
            <NotificationBell />
            {user?.role === 'ADMIN' && (
                          <Link
                            to="/admin/users"
                            className="flex items-center gap-2 px-3 py-1.5 text-sm text-red-300 hover:text-red-100 hover:bg-slate-800 rounded-lg transition"
                          >
                            <Users className="w-4 h-4" />
                            <span>Admin</span>
                          </Link>
                        )}
                        <Link
                          to="/settings/profile"
                          className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition"
                        >
                          <User className="w-4 h-4" />
                          <span>Profile</span>
                        </Link>
                        <Link
                          to="/analytics"
                          className="flex items-center gap-2 px-3 py-1.5 text-sm text-violet-300 hover:text-violet-100 hover:bg-slate-800 rounded-lg transition"
                        >
                          <BarChart3 className="w-4 h-4" />
                          <span>Analytics</span>
                        </Link>
            <Link
                          to="/simulate"
                          className="flex items-center gap-2 px-3 py-1.5 text-sm text-violet-300 hover:text-violet-100 hover:bg-slate-800 rounded-lg transition"
                        >
                          <Zap className="w-4 h-4" />
                          <span>Simulate</span>
                        </Link>
            <Link
                          to="/settings/mfa"
                          className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition"
                        >
                          <Shield className="w-4 h-4" />
                          <span>MFA</span>
                        </Link>
                        <button
                          onClick={() => logout()}
              className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        {/* Stats grid */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          <StatCard
                      label="Total alerts"
                      value={stats.total}
                      onClick={() => { setFilter('all'); setSeverityFilter(null); }}
                      active={filter === 'all' && !severityFilter}
                    />
                    <StatCard
                      label="Critical"
                      value={stats.critical}
                      accent="text-red-300"
                      onClick={() => { setFilter('all'); setSeverityFilter('CRITICAL'); }}
                      active={severityFilter === 'CRITICAL'}
                    />
                    <StatCard
                      label="High"
                      value={stats.high}
                      accent="text-orange-300"
                      onClick={() => { setFilter('all'); setSeverityFilter('HIGH'); }}
                      active={severityFilter === 'HIGH'}
                    />
                    <StatCard
                      label="Medium"
                      value={stats.medium}
                      accent="text-amber-300"
                      onClick={() => { setFilter('all'); setSeverityFilter('MEDIUM'); }}
                      active={severityFilter === 'MEDIUM'}
                    />
                    <StatCard
                      label="Unassigned"
                      value={stats.unassigned}
                      accent="text-slate-300"
                      onClick={() => { setFilter('unassigned'); setSeverityFilter(null); }}
                      active={filter === 'unassigned'}
                    />
                    <StatCard
                      label="Stale (>72h)"
                      value={stats.stale}
                      accent="text-amber-400"
                      onClick={() => { setFilter('stale'); setSeverityFilter(null); }}
                      active={filter === 'stale'}
                    />
        </div>

        {/* Alerts table */}
        <section>
          <div className="flex items-center justify-between mb-4 gap-3 flex-wrap">
                                <h2 className="text-base font-medium text-slate-200">Recent alerts</h2>
                                <div className="flex-1 max-w-xs">
                                  <input
                                    type="text"
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    placeholder="Search carrier or reporter..."
                                    className="w-full px-3 py-1.5 text-sm bg-slate-900/50 border border-slate-800 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-sky-500"
                                  />
                                </div>
                      <button
                      onClick={() => downloadCSV(filteredAlerts)}
                      disabled={filteredAlerts.length === 0}
                      className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-emerald-300 hover:text-emerald-100 hover:bg-emerald-500/10 border border-emerald-500/30 rounded-lg transition disabled:opacity-30 disabled:cursor-not-allowed"
                                                                >
                      <Download className="w-3.5 h-3.5" />
                      <span>Export CSV</span>
                      </button>
                      <div className="flex gap-1 bg-slate-900/50 border border-slate-800 rounded-lg p-1">
                        {(['all', 'unassigned', 'mine', 'stale'] as const).map((f) => (
                          <button
                            key={f}
                            onClick={() => setFilter(f)}
                            className={`px-3 py-1 text-xs font-medium rounded transition ${
                              filter === f
                                ? 'bg-sky-500/20 text-sky-300'
                                : 'text-slate-400 hover:text-slate-200'
                            }`}
                          >
                            {f === 'all' ? 'All' : f === 'unassigned' ? 'Unassigned' : f === 'mine' ? 'Mine' : 'Stale'}
                            {f === 'unassigned' && alerts.filter((a) => a.status === 'UNASSIGNED').length > 0 && (
                              <span className="ml-1 text-slate-500">
                                ({alerts.filter((a) => a.status === 'UNASSIGNED').length})
                              </span>
                            )}
                            {f === 'mine' && alerts.filter((a) => a.assignedTo === user?.username).length > 0 && (
                              <span className="ml-1 text-slate-500">
                                ({alerts.filter((a) => a.assignedTo === user?.username).length})
                              </span>
                            )}
                            {f === 'stale' && alerts.filter((a) => a.isStale).length > 0 && (
                              <span className="ml-1 text-slate-500">
                                ({alerts.filter((a) => a.isStale).length})
                              </span>
                            )}
                          </button>
                        ))}
                      </div>
                    </div>

          {isLoading && (
            <div className="flex items-center gap-2 text-slate-400 py-12 justify-center">
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Loading alerts...</span>
            </div>
          )}

          {error && !isLoading && (
            <div className="flex items-start gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
              <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
              <p className="text-sm text-red-300">{error}</p>
            </div>
          )}

          {!isLoading && !error && alerts.length === 0 && (
            <div className="text-center py-12 text-slate-400 text-sm">
              No alerts assigned to your visible departments.
            </div>
          )}

          {!isLoading && !error && alerts.length > 0 && (
            <div className="border border-slate-800 rounded-lg overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-slate-900/50">
                  <tr className="text-left text-xs uppercase tracking-wide text-slate-400">
                    <th className="px-4 py-3 font-medium w-8">
                      <input
                        type="checkbox"
                        checked={selectedIds.size > 0 && selectedIds.size === filteredAlerts.length}
                        onChange={selectAll}
                        className="w-4 h-4 rounded border-slate-600 bg-slate-800 text-sky-500"
                      />
                    </th>
                    <th className="px-4 py-3 font-medium">Alert ID</th>
                    <th className="px-4 py-3 font-medium">Carrier accused</th>
                    <th className="px-4 py-3 font-medium">Reported by</th>
                    <th className="px-4 py-3 font-medium">Severity</th>
                    <th className="px-4 py-3 font-medium">Rule</th>
                    <th className="px-4 py-3 font-medium">Department</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Created</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {filteredAlerts.map((alert) => (
                    <tr key={alert.alertId} className={`hover:bg-slate-900/30 transition ${selectedIds.has(alert.alertId) ? 'bg-sky-500/5' : ''}`}>
                      <td className="px-4 py-3 w-8">
                        <input
                          type="checkbox"
                          checked={selectedIds.has(alert.alertId)}
                          onChange={() => toggleSelect(alert.alertId)}
                          className="w-4 h-4 rounded border-slate-600 bg-slate-800 text-sky-500"
                        />
                      </td>
                      <td className="px-4 py-3 font-mono text-xs">
                        <div className="flex items-center gap-2">
                          <Link to={`/alerts/${alert.alertId}`} className="text-sky-400 hover:text-sky-300 transition">
                            {alert.alertId.slice(0, 24)}...
                          </Link>
                          {unreadCounts[alert.alertId] > 0 && (
                              <span className="flex items-center gap-1 px-1.5 py-0.5 text-xs font-medium bg-sky-500/20 text-sky-300 border border-sky-500/30 rounded">
                                <MessageCircle className="w-3 h-3" />
                                {unreadCounts[alert.alertId]}
                              </span>
                            )}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-red-300">{alert.carrierName}</span>
                      </td>
                      <td className="px-4 py-3">
                        {alert.createdBy ? <span className="text-emerald-300">{alert.createdBy}</span> : <span className="text-slate-500">—</span>}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 text-xs font-medium rounded border ${SEVERITY_STYLES[alert.severity]}`}>
                          {alert.severity}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-400 text-xs">{alert.triggeredRules}</td>
                      <td className="px-4 py-3 text-slate-300 text-xs">{alert.assignedDepartment}</td>
                      <td className="px-4 py-3"><div className="flex items-center gap-1.5"><span className={`px-2 py-1 text-xs font-medium rounded border ${STATUS_STYLES[alert.status] || STATUS_STYLES.UNASSIGNED}`}>{alert.status}</span>{alert.isStale && (<AlertTriangle className="w-3.5 h-3.5 text-amber-400" />)}</div></td>
                      <td className="px-4 py-3 text-slate-400 text-xs">{formatDate(alert.createdDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
              </main>

              {selectedIds.size > 0 && (
                <div className="fixed bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-3 bg-slate-800 border border-slate-700 rounded-xl shadow-2xl px-4 py-3 z-40">
                  <span className="text-sm text-slate-300">
                    <strong className="text-sky-400">{selectedIds.size}</strong> selected
                  </span>
                  <div className="w-px h-6 bg-slate-700" />
                  <button
                    onClick={handleBulkAccept}
                    disabled={bulkAction !== null}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-sm bg-sky-500/10 hover:bg-sky-500/20 text-sky-300 border border-sky-500/30 rounded-lg transition disabled:opacity-30"
                  >
                    {bulkAction === 'accept' && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    <span>Accept</span>
                  </button>
                  <button
                    onClick={handleBulkResolve}
                    disabled={bulkAction !== null}
                    className="flex items-center gap-1.5 px-3 py-1.5 text-sm bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-lg transition disabled:opacity-30"
                  >
                    {bulkAction === 'resolve' && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    <span>Resolve</span>
                  </button>
                  <button
                    onClick={() => setSelectedIds(new Set())}
                    className="px-3 py-1.5 text-sm text-slate-400 hover:text-slate-200 transition"
                  >
                    Clear
                  </button>
                </div>
              )}
    </div>
  );
}

function StatCard({ label, value, accent, onClick, active }: {
  label: string;
  value: number;
  accent?: string;
  onClick?: () => void;
  active?: boolean;
}) {
  const clickable = !!onClick;
  return (
    <div
      onClick={onClick}
      className={`bg-slate-900/50 border rounded-lg p-4 transition ${
        clickable ? 'cursor-pointer hover:bg-slate-900/80 hover:border-sky-500/50' : ''
      } ${active ? 'border-sky-500/70 bg-sky-500/10' : 'border-slate-800'}`}
    >
      <div className="text-xs text-slate-400 uppercase tracking-wide">{label}</div>
      <div className={`text-2xl font-semibold mt-1 ${accent || 'text-slate-100'}`}>{value}</div>
    </div>
  );
}
