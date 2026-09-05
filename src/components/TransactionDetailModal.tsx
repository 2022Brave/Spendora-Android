import React, { useState } from 'react';
import { 
  X, 
  MapPin, 
  Calendar, 
  Tag, 
  CreditCard, 
  Edit3, 
  Trash2, 
  FileText, 
  Hash, 
  AlertTriangle 
} from 'lucide-react';
import { Transaction, TransactionType, Category, Account } from '../types';

interface TransactionDetailModalProps {
  transaction: Transaction | null;
  onClose: () => void;
  onEdit: (txn: Transaction) => void;
  onDelete: (id: string) => void;
  category?: Category;
  account?: Account;
}

export const TransactionDetailModal: React.FC<TransactionDetailModalProps> = ({
  transaction,
  onClose,
  onEdit,
  onDelete,
  category,
  account
}) => {
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);

  if (!transaction) return null;

  const isExpense = transaction.transactionType === TransactionType.EXPENSE || transaction.transactionType === TransactionType.CASH_WITHDRAWAL;
  const isIncome = transaction.transactionType === TransactionType.INCOME;
  const formattedDate = new Date(transaction.occurredTimestamp).toLocaleString('en-US', {
    dateStyle: 'full',
    timeStyle: 'medium'
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        id="transaction-detail-modal"
        className="w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col"
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/60">
          <div className="flex items-center gap-2">
            <span className={`w-2.5 h-2.5 rounded-full ${isExpense ? 'bg-rose-500' : isIncome ? 'bg-emerald-500' : 'bg-sky-500'}`} />
            <span className="text-xs font-bold text-neutral-400 uppercase tracking-wider">
              {transaction.transactionType.replace('_', ' ')}
            </span>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-5">
          {/* Main Figure */}
          <div className="text-center py-2">
            <div className={`text-3xl sm:text-4xl font-extrabold tracking-tight ${isExpense ? 'text-rose-400' : isIncome ? 'text-emerald-400' : 'text-sky-400'}`}>
              {isExpense ? '-' : isIncome ? '+' : ''}₹{transaction.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
            </div>
            <h3 className="text-lg font-bold text-neutral-100 mt-1">
              {transaction.merchant || 'Unknown Payee'}
            </h3>
            <p className="text-xs text-neutral-400 mt-0.5">{formattedDate}</p>
          </div>

          {/* Details Table */}
          <div className="divide-y divide-neutral-800/80 rounded-2xl bg-neutral-950/60 border border-neutral-800/60 overflow-hidden text-xs">
            <div className="px-4 py-3 flex items-center justify-between">
              <span className="text-neutral-400 flex items-center gap-2">
                <Tag className="w-4 h-4 text-purple-400" />
                Category
              </span>
              <span className="font-semibold text-neutral-200 flex items-center gap-1.5">
                <span 
                  className="w-2.5 h-2.5 rounded-full" 
                  style={{ backgroundColor: category?.colorHex || '#9C27B0' }}
                />
                {category?.name || 'Uncategorized'}
              </span>
            </div>

            <div className="px-4 py-3 flex items-center justify-between">
              <span className="text-neutral-400 flex items-center gap-2">
                <CreditCard className="w-4 h-4 text-indigo-400" />
                Account
              </span>
              <span className="font-semibold text-neutral-200">
                {account?.name || (transaction.maskedAccountIdentifier ? `•••• ${transaction.maskedAccountIdentifier}` : 'Unlinked')}
              </span>
            </div>

            <div className="px-4 py-3 flex items-center justify-between">
              <span className="text-neutral-400 flex items-center gap-2">
                <Calendar className="w-4 h-4 text-neutral-400" />
                Cycle Period
              </span>
              <span className="font-medium text-neutral-300">
                {transaction.cycleMonthYear}
              </span>
            </div>

            <div className="px-4 py-3 flex items-center justify-between">
              <span className="text-neutral-400 flex items-center gap-2">
                <FileText className="w-4 h-4 text-neutral-400" />
                Source
              </span>
              <span className="font-medium text-neutral-300">
                {transaction.source.replace('_', ' ')}
              </span>
            </div>

            {transaction.transactionReferenceNumber && (
              <div className="px-4 py-3 flex items-center justify-between">
                <span className="text-neutral-400 flex items-center gap-2">
                  <Hash className="w-4 h-4 text-neutral-400" />
                  Reference
                </span>
                <span className="font-mono text-neutral-300 text-[11px]">
                  {transaction.transactionReferenceNumber}
                </span>
              </div>
            )}
          </div>

          {/* Location details */}
          {transaction.latitude != null && (
            <div className="p-3.5 rounded-2xl bg-purple-950/20 border border-purple-800/30 flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-purple-600/20 flex items-center justify-center text-purple-400 shrink-0">
                <MapPin className="w-4 h-4" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-semibold text-purple-200 truncate">
                  {transaction.locationLabel || 'Location Tagged'}
                </p>
                <p className="text-[11px] font-mono text-purple-400/80">
                  {transaction.latitude}, {transaction.longitude}
                </p>
              </div>
            </div>
          )}

          {/* Notes */}
          {transaction.notes && (
            <div className="p-3 rounded-xl bg-neutral-950/40 border border-neutral-800 text-xs text-neutral-300">
              <span className="font-bold text-neutral-400 block mb-1">Notes:</span>
              <p>{transaction.notes}</p>
            </div>
          )}

          {/* Delete Confirmation Box */}
          {showConfirmDelete ? (
            <div className="p-4 rounded-2xl bg-rose-950/40 border border-rose-800/40 text-rose-200 space-y-3 animate-fadeIn">
              <div className="flex items-center gap-2 text-rose-300 font-bold text-xs">
                <AlertTriangle className="w-4 h-4" />
                <span>Permanently delete this transaction?</span>
              </div>
              <p className="text-[11px] text-rose-300/80 leading-relaxed">
                This transaction will be removed from your spend reports and account ledger.
              </p>
              <div className="flex items-center gap-2 pt-1">
                <button
                  onClick={() => {
                    onDelete(transaction.id);
                    onClose();
                  }}
                  className="flex-1 py-2 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs transition"
                >
                  Confirm Delete
                </button>
                <button
                  onClick={() => setShowConfirmDelete(false)}
                  className="flex-1 py-2 rounded-xl bg-neutral-800 text-neutral-300 hover:bg-neutral-700 font-semibold text-xs transition"
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            /* Action Buttons */
            <div className="flex items-center gap-3 pt-2">
              <button
                onClick={() => {
                  onClose();
                  onEdit(transaction);
                }}
                className="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-bold border border-neutral-700 transition"
              >
                <Edit3 className="w-3.5 h-3.5 text-purple-400" />
                <span>Edit</span>
              </button>
              <button
                onClick={() => setShowConfirmDelete(true)}
                className="flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 text-xs font-bold border border-rose-500/30 transition"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
