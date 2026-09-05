import React, { useState } from 'react';
import { X, PiggyBank, AlertCircle } from 'lucide-react';
import { Budget, Category, BudgetPeriod } from '../types';

interface BudgetModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (budget: Partial<Budget>) => void;
  categories: Category[];
  cycleMonthYear: string;
  editingBudget?: Budget | null;
}

export const BudgetModal: React.FC<BudgetModalProps> = ({
  isOpen,
  onClose,
  onSave,
  categories,
  cycleMonthYear,
  editingBudget
}) => {
  const [amount, setAmount] = useState<string>(editingBudget ? editingBudget.amountLimit.toString() : '');
  const [categoryId, setCategoryId] = useState<string>(editingBudget?.categoryId || '');
  const [threshold, setThreshold] = useState<number>(editingBudget?.thresholdPercent || 80);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const parsed = parseFloat(amount);
    if (isNaN(parsed) || parsed <= 0) {
      setError('Please enter a valid budget limit greater than 0.');
      return;
    }

    onSave({
      categoryId: categoryId || null,
      amountLimit: parsed,
      period: BudgetPeriod.FINANCIAL_CYCLE,
      cycleMonthYear,
      thresholdPercent: threshold,
      isActive: true
    });

    onClose();
  };

  const expenseCategories = categories.filter(c => c.type === 'EXPENSE');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        id="budget-modal"
        className="w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col"
      >
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/60">
          <div className="flex items-center gap-2">
            <PiggyBank className="w-5 h-5 text-purple-400" />
            <h2 className="text-base font-bold text-neutral-100">
              {editingBudget ? 'Edit Cycle Budget' : 'Create Cycle Budget'}
            </h2>
          </div>
          <button onClick={onClose} className="p-1 rounded-xl text-neutral-400 hover:text-neutral-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400" />
              <span>{error}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5">
              Budget Target Category
            </label>
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-neutral-200 text-sm focus:border-purple-500 focus:outline-none"
            >
              <option value="">Overall Cycle Budget (All Categories)</option>
              {expenseCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            <p className="text-[11px] text-neutral-500 mt-1">
              {categoryId ? 'Sets a spending ceiling specifically for this category.' : 'Sets a total spending ceiling across all expenses in this cycle.'}
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1.5">
              Budget Limit (₹) *
            </label>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-500 font-bold text-base">₹</span>
              <input
                type="number"
                step="100"
                required
                placeholder="25000"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value);
                  setError(null);
                }}
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-8 pr-4 py-2.5 text-neutral-100 font-semibold text-base focus:border-purple-500 focus:outline-none"
              />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">
                Warning Alert Threshold
              </label>
              <span className="text-xs font-bold text-amber-400">{threshold}%</span>
            </div>
            <input
              type="range"
              min="50"
              max="95"
              step="5"
              value={threshold}
              onChange={(e) => setThreshold(parseInt(e.target.value))}
              className="w-full accent-purple-500 cursor-pointer"
            />
            <p className="text-[11px] text-neutral-500 mt-1">
              Shows an amber warning indicator when spending crosses this percentage.
            </p>
          </div>

          <div className="pt-3 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-neutral-800 text-neutral-300 text-xs font-semibold hover:bg-neutral-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-6 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition"
            >
              Save Budget
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
