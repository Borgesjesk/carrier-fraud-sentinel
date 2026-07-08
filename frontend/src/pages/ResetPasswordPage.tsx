import { useState, type FormEvent } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { ShieldCheck, Loader2, CheckCircle2, AlertCircle, KeyRound, ArrowLeft } from 'lucide-react';
import apiClient from '../api/client';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!token) {
      setError('Missing reset token.');
      return;
    }
    if (password.length < 12) {
      setError('Password must be at least 12 characters.');
      return;
    }
    if (password !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      await apiClient.post('/api/v1/auth/reset-password', { token, newPassword: password });
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2500);
    } catch (err: unknown) {
      const errObj = err as { response?: { status?: number } };
      const status = errObj?.response?.status;
      if (status === 401) {
        setError('This reset link has expired or was already used.');
      } else if (status === 400) {
        setError('Password does not meet the strength requirements.');
      } else {
        setError('Something went wrong. Try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4 overflow-hidden relative">
      <div className="absolute top-0 -left-40 w-80 h-80 bg-sky-500/20 rounded-full blur-3xl" />
      <div className="absolute bottom-0 -right-40 w-80 h-80 bg-violet-500/20 rounded-full blur-3xl" />

      <div className="relative w-full max-w-md">
        <div className="flex items-center justify-center mb-6">
          <ShieldCheck className="w-8 h-8 text-sky-400" />
          <span className="ml-2 text-xl font-semibold text-slate-100">FraudSentinel</span>
        </div>

        <div className="bg-slate-900/70 backdrop-blur-xl border border-slate-800 rounded-2xl p-8 shadow-2xl">
          {success ? (
            <div className="text-center py-4">
              <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-4" />
              <h1 className="text-xl font-semibold text-slate-100 mb-2">Password updated</h1>
              <p className="text-sm text-slate-400">Redirecting to login...</p>
            </div>
          ) : (
            <>
              <h1 className="text-xl font-semibold text-slate-100 mb-2">Set new password</h1>
              <p className="text-sm text-slate-400 mb-6">
                Choose a strong password of at least 12 characters.
              </p>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
                    New password
                  </label>
                  <div className="relative">
                    <KeyRound className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      disabled={isSubmitting}
                      minLength={12}
                      required
                      className="w-full pl-10 pr-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:opacity-50"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
                    Confirm password
                  </label>
                  <div className="relative">
                    <KeyRound className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                    <input
                      type="password"
                      value={confirm}
                      onChange={(e) => setConfirm(e.target.value)}
                      disabled={isSubmitting}
                      required
                      className="w-full pl-10 pr-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:opacity-50"
                    />
                  </div>
                </div>

                {error && (
                  <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
                    <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
                    <p className="text-sm text-red-300">{error}</p>
                  </div>
                )}

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full flex items-center justify-center gap-2 py-2.5 bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white font-medium rounded-lg transition disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                  <span>Update password</span>
                </button>

                <Link to="/login" className="flex items-center justify-center gap-2 text-sm text-slate-400 hover:text-slate-200 mt-4">
                  <ArrowLeft className="w-4 h-4" />
                  Back to login
                </Link>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}