import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, User, Loader2, CheckCircle2, AlertCircle, KeyRound, Mail, ShieldOff } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import apiClient from '../api/client';

interface Profile {
  username: string;
  email: string;
  role: string;
  mfaEnabled: boolean;
  backupCodesRemaining: number;
  activeSessions: number;
  createdAt: string;
}

function getPasswordStrength(password: string): { score: number; label: string; color: string } {
  if (!password) return { score: 0, label: '', color: '' };
  let score = 0;
  if (password.length >= 12) score++;
  if (password.length >= 16) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;

  if (score <= 2) return { score, label: 'Weak', color: 'bg-red-500' };
  if (score <= 4) return { score, label: 'Fair', color: 'bg-amber-500' };
  if (score <= 5) return { score, label: 'Strong', color: 'bg-emerald-500' };
  return { score, label: 'Excellent', color: 'bg-sky-500' };
}

export function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [emailPassword, setEmailPassword] = useState('');
  const [mfaPassword, setMfaPassword] = useState('');
  const [busy, setBusy] = useState<string | null>(null);
  const [pwdTotp, setPwdTotp] = useState('');
  const [emailTotp, setEmailTotp] = useState('');
  const [mfaDisableTotp, setMfaDisableTotp] = useState('');

  const loadProfile = async () => {
    setLoading(true);
    try {
      const r = await apiClient.get<Profile>('/api/v1/profile');
      setProfile(r.data);
      setNewEmail(r.data.email);
    } catch {
      setError('Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadProfile(); }, []);

  const handleChangePassword = async (e: FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) return setError('Passwords do not match');
    if (newPassword.length < 12) return setError('New password must be at least 12 characters');
    setError(null);
    setBusy('password');
    try {
      await apiClient.put('/api/v1/profile/password', {
              oldPassword,
              newPassword,
              totpCode: pwdTotp ? parseInt(pwdTotp, 10) : null
            });
      setMessage('Password updated');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { detail?: string } } };
      setError(errObj?.response?.data?.detail || 'Password change failed');
    } finally {
      setBusy(null);
    }
  };

  const handleChangeEmail = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy('email');
    try {
      await apiClient.put('/api/v1/profile/email', {
              newEmail,
              password: emailPassword,
              totpCode: emailTotp ? parseInt(emailTotp, 10) : null
            });
      setMessage('Email updated');
      setEmailPassword('');
      loadProfile();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { detail?: string } } };
      setError(errObj?.response?.data?.detail || 'Email change failed');
    } finally {
      setBusy(null);
    }
  };

  const handleDisableMfa = async (e: FormEvent) => {
    e.preventDefault();
    if (!window.confirm('Disable two-factor authentication?')) return;
    setError(null);
    setBusy('mfa');
    try {
      await apiClient.post('/api/v1/profile/mfa/disable', {
              password: mfaPassword,
              totpCode: mfaDisableTotp ? parseInt(mfaDisableTotp, 10) : null
            });
      setMessage('MFA disabled');
      setMfaPassword('');
      loadProfile();
    } catch (err: unknown) {
      const errObj = err as { response?: { data?: { detail?: string } } };
      setError(errObj?.response?.data?.detail || 'MFA disable failed');
    } finally {
      setBusy(null);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link to="/dashboard" className="flex items-center gap-2 text-slate-300 hover:text-slate-100">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
            <span className="text-slate-600">/</span>
            <span className="text-sm text-slate-400">Profile</span>
          </Link>
          <div className="flex items-center gap-4">
                      <Link
                        to={user?.role === 'CLIENT' ? '/complaints/mine' : '/dashboard'}
                        className="flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition"
                      >
                        <span>← Back</span>
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

      <main className="max-w-4xl mx-auto px-6 py-8 space-y-6">
        <div className="flex items-center gap-2 mb-2">
          <User className="w-6 h-6 text-sky-400" />
          <h2 className="text-2xl font-semibold">My profile</h2>
        </div>

        {loading && <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-sky-400" /></div>}
        {message && (
          <div className="flex items-start gap-2 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-lg">
            <CheckCircle2 className="w-5 h-5 text-emerald-400 mt-0.5" />
            <p className="text-sm text-emerald-300">{message}</p>
          </div>
        )}
        {error && (
          <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
            <AlertCircle className="w-5 h-5 text-red-400 mt-0.5" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        {profile && !loading && (
          <>
           <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
                         <div>
                           <div className="text-sm font-semibold text-slate-200">Session management</div>
                           <div className="text-xs text-slate-400">View and revoke your active sessions</div>
                         </div>
                         <Link
                           to="/settings/sessions"
                           className="px-3 py-1.5 text-sm bg-sky-500/10 hover:bg-sky-500/20 text-sky-300 border border-sky-500/30 rounded-lg transition"
                         >
                           Manage sessions
                         </Link>
                       </div>
            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4">Account info</h3>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <div className="text-xs text-slate-500">Username</div>
                  <div className="text-slate-200 font-mono">{profile.username}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">Role</div>
                  <div className="text-slate-200">{profile.role}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">Email</div>
                  <div className="text-slate-200">{profile.email || '—'}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">MFA status</div>
                  <div className={profile.mfaEnabled ? 'text-emerald-300' : 'text-slate-400'}>
                    {profile.mfaEnabled ? 'Enabled' : 'Disabled'}
                    {profile.mfaEnabled && ` (${profile.backupCodesRemaining} backup codes)`}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">Active sessions</div>
                  <div className="text-slate-200">{profile.activeSessions}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500">Member since</div>
                  <div className="text-slate-200">{profile.createdAt ? new Date(profile.createdAt).toLocaleDateString() : '—'}</div>
                </div>
              </div>
            </div>

            <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4 flex items-center gap-2">
                <KeyRound className="w-4 h-4" />
                Change password
              </h3>
              <form onSubmit={handleChangePassword} className="space-y-3">
                <input type="password" value={oldPassword} onChange={(e) => setOldPassword(e.target.value)} required placeholder="Current password" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm" />
                <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={12} placeholder="New password (min 12 chars)" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm" />
                                {newPassword && (
                                  <div className="space-y-1">
                                    <div className="flex gap-1">
                                      {[1, 2, 3, 4, 5, 6].map((i) => {
                                        const strength = getPasswordStrength(newPassword);
                                        return (
                                          <div
                                            key={i}
                                            className={`h-1 flex-1 rounded ${i <= strength.score ? strength.color : 'bg-slate-700'}`}
                                          />
                                        );
                                      })}
                                    </div>
                                    <p className="text-xs text-slate-400">Strength: <span className={
                                      getPasswordStrength(newPassword).label === 'Weak' ? 'text-red-300' :
                                      getPasswordStrength(newPassword).label === 'Fair' ? 'text-amber-300' :
                                      getPasswordStrength(newPassword).label === 'Strong' ? 'text-emerald-300' :
                                      'text-sky-300'
                                    }>{getPasswordStrength(newPassword).label}</span></p>
                                  </div>
                                )}
                <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required placeholder="Confirm new password" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm" />
                                {profile.mfaEnabled && (
                                  <input type="text" inputMode="numeric" maxLength={6} value={pwdTotp} onChange={(e) => setPwdTotp(e.target.value.replace(/\D/g, ''))} required placeholder="6-digit MFA code" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm text-center font-mono tracking-widest" />
                                )}
                                <button type="submit" disabled={busy === 'password'} className="w-full py-2 bg-gradient-to-r from-sky-500 to-violet-500 text-white font-medium rounded-lg disabled:opacity-30">
                  {busy === 'password' ? <Loader2 className="w-4 h-4 animate-spin inline" /> : 'Update password'}
                </button>
              </form>
            </div>

            {(profile.role === 'CLIENT' || profile.role === 'ADMIN') && (
                        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
                          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-400 mb-4 flex items-center gap-2">
                            <Mail className="w-4 h-4" />
                            Change email
                          </h3>
              <form onSubmit={handleChangeEmail} className="space-y-3">
                <input type="email" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} required placeholder="New email" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm" />
                <input type="password" value={emailPassword} onChange={(e) => setEmailPassword(e.target.value)} required placeholder="Confirm with password" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm" />
                                {profile.mfaEnabled && (
                                  <input type="text" inputMode="numeric" maxLength={6} value={emailTotp} onChange={(e) => setEmailTotp(e.target.value.replace(/\D/g, ''))} required placeholder="6-digit MFA code" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm text-center font-mono tracking-widest" />
                                )}
                                <button type="submit" disabled={busy === 'email'} className="w-full py-2 bg-gradient-to-r from-sky-500 to-violet-500 text-white font-medium rounded-lg disabled:opacity-30">
                  {busy === 'email' ? <Loader2 className="w-4 h-4 animate-spin inline" /> : 'Update email'}
                </button>
              </form>
                          </div>
                          )}

                          {profile.mfaEnabled && profile.role === 'ADMIN' && (
                            <div className="bg-slate-900/50 border border-red-500/30 rounded-xl p-6">
                <h3 className="text-sm font-semibold uppercase tracking-wide text-red-400 mb-4 flex items-center gap-2">
                  <ShieldOff className="w-4 h-4" />
                  Disable MFA
                </h3>
                <form onSubmit={handleDisableMfa} className="space-y-3">
                  <p className="text-xs text-slate-400">This removes two-factor protection from your account.</p>
                  <input type="password" value={mfaPassword} onChange={(e) => setMfaPassword(e.target.value)} required placeholder="Confirm with password" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm" />
                                    <input type="text" inputMode="numeric" maxLength={6} value={mfaDisableTotp} onChange={(e) => setMfaDisableTotp(e.target.value.replace(/\D/g, ''))} required placeholder="6-digit MFA code" className="w-full px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg text-sm text-center font-mono tracking-widest" />
                                    <button type="submit" disabled={busy === 'mfa'} className="w-full py-2 bg-red-500/20 hover:bg-red-500/30 text-red-300 border border-red-500/30 font-medium rounded-lg disabled:opacity-30">
                    {busy === 'mfa' ? <Loader2 className="w-4 h-4 animate-spin inline" /> : 'Disable MFA'}
                  </button>
                </form>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
