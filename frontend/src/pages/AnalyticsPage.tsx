import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { PieChart, Pie, Cell, BarChart, Bar, LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend, CartesianGrid } from 'recharts';
import { ShieldCheck, LogOut, BarChart3, Loader2, AlertCircle } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { alertService } from '../api/alertService';
import type { Alert } from '../types/Alert';

const SEVERITY_COLORS: Record<string, string> = {
  LOW: '#10b981',
  MEDIUM: '#f59e0b',
  HIGH: '#f97316',
  CRITICAL: '#ef4444',
};

const DEPT_COLOR = '#8b5cf6';
const TREND_COLOR = '#0ea5e9';
const CARRIER_COLOR = '#ec4899';

export function AnalyticsPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    alertService.getAll()
      .then(setAlerts)
      .catch(() => setError('Failed to load analytics'))
      .finally(() => setLoading(false));
  }, []);

  const severityData = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((s) => ({
    name: s,
    value: alerts.filter((a) => a.severity === s).length,
  })).filter((d) => d.value > 0);

  const deptCounts: Record<string, number> = {};
  alerts.forEach((a) => {
    if (a.assignedDepartment) deptCounts[a.assignedDepartment] = (deptCounts[a.assignedDepartment] || 0) + 1;
  });
  const departmentData = Object.entries(deptCounts)
    .map(([name, value]) => ({ name: name.replace('_', ' '), value }))
    .sort((a, b) => b.value - a.value);

  const carrierCounts: Record<string, number> = {};
  alerts.forEach((a) => {
    if (a.carrierName) carrierCounts[a.carrierName] = (carrierCounts[a.carrierName] || 0) + 1;
  });
  const topCarriers = Object.entries(carrierCounts)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 5);

  const last30Days: Record<string, number> = {};
  for (let i = 29; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(5, 10);
    last30Days[key] = 0;
  }
  alerts.forEach((a) => {
    if (!a.createdDate) return;
    const key = a.createdDate.slice(5, 10);
    if (key in last30Days) last30Days[key]++;
  });
  const trendData = Object.entries(last30Days).map(([date, count]) => ({ date, count }));

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
            <span className="text-slate-600">/</span>
            <span className="text-sm text-slate-400">Analytics</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/dashboard" className="text-sm text-slate-300 hover:text-slate-100 px-3 py-1.5 rounded-lg hover:bg-slate-800 transition">
              Back to dashboard
            </Link>
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

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center gap-2 mb-6">
          <BarChart3 className="w-6 h-6 text-violet-400" />
          <h2 className="text-2xl font-semibold tracking-tight">Fraud analytics</h2>
        </div>

        {loading && (
          <div className="flex justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-sky-400" />
          </div>
        )}

        {error && (
          <div className="flex items-start gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
            <AlertCircle className="w-5 h-5 text-red-400" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        {!loading && !error && alerts.length === 0 && (
          <div className="text-center py-20 text-slate-500">
            <BarChart3 className="w-16 h-16 mx-auto mb-3 opacity-30" />
            <p>No alerts to analyze yet.</p>
          </div>
        )}

        {!loading && !error && alerts.length > 0 && (
          <div className="grid md:grid-cols-2 gap-6">
            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Alerts by severity</h3>
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie data={severityData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label>
                    {severityData.map((entry, i) => (
                      <Cell key={i} fill={SEVERITY_COLORS[entry.name]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>

            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Alerts by department</h3>
              <ResponsiveContainer width="100%" height={280}>
                <BarChart data={departmentData} layout="vertical" margin={{ left: 30 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis type="number" stroke="#64748b" />
                  <YAxis dataKey="name" type="category" stroke="#64748b" width={120} tick={{ fontSize: 11 }} />
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                  <Bar dataKey="value" fill={DEPT_COLOR} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 md:col-span-2">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Alerts trend last 30 days</h3>
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={trendData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis dataKey="date" stroke="#64748b" tick={{ fontSize: 11 }} />
                  <YAxis stroke="#64748b" allowDecimals={false} />
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                  <Line type="monotone" dataKey="count" stroke={TREND_COLOR} strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 md:col-span-2">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Top 5 accused carriers</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={topCarriers} layout="vertical" margin={{ left: 30 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                  <XAxis type="number" stroke="#64748b" allowDecimals={false} />
                  <YAxis dataKey="name" type="category" stroke="#64748b" width={180} tick={{ fontSize: 11 }} />
                  <Tooltip contentStyle={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: 8 }} />
                  <Bar dataKey="value" fill={CARRIER_COLOR} radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
