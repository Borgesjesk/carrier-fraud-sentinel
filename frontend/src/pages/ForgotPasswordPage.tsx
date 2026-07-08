import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ShieldCheck, Loader2, CheckCircle2, AlertCircle, Mail, ArrowLeft } from 'lucide-react';
import apiClient from '../api/client';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!email.trim() || isSubmitting) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await apiClient.post('/api/v1/auth/forgot-password', { email: email.trim() });
      setSubmitted(true);
    } catch {
      setError('Something went wrong. Try again.');
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
          {submitted ? (
            <div className="text-center py-4">
              <CheckCircle2 className="w-12 h-12 text-emerald-400 mx-auto mb-4" />
              <h1 className="text-xl font-semibold text-slate-100 mb-2">Check your email</h1>
              <p className="text-sm text-slate-400 mb-6">
                If an account exists for that email, we've sent a reset link.
                The link is valid for 15 minutes.
              </p>
              <Link to="/login" className="inline-flex items-center gap-2 text-sm text-sky-300 hover:text-sky-200">
                <ArrowLeft className="w-4 h-4" />
                Back to login
              </Link>
            </div>
          ) : (
            <>
              <h1 className="text-xl font-semibold text-slate-100 mb-2">Forgot password?</h1>
              <p className="text-sm text-slate-400 mb-6">
                Enter your email and we'll send you a reset link.
              </p>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
                    Email
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-slate-500 absolute left-3 top-3" />
                    <input
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      disabled={isSubmitting}
                      placeholder="you@example.com"
                      required
                      className="w-full pl-10 pr-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:opacity-50"
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
                  disabled={isSubmitting || !email.trim()}
                  className="w-full flex items-center justify-center gap-2 py-2.5 bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white font-medium rounded-lg transition disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : null}
                  <span>Send reset link</span>
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