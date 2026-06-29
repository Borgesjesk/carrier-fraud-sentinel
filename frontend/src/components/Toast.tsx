import { useEffect } from 'react';
import { MessageCircle, X } from 'lucide-react';

interface ToastProps {
  message: string;
  onClose: () => void;
  onClick?: () => void;
}

export function Toast({ message, onClose, onClick }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const handleBodyClick = () => {
    if (onClick) {
      onClick();
      onClose();
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-4 fade-in duration-300">
      <div
        onClick={handleBodyClick}
        className={`flex items-center gap-3 px-4 py-3 bg-slate-900 border border-sky-500/40 rounded-lg shadow-lg shadow-sky-500/10 max-w-sm ${onClick ? 'cursor-pointer hover:border-sky-500/60 hover:bg-slate-800/50' : ''} transition`}
      >
        <MessageCircle className="w-5 h-5 text-sky-400 flex-shrink-0" />
        <p className="text-sm text-slate-200 flex-1">{message}</p>
        <button
          onClick={(e) => { e.stopPropagation(); onClose(); }}
          className="text-slate-500 hover:text-slate-300 transition flex-shrink-0"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
