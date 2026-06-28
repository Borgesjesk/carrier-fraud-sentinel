import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, Loader2, AlertCircle, Plus, FileText } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { complaintService } from '../api/complaintService';
import type { Alert, AlertStatus } from '../types/Alert';

const STATUS_STYLES: Record<AlertStatus, string> = {
  UNASSIGNED: 'bg-slate-700/50 text-slate-300',
  ASSIGNED: 'bg-sky-500/15 text-sky-300',
  ACCEPTED: 'bg-sky-500/15 text-sky-300',
  IN_PROGRESS: 'bg-violet-500/15 text-violet-300',
  RESOLVED: 'bg-emerald-500/15 text-emerald-300',
  ESCALATED: 'bg-red-500/15 text-red-300',
};

const POLL_INTERVAL_MS = 30_000;

export function MyComplaintsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = () => {
      complaintService.myComplaints()
        .then((data) => { if (!cancelled) setAlerts(data); })
        .catch(() => { if (!cancelled) setError('Failed to load your cases.'); })
        .finally(() => { if (!cancelled) setIsLoading(false); });
    };

    load();
    const intervalId = setInterval(load, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, []);

  const formatDate = (iso: string) => new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <div className="text-sm font-medium text-slate-200">{user?.username}</div>
              <div className="text-xs text-slate-400">CLIENT</div>
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

      <main className="max-w-5xl mx-auto px-6 py-8">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-2xl font-semibold tracking-tight">My cases</h2>
            <p className="text-sm text-slate-400 mt-1">Auto-refreshes every 30 seconds.</p>
          </div>
          <button
            onClick={() => navigate('/complaints/new')}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white text-sm font-medium rounded-lg transition shadow-lg shadow-sky-500/20"
          >
            <Plus className="w-4 h-4" />
            <span>New complaint</span>
          </button>
        </div>

        {isLoading && (
          <div className="flex items-center gap-2 text-slate-400 py-12 justify-center">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Loading your cases...</span>
          </div>
        )}

        {error && !isLoading && (
          <div className="flex items-start gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
            <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        {!isLoading && !error && alerts.length === 0 && (
          <div className="text-center py-16 bg-slate-900/30 border border-slate-800 rounded-lg">
            <FileText className="w-10 h-10 text-slate-600 mx-auto mb-3" />
            <p className="text-slate-400 text-sm mb-4">You haven't submitted any complaints yet.</p>
            <button
              onClick={() => navigate('/complaints/new')}
              className="text-sky-400 hover:text-sky-300 text-sm font-medium"
            >
              Submit your first complaint →
            </button>
          </div>
        )}

        {!isLoading && !error && alerts.length > 0 && (
          <div className="space-y-3">
            {alerts.map((alert) => (
              <div key={alert.alertId} className="bg-slate-900/50 border border-slate-800 rounded-lg p-4 hover:border-slate-700 transition">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-slate-500 font-mono mb-1">{alert.alertId}</p>
                    <h3 className="text-base font-medium text-slate-100 mb-1">{alert.carrierName}</h3>
                    <p className="text-xs text-slate-400">
                      {alert.triggeredRules.replace('ClientComplaint:', '')} · {alert.assignedDepartment}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <span className={`px-2 py-1 text-xs font-medium rounded ${STATUS_STYLES[alert.status]}`}>
                      {alert.status}
                    </span>
                    <span className="text-xs text-slate-500">{formatDate(alert.createdDate)}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
