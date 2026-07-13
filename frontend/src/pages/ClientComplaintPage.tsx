import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, LogOut, Upload, X, FileText, AlertCircle, Loader2, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { complaintService } from '../api/complaintService';
import type { ComplaintType } from '../types/Complaint';
import type { DocumentCategory } from '../types/Alert';
import { AxiosError } from 'axios';
import type { ProblemDetail } from '../types/ProblemDetail';

const COMPLAINT_TYPES: { value: ComplaintType; label: string }[] = [
  { value: 'PAYMENT', label: 'Payment dispute' },
  { value: 'INSURANCE', label: 'Insurance claim' },
  { value: 'ACCIDENT', label: 'Accident report' },
  { value: 'COMMERCIAL_DISPUTE', label: 'Commercial dispute' },
  { value: 'FRAUD', label: 'Suspected fraud' },
  { value: 'REVIEWING', label: 'Document under review' },
];

const ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
const MAX_SIZE = 10 * 1024 * 1024;
const DOCUMENT_CATEGORIES: { value: DocumentCategory; label: string }[] = [
  { value: 'INVOICE', label: 'Invoice' },
  { value: 'CMR', label: 'CMR' },
  { value: 'LOAD_ORDER', label: 'Orden de carga' },
  { value: 'EMAIL', label: 'Email / texto' },
  { value: 'OTHER', label: 'Other' },
];

export function ClientComplaintPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const [carrierName, setCarrierName] = useState('');
  const [complaintType, setComplaintType] = useState<ComplaintType>('PAYMENT');
  const [description, setDescription] = useState('');
  const [documents, setDocuments] = useState<File[]>([]);
  const [categories, setCategories] = useState<DocumentCategory[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    setError(null);

    for (const file of files) {
      if (!ALLOWED_TYPES.includes(file.type)) {
        setError(`${file.name}: unsupported type. Use PDF, JPEG, PNG, or WebP.`);
        return;
      }
      if (file.size > MAX_SIZE) {
        setError(`${file.name}: exceeds 10 MB limit.`);
        return;
      }
    }
    setDocuments((prev) => [...prev, ...files]);
        setCategories((prev) => [...prev, ...files.map(() => 'OTHER' as DocumentCategory)]);
        event.target.value = '';
  };

  const removeDocument = (index: number) => {
      setDocuments((prev) => prev.filter((_, i) => i !== index));
      setCategories((prev) => prev.filter((_, i) => i !== index));
  };

  const updateCategory = (index: number, category: DocumentCategory) => {
      setCategories((prev) => prev.map((c, i) => (i === index ? category : c)));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (documents.length === 0) {
      setError('Attach at least one document supporting your complaint.');
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await complaintService.submit(
              { carrierName, description, complaintType },
              documents,
              categories
            );
      setSuccess(`Case ${result.alertId} created. Routed to ${result.assignedDepartment}.`);
      setCarrierName('');
      setDescription('');
      setDocuments([]);
      setCategories([]);
      setComplaintType('PAYMENT');
    } catch (err) {
      const axiosError = err as AxiosError<ProblemDetail>;
      setError(axiosError.response?.data?.detail || 'Failed to submit complaint. Try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatBytes = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm">
        <div className="max-w-3xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ShieldCheck className="w-6 h-6 text-sky-400" />
            <h1 className="text-lg font-semibold tracking-tight">FraudSentinel</h1>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <div className="text-sm font-medium text-slate-200">{user?.username}</div>
              <div className="text-xs text-slate-400">CLIENT</div>
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

      <main className="max-w-3xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h2 className="text-2xl font-semibold tracking-tight mb-2">Submit a complaint</h2>
          <p className="text-sm text-slate-400">
            Provide details about the carrier and attach supporting documents. Your case will be routed to the appropriate department for investigation.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6 bg-slate-900/50 border border-slate-800 rounded-lg p-6">
          <div>
            <label htmlFor="carrierName" className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
              Carrier name
            </label>
            <input
              id="carrierName"
              type="text"
              value={carrierName}
              onChange={(e) => setCarrierName(e.target.value)}
              required
              minLength={2}
              maxLength={100}
              disabled={isSubmitting}
              className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-3 py-2.5 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition disabled:opacity-50"
              placeholder="e.g. ACME Logistics SL"
            />
          </div>

          <div>
            <label htmlFor="complaintType" className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
              Complaint type
            </label>
            <select
              id="complaintType"
              value={complaintType}
              onChange={(e) => setComplaintType(e.target.value as ComplaintType)}
              disabled={isSubmitting}
              className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-3 py-2.5 text-slate-100 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition disabled:opacity-50"
            >
              {COMPLAINT_TYPES.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="description" className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
              Description
              <span className="ml-2 text-slate-500 normal-case">({description.length}/2000, min 20)</span>
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              minLength={20}
              maxLength={2000}
              rows={6}
              disabled={isSubmitting}
              className="w-full bg-slate-800/50 border border-slate-700 rounded-lg px-3 py-2.5 text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent transition disabled:opacity-50 resize-none"
              placeholder="Describe what happened, dates involved, amounts in dispute, and any other relevant context..."
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-2 uppercase tracking-wide">
              Supporting documents
              <span className="ml-2 text-slate-500 normal-case">(PDF, JPEG, PNG, WebP · max 10 MB each)</span>
            </label>

            <label className="flex items-center justify-center gap-2 px-4 py-6 bg-slate-800/30 border-2 border-dashed border-slate-700 hover:border-sky-500/50 rounded-lg cursor-pointer transition">
              <Upload className="w-5 h-5 text-slate-400" />
              <span className="text-sm text-slate-300">Click to add documents</span>
              <input
                type="file"
                multiple
                accept=".pdf,image/jpeg,image/png,image/webp"
                onChange={handleFileSelect}
                disabled={isSubmitting}
                className="hidden"
              />
            </label>

            {documents.length > 0 && (
              <ul className="mt-3 space-y-2">
                {documents.map((file, index) => (
                  <li key={index} className="flex items-center gap-3 px-3 py-2 bg-slate-800/50 border border-slate-700 rounded-lg">
                                      <FileText className="w-4 h-4 text-slate-400 flex-shrink-0" />
                                      <div className="flex-1 min-w-0">
                                        <div className="text-sm text-slate-200 truncate">{file.name}</div>
                                        <div className="text-xs text-slate-500">{formatBytes(file.size)}</div>
                                      </div>
                                      <select
                                        value={categories[index] || 'OTHER'}
                                        onChange={(e) => updateCategory(index, e.target.value as DocumentCategory)}
                                        disabled={isSubmitting}
                                        className="bg-slate-900 border border-slate-700 text-slate-200 text-xs rounded px-2 py-1 focus:outline-none focus:ring-1 focus:ring-sky-500"
                                      >
                                        {DOCUMENT_CATEGORIES.map((c) => (
                                          <option key={c.value} value={c.value}>{c.label}</option>
                                        ))}
                                      </select>
                                      <button
                                        type="button"
                                        onClick={() => removeDocument(index)}
                                        disabled={isSubmitting}
                                        className="p-1 text-slate-400 hover:text-red-400 transition"
                                      >
                                        <X className="w-4 h-4" />
                                      </button>
                                    </li>
                ))}
              </ul>
            )}
          </div>

          {error && (
            <div className="flex items-start gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
              <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
              <p className="text-sm text-red-300">{error}</p>
            </div>
          )}

          {success && (
                      <div className="flex items-start gap-2 p-3 bg-emerald-500/10 border border-emerald-500/30 rounded-lg">
                        <CheckCircle2 className="w-4 h-4 text-emerald-400 mt-0.5 flex-shrink-0" />
                        <div className="flex-1">
                          <p className="text-sm text-emerald-300">{success}</p>
                          <button
                            type="button"
                            onClick={() => navigate('/complaints/mine')}
                            className="text-sm text-emerald-300 hover:text-emerald-200 underline mt-1"
                          >
                            View my cases →
                          </button>
                        </div>
                      </div>
                    )}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-gradient-to-r from-sky-500 to-violet-500 hover:from-sky-400 hover:to-violet-400 text-white font-medium py-2.5 px-4 rounded-lg transition shadow-lg shadow-sky-500/20 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Submitting...
              </>
            ) : (
              <>Submit complaint</>
            )}
          </button>
        </form>
      </main>
    </div>
  );
}
