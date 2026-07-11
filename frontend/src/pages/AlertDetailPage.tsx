import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, ShieldCheck, Loader2, AlertCircle, CheckCircle2, Search, ArrowUpCircle, ArrowRightLeft, FileText, Download } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { CommentsThread } from '../components/CommentsThread';
import { alertService } from '../api/alertService';
import { alertReadService } from '../api/alertReadService';
import { NotesThread } from '../components/NotesThread';
import { AlertTimeline } from '../components/AlertTimeline';
import { commentService } from '../api/commentService';
import type { Comment } from '../types/Comment';
import type { Alert, Severity, AlertStatus } from '../types/Alert';

const SEVERITY_STYLES: Record<Severity, string> = {
  LOW: 'bg-slate-700/50 text-slate-300 border-slate-600',
  MEDIUM: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
  HIGH: 'bg-orange-500/15 text-orange-300 border-orange-500/30',
  CRITICAL: 'bg-red-500/15 text-red-300 border-red-500/30',
};

const STATUS_STYLES: Record<AlertStatus, string> = {
  UNASSIGNED: 'bg-slate-700/50 text-slate-300',
  ASSIGNED: 'bg-sky-500/15 text-sky-300',
  ACCEPTED: 'bg-sky-500/15 text-sky-300',
  IN_PROGRESS: 'bg-violet-500/15 text-violet-300',
  RESOLVED: 'bg-emerald-500/15 text-emerald-300',
  ESCALATED: 'bg-red-500/15 text-red-300',
};

export function AlertDetailPage() {
  const { alertId } = useParams<{ alertId: string }>();
  useNavigate();
  const { user } = useAuth();

  const [alert, setAlert] = useState<Alert | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionInFlight, setActionInFlight] = useState<string | null>(null);
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [transferTarget, setTransferTarget] = useState('LEGAL');
  const [transferReason, setTransferReason] = useState('');
  const [timelineComments, setTimelineComments] = useState<Comment[]>([]);

  useEffect(() => {
      if (!alertId) return;
      alertService.getById(alertId)
        .then(setAlert)
        .catch(() => setError('Alert not found or you do not have permission to view it.'))
        .finally(() => setIsLoading(false));

      commentService.list(alertId).then(setTimelineComments).catch(() => {});
      alertReadService.markAsRead(alertId).catch(() => {
      });
    }, [alertId]);

  const runAction = async (label: string, fn: () => Promise<Alert>) => {
    setActionInFlight(label);
    setError(null);
    try {
      const updated = await fn();
      setAlert(updated);
    } catch {
      setError(`Failed to ${label.toLowerCase()}. Try again.`);
    } finally {
      setActionInFlight(null);
    }
  };

  const handleAccept = () => runAction('Accept', () => alertService.accept(alertId!, user!.username));
  const handleInvestigate = () => runAction('Investigate', () => alertService.investigate(alertId!));
  const handleResolve = () => {
    const resolution = prompt('Resolution summary:');
    if (!resolution) return;
    runAction('Resolve', () => alertService.resolve(alertId!, resolution));
  };

   const handleTransfer = async () => {
    if (!transferReason.trim()) return;
    setActionInFlight('Transfer');
    try {
      const updated = await alertService.transfer(alertId!, transferTarget, transferReason.trim());
      setAlert(updated);
      setShowTransferModal(false);
      setTransferReason('');
    } catch {
      setError('Failed to transfer.');
    } finally {
      setActionInFlight(null);
    }
  };
  const handleEscalate = () => {
    const reason = prompt('Escalation reason:');
    if (!reason) return;
    runAction('Escalate', () => alertService.escalate(alertId!, reason));
  };

  const formatDate = (iso: string) => new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link
                      to={user?.role === 'CLIENT' ? '/complaints/mine' : '/dashboard'}
                      className="flex items-center gap-2 text-slate-300 hover:text-slate-100 transition"
                    >
                      <ArrowLeft className="w-4 h-4" />
                      <span className="text-sm">{user?.role === 'CLIENT' ? 'My cases' : 'Dashboard'}</span>
                    </Link>
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-sky-400" />
            <span className="text-sm font-medium">FraudSentinel</span>
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-8">
        {isLoading && (
          <div className="flex items-center gap-2 text-slate-400 py-12 justify-center">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Loading alert...</span>
          </div>
        )}

        {error && (
          <div className="flex items-start gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-lg mb-6">
            <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
            <p className="text-sm text-red-300">{error}</p>
          </div>
        )}

        {alert && (
          <>
            <div className="flex items-start justify-between mb-6">
              <div>
                <p className="text-xs text-slate-500 font-mono mb-1">{alert.alertId}</p>
                <h1 className="text-2xl font-semibold tracking-tight text-red-300">{alert.carrierName}</h1>
                {alert.createdBy && <p className="text-sm text-slate-400 mt-1">Reported by <span className="text-emerald-300">{alert.createdBy}</span></p>}
              </div>
              <div className="flex gap-2">
                <span className={`px-3 py-1 text-xs font-medium rounded border ${SEVERITY_STYLES[alert.severity]}`}>
                  {alert.severity}
                </span>
                <span className={`px-3 py-1 text-xs font-medium rounded ${STATUS_STYLES[user?.role === 'CLIENT' ? alert.clientVisibleStatus : alert.status]}`}>
                                  {user?.role === 'CLIENT' ? alert.clientVisibleStatus : alert.status}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 mb-8">
                          <DetailCard label="Risk score" value={Math.min(100, (alert.riskScore / 3) * 100).toFixed(0) + '%'} />
                          <DetailCard label="Triggered rule" value={alert.triggeredRules} />
                          <DetailCard label="Assigned department" value={alert.assignedDepartment} />
                          <DetailCard label="Created" value={formatDate(alert.createdDate)} />
                          {alert.assignedTo && (
                            <DetailCard label="Assigned to" value={alert.assignedTo} />
                          )}
                          {alert.lastTransferAt && (
                            <DetailCard
                              label="Last transfer"
                              value={`${alert.lastTransferBy} · ${alert.lastTransferFromDept} → ${alert.assignedDepartment} · ${formatDate(alert.lastTransferAt)}`}
                            />
                          )}
                        </div>

                        {alert.description && (
              <section className="bg-slate-900/50 border border-slate-800 rounded-lg p-6 mb-6">
                <h2 className="text-sm font-medium text-slate-200 mb-3 uppercase tracking-wide">Description</h2>
                <p className="text-sm text-slate-300 whitespace-pre-wrap">{alert.description}</p>
                {alert.createdBy && (
                  <p className="text-xs text-slate-500 mt-3">Submitted by {alert.createdBy}</p>
                )}
              </section>
            )}

            {alert.documents && alert.documents.length > 0 && (
              <section className="bg-slate-900/50 border border-slate-800 rounded-lg p-6 mb-6">
                <h2 className="text-sm font-medium text-slate-200 mb-3 uppercase tracking-wide">
                  Documents ({alert.documents.length})
                </h2>
                <ul className="space-y-2">
                  {alert.documents.map((doc) => (
                    <li key={doc.documentId} className="flex items-center gap-3 px-3 py-2 bg-slate-800/30 border border-slate-800 rounded-lg">
                      <FileText className="w-4 h-4 text-slate-400 flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                                                  <span className="text-sm text-slate-200 truncate">{doc.originalFilename}</span>
                                                  <span className="px-1.5 py-0.5 text-[10px] font-medium bg-sky-500/15 text-sky-300 border border-sky-500/30 rounded uppercase tracking-wide">
                                                    {doc.category === 'INVOICE' ? 'Factura' :
                                                     doc.category === 'CMR' ? 'CMR' :
                                                     doc.category === 'LOAD_ORDER' ? 'Orden carga' :
                                                     doc.category === 'EMAIL' ? 'Email' : 'Otro'}
                                                  </span>
                                                </div>
                                                <div className="text-xs text-slate-500">{(doc.sizeBytes / 1024).toFixed(1)} KB · {doc.contentType}</div>
                      </div>
                      <a
                        href={`${import.meta.env.VITE_API_BASE_URL}/api/v1/complaints/${alert.alertId}/documents/${doc.documentId}?inline=true`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center gap-1 px-3 py-1.5 text-xs text-sky-300 hover:text-sky-200 hover:bg-sky-500/10 rounded-lg transition"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>Download</span>
                      </a>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            {user?.role !== 'CLIENT' && (
            <section className="bg-slate-900/50 border border-slate-800 rounded-lg p-6">
              <h2 className="text-sm font-medium text-slate-200 mb-4 uppercase tracking-wide">Workflow actions</h2>
              <div className="flex flex-wrap gap-3">
                <ActionButton icon={CheckCircle2} label="Accept" onClick={handleAccept} inFlight={actionInFlight} disabled={alert.status !== 'UNASSIGNED'} />
                <ActionButton icon={Search} label="Investigate" onClick={handleInvestigate} inFlight={actionInFlight} disabled={alert.status !== 'ACCEPTED'} />
                <ActionButton icon={CheckCircle2} label="Resolve" onClick={handleResolve} inFlight={actionInFlight} disabled={alert.status === 'RESOLVED' || alert.status === 'UNASSIGNED'} variant="success" />
                <ActionButton icon={ArrowRightLeft} label="Transfer" onClick={() => setShowTransferModal(true)} inFlight={actionInFlight} disabled={alert.status === 'RESOLVED'} />
                <ActionButton icon={ArrowUpCircle} label="Escalate" onClick={handleEscalate} inFlight={actionInFlight} disabled={alert.status === 'RESOLVED'} variant="warning" />
              </div>
              <p className="text-xs text-slate-500 mt-4">
                State transitions: UNASSIGNED → ACCEPTED → IN_PROGRESS → RESOLVED (or ESCALATED at any time)
              </p>
            </section>
            )}

            <div className="mt-6">
                          <AlertTimeline alert={alert} comments={timelineComments} />
                        </div>

                        {user?.role !== 'CLIENT' && (
                          <div className="mt-6">
                            <NotesThread alertId={alert.alertId} />
                          </div>
                        )}

                        <div className="mt-6">
                          <CommentsThread alertId={alert.alertId} />
                        </div>
          </>
        )}
              </main>

              {showTransferModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
                  <div className="bg-slate-900 border border-slate-700 rounded-xl p-6 max-w-md w-full shadow-2xl">
                    <h3 className="text-lg font-semibold text-slate-100 mb-4">Transfer alert</h3>

                    <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
                      Target department
                    </label>
                    <select
                      value={transferTarget}
                      onChange={(e) => setTransferTarget(e.target.value)}
                      className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-3 py-2 text-slate-100 mb-4 focus:outline-none focus:ring-2 focus:ring-sky-500"
                    >
                      <option value="LEGAL">Legal</option>
                      <option value="INSURANCE">Insurance</option>
                      <option value="MEDIATION">Mediation</option>
                      <option value="FRAUD_INVESTIGATION">Fraud investigation</option>
                      <option value="CUSTOMER_SERVICE">Customer service</option>
                      <option value="COMPLIANCE_REVIEW">Compliance review</option>
                      <option value="DEPARTMENT_MANAGER">Department manager</option>
                    </select>

                    <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
                      Reason
                    </label>
                    <textarea
                      value={transferReason}
                      onChange={(e) => setTransferReason(e.target.value)}
                      rows={3}
                      className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 mb-4 focus:outline-none focus:ring-2 focus:ring-sky-500 resize-none"
                      placeholder="Why is this being transferred?"
                    />

                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => { setShowTransferModal(false); setTransferReason(''); }}
                        className="px-4 py-2 text-sm text-slate-300 hover:bg-slate-800 rounded-lg transition"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={handleTransfer}
                        disabled={!transferReason.trim() || actionInFlight === 'Transfer'}
                        className="px-4 py-2 text-sm font-medium bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 border border-sky-500/30 rounded-lg transition disabled:opacity-30"
                      >
                        {actionInFlight === 'Transfer' ? 'Transferring...' : 'Transfer'}
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          );
        }

function DetailCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="bg-slate-900/50 border border-slate-800 rounded-lg p-4">
      <div className="text-xs text-slate-400 uppercase tracking-wide mb-1">{label}</div>
      <div className="text-base font-medium text-slate-100">{value}</div>
    </div>
  );
}

function ActionButton({ icon: Icon, label, onClick, inFlight, disabled, variant = 'primary' }: {
  icon: typeof CheckCircle2;
  label: string;
  onClick: () => void;
  inFlight: string | null;
  disabled?: boolean;
  variant?: 'primary' | 'success' | 'warning';
}) {
  const isLoading = inFlight === label;
  const styles = {
    primary: 'bg-sky-500/10 hover:bg-sky-500/20 text-sky-300 border-sky-500/30',
    success: 'bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border-emerald-500/30',
    warning: 'bg-orange-500/10 hover:bg-orange-500/20 text-orange-300 border-orange-500/30',
  };
  return (
    <button
      onClick={onClick}
      disabled={disabled || inFlight !== null}
      className={`flex items-center gap-2 px-4 py-2 text-sm font-medium border rounded-lg transition disabled:opacity-30 disabled:cursor-not-allowed ${styles[variant]}`}
    >
      {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Icon className="w-4 h-4" />}
      <span>{label}</span>
    </button>
  );
}
