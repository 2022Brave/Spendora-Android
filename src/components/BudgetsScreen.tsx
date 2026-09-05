import React, { useState } from 'react';
import { 
  PiggyBank, 
  Plus, 
  AlertTriangle, 
  CheckCircle2, 
  Trash2, 
  Edit3, 
  Sparkles,
  Calendar
} from 'lucide-react';
import { 
  Budget, 
  Category, 
  Transaction, 
  TransactionType, 
  FinancialCycleInfo 
} from '../types';

interface BudgetsScreenProps {
  budgets: Budget[];
  categories: Category[];
  transactions: Transaction[];
  cycleInfo: FinancialCycleInfo;
  onOpenAddBudget: (budget?: Budget | null) => void;
  onDeleteBudget: (id: string) => void;
}

export const BudgetsScreen: React.FC<BudgetsScreenProps> = ({
  budgets,
  categories,
  transactions,
  cycleInfo,
  onOpenAddBudget,
  onDeleteBudget
}) => {
  // Filter cycle transactions
  const cycleExpenses = transactions.filter(
    t => t.cycleMonthYear === cycleInfo.cycleMonthYear && 
         (t.transactionType === TransactionType.EXPENSE || t.transactionType === TransactionType.CASH_WITHDRAWAL) &&
         t.reviewStatus !== 'DISMISSED'
  );

  const totalSpentInCycle = cycleExpenses.reduce((sum, t) => sum + t.amount, 0);

  // Overall budget
  const overallBudget = budgets.find(b => b.categoryId === null);
  const categoryBudgets = budgets.filter(b => b.categoryId !== null);

  const getCategory = (catId?: string | null) => categories.find(c => c.id === catId);

  const calculateCategorySpend = (catId: string | null) => {
    if (!catId) return totalSpentInCycle;
    return cycleExpenses
      .filter(t => t.categoryId === catId)
      .reduce((sum, t) => sum + t.amount, 0);
  };

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-neutral-100 font-display">
            Budgets & Thresholds
          </h1>
          <p className="text-xs text-neutral-400 mt-0.5">
            Real-time tracking for {cycleInfo.displayLabel} cycle ({cycleInfo.daysRemaining} days remaining)
          </p>
        </div>

        <button
          onClick={() => onOpenAddBudget(null)}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Budget Limit</span>
        </button>
      </div>

      {/* Overall Budget Showcase */}
      {overallBudget ? (
        (() => {
          const spent = totalSpentInCycle;
          const limit = overallBudget.amountLimit;
          const pct = Math.round((spent / limit) * 100);
          const remaining = Math.max(0, limit - spent);
          const isOver = spent > limit;
          const isWarning = pct >= overallBudget.thresholdPercent;

          return (
            <div className="p-6 rounded-3xl bg-gradient-to-br from-neutral-900 via-neutral-900 to-purple-950/30 border border-neutral-800 shadow-xl relative overflow-hidden space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center border border-purple-500/30">
                    <PiggyBank className="w-6 h-6" />
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-purple-400 tracking-wider">Primary Target</span>
                    <h2 className="text-lg font-bold text-neutral-100">Overall Cycle Budget</h2>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => onOpenAddBudget(overallBudget)}
                    className="p-2 rounded-xl text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800 transition"
                    title="Edit limit"
                  >
                    <Edit3 className="w-4 h-4" />
                  </button>
                  <span className={`text-xs font-bold px-3 py-1 rounded-full ${
                    isOver 
                      ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40' 
                      : isWarning 
                      ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40' 
                      : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                  }`}>
                    {isOver ? 'Exceeded Budget' : isWarning ? `Warning (${pct}%)` : `${pct}% Consumed`}
                  </span>
                </div>
              </div>

              {/* Progress bar */}
              <div className="space-y-2">
                <div className="w-full h-4 rounded-full bg-neutral-950 p-0.5 border border-neutral-800">
                  <div 
                    className={`h-full rounded-full transition-all duration-500 ${
                      isOver 
                        ? 'bg-rose-500' 
                        : isWarning 
                        ? 'bg-amber-500' 
                        : 'bg-gradient-to-r from-purple-500 to-indigo-500'
                    }`}
                    style={{ width: `${Math.min(100, pct)}%` }}
                  />
                </div>

                <div className="flex items-center justify-between text-xs text-neutral-400">
                  <span>Spent: <strong className="text-neutral-100">₹{spent.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></span>
                  <span>Limit: <strong className="text-neutral-100">₹{limit.toLocaleString('en-IN')}</strong></span>
                  <span>
                    Remaining: <strong className={isOver ? 'text-rose-400' : 'text-emerald-400'}>
                      {isOver ? '-' : ''}₹{remaining.toLocaleString('en-IN')}
                    </strong>
                  </span>
                </div>
              </div>
            </div>
          );
        })()
      ) : (
        <div className="p-8 rounded-3xl bg-neutral-900 border border-dashed border-neutral-800 text-center space-y-3">
          <PiggyBank className="w-10 h-10 text-neutral-600 mx-auto" />
          <div>
            <h3 className="text-sm font-bold text-neutral-200">No Overall Budget Configured</h3>
            <p className="text-xs text-neutral-500 mt-0.5">
              Set a total spending target for your financial cycle to keep overspending in check.
            </p>
          </div>
          <button
            onClick={() => onOpenAddBudget(null)}
            className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold transition"
          >
            Configure Overall Budget
          </button>
        </div>
      )}

      {/* Category Budgets Grid */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-neutral-200 uppercase tracking-wider">
            Category Budgets ({categoryBudgets.length})
          </h2>
          <span className="text-xs text-neutral-500">Alert threshold triggers automatically</span>
        </div>

        {categoryBudgets.length === 0 ? (
          <div className="p-8 rounded-3xl bg-neutral-900/60 border border-neutral-800 text-center text-xs text-neutral-500">
            No category-specific limits yet. Tap <strong>Add Budget Limit</strong> to cap Dining, Groceries, or Shopping individually.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {categoryBudgets.map((b) => {
              const cat = getCategory(b.categoryId);
              const spent = calculateCategorySpend(b.categoryId);
              const limit = b.amountLimit;
              const pct = Math.round((spent / limit) * 100);
              const remaining = Math.max(0, limit - spent);
              const isOver = spent > limit;
              const isWarning = pct >= b.thresholdPercent;

              return (
                <div 
                  key={b.id}
                  className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md space-y-3 hover:border-neutral-700 transition"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div 
                        className="w-9 h-9 rounded-xl flex items-center justify-center text-white text-xs font-bold shadow-sm"
                        style={{ backgroundColor: cat?.colorHex || '#9C27B0' }}
                      >
                        {(cat?.name || 'C').substring(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <h4 className="text-xs font-bold text-neutral-100">{cat?.name || 'Category'}</h4>
                        <p className="text-[10px] text-neutral-400">Limit: ₹{limit.toLocaleString('en-IN')}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <button
                        onClick={() => onOpenAddBudget(b)}
                        className="p-1.5 rounded-lg text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800"
                      >
                        <Edit3 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => onDeleteBudget(b.id)}
                        className="p-1.5 rounded-lg text-rose-400/80 hover:text-rose-400 hover:bg-rose-500/10"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <div className="w-full h-2.5 rounded-full bg-neutral-950 p-0.5 border border-neutral-800">
                      <div 
                        className={`h-full rounded-full transition-all duration-300 ${
                          isOver ? 'bg-rose-500' : isWarning ? 'bg-amber-500' : 'bg-purple-500'
                        }`}
                        style={{ width: `${Math.min(100, pct)}%` }}
                      />
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-neutral-400">
                      <span>Spent: <strong className="text-neutral-200">₹{spent.toLocaleString('en-IN')}</strong> ({pct}%)</span>
                      <span className={isOver ? 'text-rose-400 font-bold' : 'text-neutral-400'}>
                        {isOver ? `Over by ₹${(spent - limit).toLocaleString('en-IN')}` : `₹${remaining.toLocaleString('en-IN')} left`}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
