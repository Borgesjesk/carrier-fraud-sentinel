import { Clock, User, CheckCircle2, ArrowRightLeft, ArrowUpCircle, MessageSquare, FileText } from 'lucide-react';
import type { Alert } from '../types/Alert';
import type { Comment } from '../types/Comment';

interface TimelineEvent {
  timestamp: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  title: string;
  description?: string;
}

interface Props {
  alert: Alert;
  comments?: Comment[];
}

export function AlertTimeline({ alert, comments = [] }: Props) {
  const events: TimelineEvent[] = [];

  if (alert.createdDate) {
    events.push({
      timestamp: alert.createdDate,
      icon: FileText,
      color: 'text-sky-400 bg-sky-500/10 border-sky-500/30',
      title: 'Alert created',
      description: alert.createdBy ? `Reported by ${alert.createdBy}` : undefined,
    });
  }

  if (alert.assignedTo && !alert.acceptedDate) {
    events.push({
      timestamp: alert.createdDate,
      icon: User,
      color: 'text-violet-400 bg-violet-500/10 border-violet-500/30',
      title: 'Assigned',
      description: `To ${alert.assignedTo}`,
    });
  }

  if (alert.acceptedDate) {
    events.push({
      timestamp: alert.acceptedDate,
      icon: CheckCircle2,
      color: 'text-sky-400 bg-sky-500/10 border-sky-500/30',
      title: 'Accepted',
      description: alert.assignedTo ? `By ${alert.assignedTo}` : undefined,
    });
  }

  if (alert.lastTransferAt) {
    events.push({
      timestamp: alert.lastTransferAt,
      icon: ArrowRightLeft,
      color: 'text-amber-400 bg-amber-500/10 border-amber-500/30',
      title: 'Transferred',
      description: alert.lastTransferFromDept
        ? `From ${alert.lastTransferFromDept} to ${alert.assignedDepartment}${alert.lastTransferBy ? ` by ${alert.lastTransferBy}` : ''}`
        : undefined,
    });
  }

  if (alert.status === 'ESCALATED') {
    events.push({
      timestamp: alert.createdDate,
      icon: ArrowUpCircle,
      color: 'text-red-400 bg-red-500/10 border-red-500/30',
      title: 'Escalated',
    });
  }

  comments.forEach((c) => {
    events.push({
      timestamp: c.createdAt,
      icon: MessageSquare,
      color: c.authorRole === 'CLIENT'
        ? 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30'
        : 'text-sky-400 bg-sky-500/10 border-sky-500/30',
      title: `Comment by ${c.authorUsername}`,
      description: c.body ? (c.body.length > 80 ? c.body.slice(0, 80) + '...' : c.body) : '(no message)',
    });
  });

  if (alert.resolvedDate) {
    events.push({
      timestamp: alert.resolvedDate,
      icon: CheckCircle2,
      color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30',
      title: 'Resolved',
      description: alert.assignedTo ? `By ${alert.assignedTo}` : undefined,
    });
  }

  events.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());

  const formatDate = (iso: string) => {
    const d = new Date(iso);
    return d.toLocaleString('en-GB', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6">
      <div className="flex items-center gap-2 mb-6">
        <Clock className="w-5 h-5 text-sky-400" />
        <h3 className="text-base font-semibold">Timeline</h3>
        <span className="text-xs text-slate-500 ml-auto">{events.length} events</span>
      </div>

      {events.length === 0 ? (
        <p className="text-sm text-slate-500 text-center py-8">No events yet.</p>
      ) : (
        <div className="relative">
          <div className="absolute left-4 top-4 bottom-4 w-px bg-slate-800" />
          <div className="space-y-4">
            {events.map((event, i) => (
              <div key={i} className="relative flex gap-4">
                <div className={`relative z-10 flex-shrink-0 w-8 h-8 rounded-full border flex items-center justify-center ${event.color}`}>
                  <event.icon className="w-4 h-4" />
                </div>
                <div className="flex-1 pb-1">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-medium text-slate-200">{event.title}</p>
                    <span className="text-xs text-slate-500 flex-shrink-0">{formatDate(event.timestamp)}</span>
                  </div>
                  {event.description && (
                    <p className="text-xs text-slate-400 mt-0.5">{event.description}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
