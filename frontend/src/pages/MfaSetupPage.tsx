import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { ShieldCheck, Loader2, CheckCircle2, AlertCircle, Copy, ArrowLeft } from 'lucide-react';
import apiClient from '../api/client';

interface MfaSetupResponse {
  secret: string;
  otpauthUri: string;
}

export function MfaSetupPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'start' | 'scan' | 'verify' | 'done'>('start');
  const [setupData, setSetupData] = useState<MfaSetupResponse | null>(null);
  const [code, setCode] = useState('');
  const [backupCodes, setBackupCodes] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleStart = async () => {
    setError(null);
    setIsSubmitting(true);
    try {
      const response = await apiClient.post<MfaSetupResponse>('/api/v1/auth/mfa/setup');
      setSetupData(response.data);
      setStep('scan');
    } catch (err: unknown) {
      const errObj = err as { response?: { status?: number } };
      if (errObj?.response?.status === 500) {
        setError('MFA may already be enabled on this account.');
      } else {
        setError('Failed to start MFA setup.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerify = async (event: FormEvent) => {
    event.preventDefault();
    if (code.length !== 6) return;
    setError(null);
    setIsSubmitting(true);
    try {
      const response = await apiClient.post<string[]>(
        '/api/v1/auth/mfa/verify-setup',
        { code: parseInt(code, 10) }
      );
      setBackupCodes(response.data);
      setStep('done');
    } catch (err: unknown) {
      const errObj = err as { response?: { status?: number } };
      if (errObj?.response?.status === 400) {
        setError('Invalid code. Check your authenticator app and try again.');
      } else {
        setError('Verification failed. Try again.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const copySecret = () => {
    if (setupData) {
      navigator.clipboard.writeText(setupData.secret);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const copyBackupCodes = () => {
    navigator.clipboard.writeText(backupCodes.join('\n'));
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100 transition">
            <ArrowLeft className="w-4 h-4" />
            <span className="text-sm">Dashboard</span>
          </Link>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-sky-400" />
            <span className="text-sm font-medium">FraudSentinel</span>
          </div>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-6 py-12">
        <h1 className="text-2xl font-semibold tracking-tight mb-2">Two-factor authentication</h1>
        <p className="text-sm text-slate-400 mb-8">
          Add an extra security layer using an authenticator app (Google Authenticator, Authy, 1Password).
        </p>

        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-8">
          {step === 'start' && (
            <div className="text-center">
              <ShieldCheck className="w-16 h-16 text-sky-400 mx-auto mb-4" />
              <h2 className="text-lg font-medium mb-2">Enable MFA</h2>
              <p className="text-sm text-slate-400 mb-6">
                You will scan a QR code, verify with a 6-digit code, and receive 10 backup codes.
              </p>
              {error && (
                <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg mb-4 text-left">
                  <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
                  <p className="text-sm text-red-300">{error}</p>
                </div>
              )}
              <button
                onClick={handleStart}
                disabled={isSubmitting}
                className="px-6 py-2.5 bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white font-medium rounded-lg transition disabled:opacity-30"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                Get started
              </button>
            </div>
          )}

          {step === 'scan' && setupData && (
            <div>
              <h2 className="text-lg font-medium mb-4">Scan the QR code</h2>
              <p className="text-sm text-slate-400 mb-6">
                Open your authenticator app and scan this code.
              </p>

              <div className="flex flex-col items-center bg-white p-6 rounded-lg mb-6">
                <QRCodeSVG value={setupData.otpauthUri} size={200} />
              </div>

              <div className="mb-6">
                <p className="text-xs text-slate-400 mb-2 uppercase tracking-wide">Or enter secret manually</p>
                <div className="flex gap-2">
                  <code className="flex-1 px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg font-mono text-sm text-slate-200 break-all">
                    {setupData.secret}
                  </code>
                  <button
                    onClick={copySecret}
                    className="px-3 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg text-slate-300 transition"
                  >
                    {copied ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                onClick={() => setStep('verify')}
                className="w-full py-2.5 bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 border border-sky-500/30 font-medium rounded-lg transition"
              >
                Next: verify code
              </button>
            </div>
          )}

          {step === 'verify' && (
            <form onSubmit={handleVerify}>
              <h2 className="text-lg font-medium mb-4">Verify code</h2>
              <p className="text-sm text-slate-400 mb-6">
                Enter the 6-digit code from your authenticator app.
              </p>

              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]{6}"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                disabled={isSubmitting}
                required
                autoFocus
                placeholder="123456"
                className="w-full px-3 py-3 bg-slate-800/50 border border-slate-700 rounded-lg text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 text-center text-2xl tracking-widest font-mono mb-4"
              />

              {error && (
                <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg mb-4">
                  <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
                  <p className="text-sm text-red-300">{error}</p>
                </div>
              )}

              <button
                type="submit"
                disabled={code.length !== 6 || isSubmitting}
                className="w-full py-2.5 bg-gradient-to-r from-sky-500 to-violet-500 text-white font-medium rounded-lg transition disabled:opacity-30"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin inline mr-2" /> : null}
                Verify and enable MFA
              </button>
            </form>
          )}

          {step === 'done' && (
            <div>
              <div className="text-center mb-6">
                <CheckCircle2 className="w-16 h-16 text-emerald-400 mx-auto mb-4" />
                <h2 className="text-lg font-medium mb-2">MFA enabled</h2>
                <p className="text-sm text-slate-400">
                  Save these backup codes somewhere safe. Each can be used once to sign in if you lose your device.
                </p>
              </div>

              <div className="bg-slate-800/50 border border-slate-700 rounded-lg p-4 mb-6">
                <div className="grid grid-cols-2 gap-2">
                  {backupCodes.map((bc) => (
                    <code key={bc} className="font-mono text-sm text-slate-200 py-1 text-center">{bc}</code>
                  ))}
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  onClick={copyBackupCodes}
                  className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg transition"
                >
                  Copy backup codes
                </button>
                <button
                  onClick={() => navigate('/dashboard')}
                  className="flex-1 py-2.5 bg-gradient-to-r from-sky-500 to-violet-500 text-white font-medium rounded-lg transition"
                >
                  Done
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
