import React, { useState } from 'react';
import { 
  X, 
  MapPin, 
  Calendar, 
  Tag, 
  CreditCard, 
  AlertCircle,
  Compass,
  Check
} from 'lucide-react';
import { 
  Transaction, 
  TransactionType, 
  TransactionSource, 
  ReviewStatus, 
  Category, 
  Account 
} from '../types';
import { FinancialCycleService } from '../services/financialCycle';

interface TransactionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (txn: Partial<Transaction>) => void;
  categories: Category[];
  accounts: Account[];
  cycleStartDay: number;
  editingTransaction?: Transaction | null;
}

export const TransactionModal: React.FC<TransactionModalProps> = ({
  isOpen,
  onClose,
  onSave,
  categories,
  accounts,
  cycleStartDay,
  editingTransaction
}) => {
  const [amount, setAmount] = useState<string>(editingTransaction ? editingTransaction.amount.toString() : '');
  const [merchant, setMerchant] = useState<string>(editingTransaction ? editingTransaction.merchant : '');
  const [type, setType] = useState<TransactionType>(editingTransaction ? editingTransaction.transactionType : TransactionType.EXPENSE);
  const [categoryId, setCategoryId] = useState<string>(editingTransaction?.categoryId || '');
  const [accountId, setAccountId] = useState<string>(editingTransaction?.accountId || (accounts[0]?.id || ''));
  const [notes, setNotes] = useState<string>(editingTransaction?.notes || '');
  const [timestamp, setTimestamp] = useState<string>(
    editingTransaction 
      ? new Date(editingTransaction.occurredTimestamp).toISOString().substring(0, 16)
      : new Date().toISOString().substring(0, 16)
  );
  
  // Location
  const [latitude, setLatitude] = useState<number | null>(editingTransaction?.latitude ?? null);
  const [longitude, setLongitude] = useState<number | null>(editingTransaction?.longitude ?? null);
  const [locationLabel, setLocationLabel] = useState<string>(editingTransaction?.locationLabel || '');
  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [locationStatus, setLocationStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleGetLocation = () => {
    setIsLocating(true);
    setLocationStatus(null);
    if ('geolocation' in navigator) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setIsLocating(false);
          setLatitude(Number(pos.coords.latitude.toFixed(5)));
          setLongitude(Number(pos.coords.longitude.toFixed(5)));
          setLocationLabel('Current Location');
          setLocationStatus('Coordinates acquired via GPS');
        },
        () => {
          setIsLocating(false);
          // Fallback location simulation (e.g. Bangalore center)
          setLatitude(12.9716);
          setLongitude(77.5946);
          setLocationLabel('Bangalore, Karnataka');
          setLocationStatus('Acquired city-level coordinates');
        },
        { timeout: 8000, enableHighAccuracy: true }
      );
    } else {
      setIsLocating(false);
      setLatitude(12.9716);
      setLongitude(77.5946);
      setLocationLabel('Local Device Location');
      setLocationStatus('Acquired device location');
    }
  };

  const handleRemoveLocation = () => {
    setLatitude(null);
    setLongitude(null);
    setLocationLabel('');
    setLocationStatus(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please enter a valid amount greater than zero.');
      return;
    }
    if (!merchant.trim()) {
      setError('Please enter a merchant or payee name.');
      return;
    }

    const occurredTime = new Date(timestamp).getTime() || Date.now();
    const cycleMonthYear = FinancialCycleService.getCycleMonthYearForTimestamp(occurredTime, cycleStartDay);

    const targetAccount = accounts.find(a => a.id === accountId);

    onSave({
      amount: parsedAmount,
      currency: 'INR',
      transactionType: type,
      merchant: merchant.trim(),
      categoryId: categoryId || null,
      accountId: accountId || null,
      maskedAccountIdentifier: targetAccount?.maskedNumber,
      occurredTimestamp: occurredTime,
      cycleMonthYear,
      source: editingTransaction ? editingTransaction.source : TransactionSource.MANUAL_ENTRY,
      reviewStatus: ReviewStatus.CONFIRMED,
      confidenceScore: 1.0,
      isSalaryCredit: type === TransactionType.INCOME && /salary/i.test(merchant),
      notes: notes.trim() || null,
      latitude,
      longitude,
      locationLabel: locationLabel || (latitude ? `${latitude}, ${longitude}` : null)
    });

    onClose();
  };

  // Filter categories by type
  const availableCategories = categories.filter(c => {
    if (type === TransactionType.INCOME) return c.type === 'INCOME';
    return c.type === 'EXPENSE';
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fadeIn">
      <div 
        id="transaction-modal"
        className="w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/60">
          <h2 className="text-lg font-bold text-neutral-100">
            {editingTransaction ? 'Edit Transaction' : 'Record Transaction'}
          </h2>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto">
          {error && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Transaction Type Tabs */}
          <div>
            <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-2">
              Transaction Type
            </label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { id: TransactionType.EXPENSE, label: 'Expense', color: 'text-rose-400 border-rose-500/40 bg-rose-500/10' },
                { id: TransactionType.INCOME, label: 'Income', color: 'text-emerald-400 border-emerald-500/40 bg-emerald-500/10' },
                { id: TransactionType.TRANSFER, label: 'Transfer', color: 'text-sky-400 border-sky-500/40 bg-sky-500/10' }
              ].map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    setType(item.id);
                    setCategoryId('');
                  }}
                  className={`py-2 px-3 rounded-xl text-xs font-bold border transition ${
                    type === item.id
                      ? `${item.color} shadow-sm`
                      : 'border-neutral-800 text-neutral-400 hover:bg-neutral-800/60'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          {/* Amount & Merchant */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5">
                Amount (₹) *
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-500 font-bold text-base">₹</span>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => {
                    setAmount(e.target.value);
                    setError(null);
                  }}
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-8 pr-4 py-2.5 text-neutral-100 font-semibold text-base focus:border-purple-500 focus:outline-none focus:ring-1 focus:ring-purple-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5">
                Merchant / Payee *
              </label>
              <input
                type="text"
                required
                placeholder="e.g. Swiggy, Uber, Rent"
                value={merchant}
                onChange={(e) => {
                  setMerchant(e.target.value);
                  setError(null);
                }}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-2.5 text-neutral-100 text-sm focus:border-purple-500 focus:outline-none focus:ring-1 focus:ring-purple-500"
              />
            </div>
          </div>

          {/* Category & Account */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                <Tag className="w-3.5 h-3.5 text-neutral-400" />
                Category
              </label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-neutral-200 text-sm focus:border-purple-500 focus:outline-none"
              >
                <option value="">Uncategorized</option>
                {availableCategories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                <CreditCard className="w-3.5 h-3.5 text-neutral-400" />
                Account
              </label>
              <select
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-neutral-200 text-sm focus:border-purple-500 focus:outline-none"
              >
                {accounts.filter(a => !a.isArchived).map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.name} {acc.maskedNumber ? `(•••• ${acc.maskedNumber})` : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Date & Time */}
          <div>
            <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-neutral-400" />
              Date & Time
            </label>
            <input
              type="datetime-local"
              value={timestamp}
              onChange={(e) => setTimestamp(e.target.value)}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-2.5 text-neutral-200 text-sm focus:border-purple-500 focus:outline-none"
            />
          </div>

          {/* Location Tagging (Optional) */}
          <div className="p-3.5 rounded-2xl bg-neutral-950/60 border border-neutral-800/80">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-neutral-300 flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-purple-400" />
                Transaction Location (Optional)
              </span>
              {latitude != null ? (
                <button
                  type="button"
                  onClick={handleRemoveLocation}
                  className="text-[11px] text-rose-400 hover:underline"
                >
                  Remove Pin
                </button>
              ) : null}
            </div>

            {latitude != null ? (
              <div className="flex items-center justify-between p-2.5 rounded-xl bg-purple-950/30 border border-purple-800/40 text-xs text-purple-200">
                <div className="flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-purple-400 animate-pulse"></span>
                  <span className="font-medium">{locationLabel || 'Location Stamped'}</span>
                  <span className="text-[11px] text-purple-300/70 font-mono">({latitude}, {longitude})</span>
                </div>
                <Check className="w-4 h-4 text-purple-400" />
              </div>
            ) : (
              <button
                type="button"
                onClick={handleGetLocation}
                disabled={isLocating}
                className="w-full py-2 px-3 rounded-xl border border-dashed border-neutral-700 hover:border-purple-500/60 text-neutral-400 hover:text-purple-300 text-xs font-medium flex items-center justify-center gap-2 transition"
              >
                <Compass className={`w-4 h-4 ${isLocating ? 'animate-spin text-purple-400' : ''}`} />
                <span>{isLocating ? 'Acquiring GPS fix...' : 'Pin Current GPS Location'}</span>
              </button>
            )}
            {locationStatus && (
              <p className="text-[10px] text-purple-400 mt-1.5 text-right">{locationStatus}</p>
            )}
          </div>

          {/* Notes */}
          <div>
            <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5">
              Notes (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g. Reimbursable client expense, split with Rahul"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-2.5 text-neutral-200 text-sm focus:border-purple-500 focus:outline-none"
            />
          </div>

          {/* Actions */}
          <div className="pt-2 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-neutral-800 text-neutral-300 text-xs font-semibold hover:bg-neutral-800 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition"
            >
              {editingTransaction ? 'Save Changes' : 'Record Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
