import React from 'react';
import { 
  AlertCircle, 
  Check, 
  X, 
  Edit3, 
  ShieldCheck, 
  FileText, 
  Info,
  Calendar,
  Lock
} from 'lucide-react';
import { Transaction, Category, Account } from '../types';

interface PendingReviewScreenProps {
  pendingTransactions: Transaction[];
  categories: Category[];
  accounts: Account[];
  onConfirm: (id: string) => void;
  onDismiss: (id: string) => void;
  onEdit: (txn: Transaction) => void;
  onClearAll: () => void;
}

export const PendingReviewScreen: React.FC<PendingReviewScreenProps> = ({
  pendingTransactions,
  categories,
  accounts,
  onConfirm,
  onDismiss,
  onEdit,
  onClearAll
}) => {
  const getCategory = (id?: string | null) => categories.find(c => c.id === id);
  const getAccount = (id?: string | null) => accounts.find(a => a.id === id);

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-neutral-100 font-display">
              Pending SMS Review
            </h1>
            <span className="px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 text-xs font-bold">
              {pendingTransactions.length} Pending
            </span>
          </div>
          <p className="text-xs text-neutral-400 mt-0.5">
            Verify ambiguous SMS alerts before permanent ledger inclusion
          </p>
        </div>

        {pendingTransactions.length > 0 && (
          <button
            onClick={onClearAll}
            className="px-3.5 py-2 rounded-xl bg-neutral-900 hover:bg-neutral-800 text-neutral-400 hover:text-neutral-200 border border-neutral-800 text-xs font-semibold transition"
          >
            Dismiss All
          </button>
        )}
      </div>

      {/* Privacy Guarantee Note */}
      <div className="p-4 rounded-3xl bg-purple-950/20 border border-purple-800/30 flex items-start gap-3">
        <Lock className="w-5 h-5 text-purple-400 shrink-0 mt-0.5" />
        <div className="text-xs text-neutral-300 leading-relaxed">
          <strong className="text-purple-300 block mb-0.5">On-Device Privacy Invariant:</strong>
          Raw SMS bodies are held strictly in temporary review cache. Once confirmed or dismissed, the raw SMS body is permanently destroyed from local storage.
        </div>
      </div>

      {/* Transactions List */}
      {pendingTransactions.length === 0 ? (
        <div className="p-16 rounded-3xl bg-neutral-900/60 border border-neutral-800 text-center space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mx-auto">
            <ShieldCheck className="w-8 h-8" />
          </div>
          <div>
            <h3 className="text-base font-bold text-neutral-200">No Pending Reviews</h3>
            <p className="text-xs text-neutral-500 mt-1 max-w-sm mx-auto">
              All parsed SMS transactions have been verified. Newly received ambiguous alerts will appear here.
            </p>
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {pendingTransactions.map((txn) => {
            const cat = getCategory(txn.categoryId);
            const acc = getAccount(txn.accountId);
            const dateStr = new Date(txn.occurredTimestamp).toLocaleString('en-US', {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            });

            const confidencePercent = Math.round(txn.confidenceScore * 100);

            return (
              <div 
                key={txn.id}
                className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 hover:border-neutral-700 shadow-xl space-y-4 transition"
              >
                {/* Header row */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-neutral-800/80">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-amber-500/10 text-amber-400 flex items-center justify-center font-bold text-sm border border-amber-500/20">
                      ₹
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-neutral-100">
                          {txn.merchant}
                        </span>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-neutral-800 text-neutral-400">
                          {txn.transactionType}
                        </span>
                      </div>
                      <p className="text-[11px] text-neutral-400 mt-0.5">
                        {dateStr} • Account: {acc?.name || txn.maskedAccountIdentifier || 'Unknown'}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-3">
                    <div className="text-right">
                      <div className="text-lg font-black text-rose-400">
                        ₹{txn.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </div>
                      <span className="text-[10px] text-amber-400 font-bold">
                        Confidence: {confidencePercent}%
                      </span>
                    </div>
                  </div>
                </div>

                {/* Raw SMS Excerpt Box */}
                {txn.rawSmsExcerpt && (
                  <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-1">
                    <span className="text-[10px] uppercase font-bold text-neutral-500 flex items-center gap-1.5">
                      <FileText className="w-3.5 h-3.5 text-neutral-400" />
                      Captured SMS Excerpt (Temporary)
                    </span>
                    <p className="text-xs font-mono text-neutral-300 leading-relaxed break-words">
                      "{txn.rawSmsExcerpt}"
                    </p>
                  </div>
                )}

                {/* Actions */}
                <div className="flex items-center justify-end gap-2.5 pt-1">
                  <button
                    onClick={() => onDismiss(txn.id)}
                    className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-semibold transition"
                  >
                    <X className="w-3.5 h-3.5 text-rose-400" />
                    <span>Dismiss</span>
                  </button>

                  <button
                    onClick={() => onEdit(txn)}
                    className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-300 text-xs font-semibold transition"
                  >
                    <Edit3 className="w-3.5 h-3.5 text-purple-400" />
                    <span>Edit & Confirm</span>
                  </button>

                  <button
                    onClick={() => onConfirm(txn.id)}
                    className="flex items-center gap-1.5 px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-md shadow-purple-600/30 transition active:scale-95"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>Confirm & Purge Excerpt</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
