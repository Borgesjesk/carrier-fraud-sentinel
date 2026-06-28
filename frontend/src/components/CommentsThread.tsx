import { useEffect, useState, FormEvent } from 'react';
import { MessageSquare, Send, Loader2, AlertCircle, User } from 'lucide-react';
import { commentService } from '../api/commentService';
import type { Comment } from '../types/Comment';

const ROLE_STYLES: Record<string, string> = {
  ADMIN: 'bg-red-500/15 text-red-300',
  ANALYST: 'bg-sky-500/15 text-sky-300',
  COMPLIANCE: 'bg-violet-500/15 text-violet-300',
  CLIENT: 'bg-emerald-500/15 text-emerald-300',
};

interface CommentsThreadProps {
  alertId: string;
}

export function CommentsThread({ alertId }: CommentsThreadProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [content, setContent] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isPosting, setIsPosting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    commentService.list(alertId)
      .then((data) => { if (!cancelled) setComments(data); })
      .catch(() => { if (!cancelled) setError('Failed to load comments.'); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, [alertId]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!content.trim() || isPosting) return;

    setIsPosting(true);
    setError(null);
    try {
      const newComment = await commentService.create(alertId, { content: content.trim() });
      setComments((prev) => [...prev, newComment]);
      setContent('');
    } catch {
      setError('Failed to post comment. Try again.');
    } finally {
      setIsPosting(false);
    }
  };

  const formatDate = (iso: string) => new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit',
  });

  return (
    <section className="bg-slate-900/50 border border-slate-800 rounded-lg p-6">
      <div className="flex items-center gap-2 mb-4">
        <MessageSquare className="w-4 h-4 text-slate-400" />
        <h2 className="text-sm font-medium text-slate-200 uppercase tracking-wide">
          Comments ({comments.length})
        </h2>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 text-slate-400 py-6 justify-center">
          <Loader2 className="w-4 h-4 animate-spin" />
          <span className="text-sm">Loading comments...</span>
        </div>
      )}

      {!isLoading && comments.length === 0 && (
        <p className="text-sm text-slate-500 text-center py-6">
          No comments yet. Be the first to comment.
        </p>
      )}

      {!isLoading && comments.length > 0 && (
        <ul className="space-y-3 mb-4">
          {comments.map((comment) => (
            <li key={comment.commentId} className="bg-slate-800/30 border border-slate-800 rounded-lg p-3">
              <div className="flex items-center gap-2 mb-2">
                <User className="w-3.5 h-3.5 text-slate-500" />
                <span className="text-sm font-medium text-slate-200">{comment.author}</span>
                <span className={`px-2 py-0.5 text-xs rounded ${ROLE_STYLES[comment.authorRole] || 'bg-slate-700 text-slate-300'}`}>
                  {comment.authorRole}
                </span>
                <span className="text-xs text-slate-500 ml-auto">{formatDate(comment.createdAt)}</span>
              </div>
              <p className="text-sm text-slate-300 whitespace-pre-wrap break-words">{comment.content}</p>
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
          placeholder="Add a comment..."
          maxLength={2000}
          rows={2}
          disabled={isPosting}
          className="flex-1 bg-slate-800/50 border border-slate-700 rounded-lg px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition disabled:opacity-50 resize-none"
        />
        <button
          type="submit"
          disabled={isPosting || !content.trim()}
          className="flex items-center gap-2 px-4 py-2 bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 border border-sky-500/30 rounded-lg text-sm font-medium transition disabled:opacity-30 disabled:cursor-not-allowed"
        >
          {isPosting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
          <span>Post</span>
        </button>
      </form>
    </section>
  );
}
