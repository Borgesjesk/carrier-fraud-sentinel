import { useEffect, useState, type FormEvent } from 'react';
import { FileText, Send, Loader2, AlertCircle, User, Lock } from 'lucide-react';
import { noteService } from '../api/noteService';
import type { Note } from '../types/Note';

const ROLE_STYLES: Record<string, string> = {
  ADMIN: 'bg-red-500/15 text-red-300',
  ANALYST: 'bg-sky-500/15 text-sky-300',
  COMPLIANCE: 'bg-violet-500/15 text-violet-300',
};

interface NotesThreadProps {
  alertId: string;
}

export function NotesThread({ alertId }: NotesThreadProps) {
  const [notes, setNotes] = useState<Note[]>([]);
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isPosting, setIsPosting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    noteService.list(alertId)
      .then((data) => { if (!cancelled) setNotes(data); })
      .catch(() => { if (!cancelled) setError('Failed to load notes.'); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, [alertId]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!content.trim() || isPosting) return;
    setIsPosting(true);
    setError(null);
    try {
      const newNote = await noteService.create(alertId, { content: content.trim() });
      setNotes((prev) => [...prev, newNote]);
      setContent('');
    } catch {
      setError('Failed to post note. Try again.');
    } finally {
      setIsPosting(false);
    }
  };

  const formatDate = (iso: string) => new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
  });

  return (
    <section className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-6">
      <div className="flex items-center gap-2 mb-2">
        <Lock className="w-4 h-4 text-amber-400" />
        <h2 className="text-sm font-medium text-amber-200 uppercase tracking-wide">
          Internal notes ({notes.length})
        </h2>
      </div>
      <p className="text-xs text-amber-200/60 mb-4">Staff-only. Clients cannot see these.</p>

      {isLoading && (
        <div className="flex items-center gap-2 text-slate-400 py-6 justify-center">
          <Loader2 className="w-4 h-4 animate-spin" />
          <span className="text-sm">Loading notes...</span>
        </div>
      )}

      {!isLoading && notes.length === 0 && (
        <p className="text-sm text-slate-500 text-center py-6">
          No internal notes yet.
        </p>
      )}

      {!isLoading && notes.length > 0 && (
        <ul className="space-y-3 mb-4">
          {notes.map((note) => (
            <li key={note.noteId} className="bg-slate-900/40 border border-slate-800 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <User className="w-3.5 h-3.5 text-slate-500" />
                <span className="text-sm font-medium text-slate-200">{note.author}</span>
                <span className={`px-2 py-0.5 text-xs rounded ${ROLE_STYLES[note.authorRole] || 'bg-slate-700 text-slate-300'}`}>
                  {note.authorRole}
                </span>
                <span className="text-xs text-slate-500 ml-auto">{formatDate(note.createdAt)}</span>
              </div>
              <p className="text-sm text-slate-300 whitespace-pre-wrap break-words">{note.content}</p>
            </li>
          ))}
        </ul>
      )}

      {error && (
        <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg mb-3">
          <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
          <p className="text-sm text-red-300">{error}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex gap-2 items-start">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="Internal note (staff-only, up to 5000 chars)..."
          maxLength={5000}
          rows={3}
          disabled={isPosting}
          className="flex-1 bg-slate-900/50 border border-slate-800 rounded-lg px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:border-transparent transition disabled:opacity-50 resize-none"
        />
        <button
          type="submit"
          disabled={isPosting || !content.trim()}
          className="flex items-center gap-2 px-4 py-2 bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30 rounded-lg text-sm font-medium transition disabled:opacity-30 disabled:cursor-not-allowed"
        >
          {isPosting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
          <span>Post</span>
        </button>
      </form>
    </section>
  );
}
