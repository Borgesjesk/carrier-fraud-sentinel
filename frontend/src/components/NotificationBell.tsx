import { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, MessageSquare, X } from 'lucide-react';
import { alertReadService } from '../api/alertReadService';
import { alertService } from '../api/alertService';
import type { Alert } from '../types/Alert';

interface Notification {
  alertId: string;
  carrierName: string;
  count: number;
}

export function NotificationBell() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const load = async () => {
      try {
        const alerts: Alert[] = await alertService.getAll();
        const counts: Record<string, number> = await alertReadService.unreadCounts();
        const notifs: Notification[] = Object.entries(counts)
          .filter(([, count]) => count > 0)
          .map(([alertId, count]) => {
            const alert = alerts.find((a) => a.alertId === alertId);
            return {
              alertId,
              carrierName: alert?.carrierName || 'Unknown',
              count,
            };
          })
          .slice(0, 10);
        setNotifications(notifs);
      } catch {
      }
    };
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const totalUnread = notifications.reduce((sum, n) => sum + n.count, 0);

  const handleClick = (alertId: string) => {
    setIsOpen(false);
    navigate(`/alerts/${alertId}`);
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative flex items-center gap-2 px-3 py-1.5 text-sm text-slate-300 hover:text-slate-100 hover:bg-slate-800 rounded-lg transition"
      >
        <Bell className="w-4 h-4" />
        {totalUnread > 0 && (
          <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full min-w-5 h-5 flex items-center justify-center px-1">
            {totalUnread > 99 ? '99+' : totalUnread}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl z-50">
          <div className="flex items-center justify-between p-4 border-b border-slate-800">
            <h3 className="text-sm font-semibold text-slate-200">Notifications</h3>
            <button onClick={() => setIsOpen(false)} className="text-slate-500 hover:text-slate-300">
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="max-h-96 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="p-8 text-center text-slate-500">
                <Bell className="w-8 h-8 mx-auto mb-2 opacity-30" />
                <p className="text-sm">All caught up!</p>
              </div>
            ) : (
              notifications.map((n) => (
                <button
                  key={n.alertId}
                  onClick={() => handleClick(n.alertId)}
                  className="w-full flex items-start gap-3 p-4 hover:bg-slate-800/50 border-b border-slate-800/50 last:border-0 transition text-left"
                >
                  <MessageSquare className="w-5 h-5 text-sky-400 flex-shrink-0 mt-0.5" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-slate-200 truncate">{n.carrierName}</p>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {n.count} new {n.count === 1 ? 'comment' : 'comments'}
                    </p>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
