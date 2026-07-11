import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, Zap, Loader2, AlertCircle, CheckCircle2, TrendingUp } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import apiClient from '../api/client';

interface TransactionRequest {
  carrierName: string;
  transportName: string;
  failedPayments: number;
  succeededPayments: number;
  offerPrice: number;
  numberOfOffers: number;
  reportedIncidents: number;
}

interface RiskAlertResponse {
  alertId: string;
  carrierName: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: string;
  assignedDepartment: string;
  riskScore: number;
  triggeredRules: string;
}

const CLEAN_EXAMPLE: TransactionRequest = {
  carrierName: 'CleanTrans SL',
  transportName: 'Volvo FH500',
  failedPayments: 1,
  succeededPayments: 45,
  offerPrice: 1500,
  numberOfOffers: 12,
  reportedIncidents: 0,
};

const FRAUD_EXAMPLE: TransactionRequest = {
  carrierName: 'TransBadCorp SL',
  transportName: 'Old Iveco 2003',
  failedPayments: 12,
  succeededPayments: 3,
  offerPrice: 4500,
  numberOfOffers: 8,
  reportedIncidents: 5,
};

export function SimulatePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState<TransactionRequest>({
    carrierName: '',
    transportName: '',
    failedPayments: 0,
    succeededPayments: 0,
    offerPrice: 1500,
    numberOfOffers: 0,
    reportedIncidents: 0,
  });
  const [result, setResult] = useState<RiskAlertResponse | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setResult(null);
    setIsSubmitting(true);
    try {
      const response = await apiClient.post<RiskAlertResponse>(
        '/api/v1/transactions/analyze',
        form
      );
      setResult(response.data);
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { detail?: string } } };
      setError(errObj?.response?.data?.detail || 'Analysis failed.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const severityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'from-red-500 to-red-700 border-red-500/30 text-red-100';
      case 'HIGH': return 'from-orange-500 to-orange-700 border-orange-500/30 text-orange-100';
      case 'MEDIUM': return 'from-amber-500 to-amber-700 border-amber-500/30 text-amber-100';
      default: return 'from-emerald-500 to-emerald-700 border-emerald-500/30 text-emerald-100';
    }
  };

  const update = <K extends keyof TransactionRequest>(key: K, value: TransactionRequest[K]) => {
    setForm({ ...form, [key]: value });
    setResult(null);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
            <span className="text-slate-600">/</span>
            <span className="text-sm text-slate-400">Simulate</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/dashboard" className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition">
              <span>Back to dashboard</span>
            </Link>
            <div className="text-right">
              <div className="text-sm font-medium text-slate-200">{user?.username}</div>
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
        <div className="mb-6">
          <div className="flex items-center gap-2 mb-2">
            <Zap className="w-6 h-6 text-violet-400" />
            <h2 className="text-2xl font-semibold tracking-tight">Rule engine playground</h2>
          </div>
          <p className="text-sm text-slate-400">
            Enter transaction attributes and watch the fraud detection rules score the input in real time.
          </p>
        </div>

        <div className="flex gap-3 mb-6">
          <button
            onClick={() => setForm(CLEAN_EXAMPLE)}
            className="px-4 py-2 text-sm bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 rounded-lg transition"
          >
            Load clean example
          </button>
          <button
            onClick={() => setForm(FRAUD_EXAMPLE)}
            className="px-4 py-2 text-sm bg-red-500/10 hover:bg-red-500/20 text-red-300 border border-red-500/30 rounded-lg transition"
          >
            Load fraud example
          </button>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Transaction</h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1">Carrier name</label>
                <input
                  type="text"
                  value={form.carrierName}
                  onChange={(e) => update('carrierName', e.target.value)}
                  required
                  className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>
              <div>
                <label className="block text-xs text-slate-400 mb-1">Transport name</label>
                <input
                  type="text"
                  value={form.transportName}
                  onChange={(e) => update('transportName', e.target.value)}
                  required
                  className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Failed payments</label>
                  <input
                    type="number"
                    min={0}
                    value={form.failedPayments}
                    onChange={(e) => update('failedPayments', parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Succeeded payments</label>
                  <input
                    type="number"
                    min={0}
                    value={form.succeededPayments}
                    onChange={(e) => update('succeededPayments', parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">
                  Offer price EUR
                  <span className="text-slate-500 ml-2">(market baseline: 1500€)</span>
                </label>
                <input
                  type="number"
                  min={0.01}
                  step="0.01"
                  value={form.offerPrice}
                  onChange={(e) => update('offerPrice', parseFloat(e.target.value) || 0)}
                  className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Number of offers</label>
                  <input
                    type="number"
                    min={0}
                    value={form.numberOfOffers}
                    onChange={(e) => update('numberOfOffers', parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100"
                  />
                </div>
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Reported incidents</label>
                  <input
                    type="number"
                    min={0}
                    value={form.reportedIncidents}
                    onChange={(e) => update('reportedIncidents', parseInt(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-2.5 bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white font-medium rounded-lg transition disabled:opacity-30 flex items-center justify-center gap-2"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <TrendingUp className="w-4 h-4" />}
                <span>{isSubmitting ? 'Analyzing...' : 'Analyze transaction'}</span>
              </button>
            </form>
          </div>

          <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
            <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Rule engine result</h3>

            {!result && !error && (
              <div className="flex flex-col items-center justify-center h-64 text-slate-500">
                <Zap className="w-12 h-12 mb-3 opacity-30" />
                <p className="text-sm">Submit a transaction to see how the rules score it.</p>
              </div>
            )}

            {error && (
              <div className="flex items-start gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
                <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                <p className="text-sm text-red-300">{error}</p>
              </div>
            )}

            {result && (
              <div className="space-y-4">
                <div className={`p-4 rounded-lg border bg-gradient-to-br ${severityColor(result.severity)}`}>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-medium uppercase tracking-wider opacity-80">Severity</span>
                    <span className="text-2xl font-bold">{result.severity}</span>
                  </div>
                  <div className="text-sm opacity-90">Weighted risk score: {Math.min(100, (result.riskScore / 3) * 100).toFixed(1)}%</div>
                </div>

                <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4">
                  <div className="text-xs text-slate-400 mb-1">Auto-routed to</div>
                  <div className="text-lg font-semibold text-slate-100">{result.assignedDepartment}</div>
                </div>

                <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4">
                  <div className="text-xs text-slate-400 mb-1">Triggering rule</div>
                  <div className="text-sm text-slate-200">{result.triggeredRules}</div>
                </div>

                <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4">
                  <div className="text-xs text-slate-400 mb-1">Alert ID</div>
                  <div className="text-sm font-mono text-slate-200">{result.alertId}</div>
                </div>

                <div className="flex items-start gap-2 text-xs text-slate-400 p-3 bg-slate-800/30 rounded-lg">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                  <p>Alert created in database. Visible on the dashboard for the {result.assignedDepartment} team.</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
