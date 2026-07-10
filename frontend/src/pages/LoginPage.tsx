import { useState, type FormEvent } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { ShieldCheck, User, Lock, Loader2, AlertCircle } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { AxiosError } from 'axios';
import type { ProblemDetail } from '../types/ProblemDetail';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mfaRequired, setMfaRequired] = useState(false);
  const [mfaCode, setMfaCode] = useState('');

  const { login, loginMfa } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      if (mfaRequired) {
              await loginMfa({ username, password }, parseInt(mfaCode, 10));
              navigate(from, { replace: true });
              return;
            }

            const result = await login({ username, password });
            if (result.mfaRequired) {
        setMfaRequired(true);
        setIsSubmitting(false);
        return;
      }
      navigate(from, { replace: true });
    } catch (err) {
      const axiosError = err as AxiosError<ProblemDetail>;
      const detail = axiosError.response?.data?.detail;
      setError(detail || 'Authentication failed. Check your credentials and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-slate-950 flex items-center justify-center p-4">
      {/* Animated background blobs */}
      <div className="absolute top-0 -left-20 w-96 h-96 bg-sky-500 rounded-full opacity-15 blur-3xl animate-pulse" />
      <div className="absolute bottom-0 -right-20 w-96 h-96 bg-violet-500 rounded-full opacity-15 blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />

      {/* Login card */}
      <div className="relative w-full max-w-md bg-slate-900/50 backdrop-blur-xl border border-slate-800 rounded-2xl shadow-2xl p-8 animate-in fade-in slide-in-from-bottom-4 duration-500">

        {/* Logo + brand */}
        <div className="flex flex-col items-center gap-3 mb-8">
          <div className="p-3 bg-gradient-to-br from-sky-500/20 to-violet-500/20 rounded-xl border border-sky-500/30">
            <ShieldCheck className="w-8 h-8 text-sky-400" />
          </div>
          <div className="text-center">
            <h1 className="text-2xl font-semibold text-slate-50 tracking-tight">FraudSentinel</h1>
            <p className="text-sm text-slate-400 mt-1">Carrier fraud detection platform</p>
          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Username field */}
          <div>
            <label htmlFor="username" className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
              Username
            </label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                minLength={3}
                autoComplete="username"
                disabled={isSubmitting}
                className="w-full bg-slate-800/50 border border-slate-700 rounded-lg pl-10 pr-3 py-2.5 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition disabled:opacity-50"
                placeholder="admin"
              />
            </div>
          </div>

          {/* Password field */}
          <div>
            <label htmlFor="password" className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
                autoComplete="current-password"
                disabled={isSubmitting}
                className="w-full bg-slate-800/50 border border-slate-700 rounded-lg pl-10 pr-3 py-2.5 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition disabled:opacity-50"
                placeholder="••••••••"
              />
            </div>
          </div>

          {/* MFA code field */}
                    {mfaRequired && (
                      <div>
                        <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
                          6-digit code from your authenticator app
                        </label>
                        <input
                          type="text"
                          inputMode="numeric"
                          pattern="[0-9]{6}"
                          maxLength={6}
                          value={mfaCode}
                          onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, ''))}
                          disabled={isSubmitting}
                          required
                          autoFocus
                          placeholder="123456"
                          className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 text-center text-lg tracking-widest font-mono"
                        />
                      </div>
                    )}

          {/* Error message */}
          {error && (
            <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg animate-in fade-in slide-in-from-top-1 duration-200">
              <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
              <p className="text-sm text-red-300">{error}</p>
            </div>
          )}

          {/* Submit button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full mt-2 bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white font-medium py-2.5 px-4 rounded-lg transition shadow-lg shadow-sky-500/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Authenticating...
              </>
            ) : (
              <>
                Sign in
                <span className="text-lg leading-none">→</span>
              </>
            )}
          </button>

          <Link to="/forgot-password" className="block text-center text-xs text-slate-400 hover:text-slate-200 mt-4">
            Forgot your password?
          </Link>
        </form>

        {/* Footer */}
        <p className="text-center text-xs text-slate-500 mt-6">
          Authorized access only. All activity is logged.
        </p>
      </div>
    </div>
  );
}
