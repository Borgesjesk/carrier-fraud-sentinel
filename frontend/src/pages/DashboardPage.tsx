import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, LogOut, AlertCircle, Loader2 } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { alertService } from '../api/alertService';
import { alertReadService } from '../api/alertReadService';
import { MessageCircle } from 'lucide-react';
import type { Alert, Severity } from '../types/Alert';

const SEVERITY_STYLES: Record<Severity, string> = {
  LOW: 'bg-slate-700/50 text-slate-300 border-slate-600',
  MEDIUM: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  HIGH: 'bg-orange-500/15 text-orange-300 border-orange-500/30',
  CRITICAL: 'bg-red-500/15 text-red-300 border-red-500/30',
};

export function DashboardPage() {
  const { user, logout } = useAuth();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [unreadCounts, setUnreadCounts] = useState<Record<string, number>>({});

  useEffect(() => {
      let cancelled = false;

      const load = () => {
        alertService.getAll()
          .then((data) => { if (!cancelled) setAlerts(data); })
          .catch(() => { if (!cancelled) setError('Failed to load alerts. Please refresh.'); })
          .finally(() => { if (!cancelled) setIsLoading(false); });

        alertReadService.unreadCounts()
          .then((counts) => { if (!cancelled) setUnreadCounts(counts); })
          .catch(() => {});
      };

      load();
      const intervalId = setInterval(load, 10_000);

      return () => {
        cancelled = true;
        clearInterval(intervalId);
      };
    }, []);

  const stats = {
    total: alerts.length,
    critical: alerts.filter((a) => a.severity === 'CRITICAL').length,
    high: alerts.filter((a) => a.severity === 'HIGH').length,
    medium: alerts.filter((a) => a.severity === 'MEDIUM').length,
  };

  const formatDate = (iso: string) => {
    return new Date(iso).toLocaleString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
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
          <StatCard label="Total alerts" value={stats.total} />
          <StatCard label="Critical" value={stats.critical} accent="text-red-300" />
          <StatCard label="High" value={stats.high} accent="text-orange-300" />
          <StatCard label="Medium" value={stats.medium} accent="text-amber-300" />
        </div>

        {/* Alerts table */}
        <section>
          <h2 className="text-base font-medium text-slate-200 mb-4">Recent alerts</h2>

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
                    <th className="px-4 py-3 font-medium">Alert ID</th>
                    <th className="px-4 py-3 font-medium">Carrier</th>
                    <th className="px-4 py-3 font-medium">Severity</th>
                    <th className="px-4 py-3 font-medium">Rule</th>
                    <th className="px-4 py-3 font-medium">Department</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium">Created</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {alerts.map((alert) => (
                    <tr key={alert.alertId} className="hover:bg-slate-900/30 transition">
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
                      <td className="px-4 py-3 text-slate-200">{alert.carrierName}</td>
                      <td className="px-4 py-3">
                        <span className={`px-2 py-1 text-xs font-medium rounded border ${SEVERITY_STYLES[alert.severity]}`}>
                          {alert.severity}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-400 text-xs">{alert.triggeredRules}</td>
                      <td className="px-4 py-3 text-slate-300 text-xs">{alert.assignedDepartment}</td>
                      <td className="px-4 py-3 text-slate-300 text-xs">{alert.status}</td>
                      <td className="px-4 py-3 text-slate-400 text-xs">{formatDate(alert.createdDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}

function StatCard({ label, value, accent }: { label: string; value: number; accent?: string }) {
  return (
    <div className="bg-slate-900/50 border border-slate-800 rounded-lg p-4">
      <div className="text-xs text-slate-400 uppercase tracking-wide">{label}</div>
      <div className={`text-2xl font-semibold mt-1 ${accent || 'text-slate-100'}`}>{value}</div>
    </div>
  );
}
