import React from 'react';
import { 
  Calendar, 
  TrendingUp, 
  TrendingDown, 
  PiggyBank, 
  AlertCircle, 
  Wallet, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Clock, 
  ChevronRight, 
  Plus, 
  Sparkles,
  MapPin
} from 'lucide-react';
import { 
  Transaction, 
  Account, 
  Category, 
  Budget, 
  FinancialCycleInfo, 
  TransactionType 
} from '../types';

interface DashboardScreenProps {
  cycleInfo: FinancialCycleInfo;
  transactions: Transaction[];
  accounts: Account[];
  categories: Category[];
  budgets: Budget[];
  pendingCount: number;
  onNavigate: (tab: string) => void;
  onOpenAddModal: () => void;
  onSelectTransaction: (txn: Transaction) => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  cycleInfo,
  transactions,
  accounts,
  categories,
  budgets,
  pendingCount,
  onNavigate,
  onOpenAddModal,
  onSelectTransaction
}) => {
  // Filter transactions belonging to active financial cycle
  const cycleTransactions = transactions.filter(
    t => t.cycleMonthYear === cycleInfo.cycleMonthYear && t.reviewStatus !== 'DISMISSED'
  );

  // Financial sums
  const totalSpent = cycleTransactions
    .filter(t => t.transactionType === TransactionType.EXPENSE || t.transactionType === TransactionType.CASH_WITHDRAWAL)
    .reduce((sum, t) => sum + t.amount, 0);

  const totalEarned = cycleTransactions
    .filter(t => t.transactionType === TransactionType.INCOME)
    .reduce((sum, t) => sum + t.amount, 0);

  const netSavings = totalEarned - totalSpent;

  // Overall cycle budget
  const overallBudget = budgets.find(b => b.categoryId === null && b.cycleMonthYear === cycleInfo.cycleMonthYear) 
    || budgets.find(b => b.categoryId === null);

  const budgetLimit = overallBudget ? overallBudget.amountLimit : 0;
  const budgetPercent = budgetLimit > 0 ? Math.round((totalSpent / budgetLimit) * 100) : 0;
  const budgetRemaining = Math.max(0, budgetLimit - totalSpent);
  const isOverBudget = budgetLimit > 0 && totalSpent > budgetLimit;
  const isBudgetWarning = budgetLimit > 0 && budgetPercent >= (overallBudget?.thresholdPercent || 80);

  // Total Account Balances
  const totalLiquidBalance = accounts
    .filter(a => !a.isArchived)
    .reduce((sum, a) => sum + a.currentBalance, 0);

  // Recent 6 transactions
  const recentTransactions = [...transactions]
    .filter(t => t.reviewStatus !== 'DISMISSED')
    .sort((a, b) => b.occurredTimestamp - a.occurredTimestamp)
    .slice(0, 6);

  const getCategory = (catId?: string | null) => categories.find(c => c.id === catId);
  const getAccount = (accId?: string | null) => accounts.find(a => a.id === accId);

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      
      {/* Pending Review Banner */}
      {pendingCount > 0 && (
        <div 
          id="pending-review-banner"
          onClick={() => onNavigate('pending')}
          className="cursor-pointer group p-4 rounded-3xl bg-gradient-to-r from-amber-500/15 via-purple-900/20 to-amber-500/10 border border-amber-500/30 flex items-center justify-between shadow-lg transition hover:border-amber-500/50"
        >
          <div className="flex items-center gap-3.5">
            <div className="w-10 h-10 rounded-2xl bg-amber-500/20 border border-amber-500/40 text-amber-300 flex items-center justify-center shrink-0">
              <AlertCircle className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h4 className="text-sm font-bold text-amber-200 flex items-center gap-2">
                <span>{pendingCount} Ambiguous Transaction{pendingCount > 1 ? 's' : ''} Awaiting Review</span>
                <span className="text-[10px] uppercase tracking-wider px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 font-extrabold">
                  Action Required
                </span>
              </h4>
              <p className="text-xs text-neutral-400 mt-0.5">
                Verify auto-parsed bank SMS details to confirm or purge raw excerpts.
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1 text-xs font-bold text-amber-400 group-hover:translate-x-1 transition">
            <span>Review</span>
            <ChevronRight className="w-4 h-4" />
          </div>
        </div>
      )}

      {/* Cycle Header Card */}
      <div className="p-6 rounded-3xl bg-gradient-to-br from-neutral-900 via-neutral-900 to-purple-950/40 border border-neutral-800 shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-purple-600/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20"></div>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 relative z-10">
          <div>
            <div className="flex items-center gap-2 text-purple-400 text-xs font-bold uppercase tracking-wider mb-1">
              <Calendar className="w-4 h-4" />
              <span>Active Financial Reporting Period</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-neutral-100 font-display">
              {cycleInfo.displayLabel} Cycle
            </h1>
            <p className="text-xs text-neutral-400 mt-1">
              From <strong className="text-neutral-200">{cycleInfo.startDate}</strong> to <strong className="text-neutral-200">{cycleInfo.endDate}</strong> (Day {cycleInfo.cycleStartDay} rollover)
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => onNavigate('settings')}
              className="px-3.5 py-2 rounded-xl bg-neutral-800/80 hover:bg-neutral-800 border border-neutral-700 text-neutral-300 text-xs font-semibold transition"
            >
              Change Start Day
            </button>
            <div className="px-4 py-2 rounded-2xl bg-purple-950/40 border border-purple-800/50 text-right">
              <span className="text-[10px] uppercase tracking-wider text-purple-300 font-bold block">Days Left</span>
              <span className="text-lg font-black text-purple-200">{cycleInfo.daysRemaining} of {cycleInfo.totalDays}d</span>
            </div>
          </div>
        </div>

        {/* Cycle Progress Bar */}
        <div className="mt-5 space-y-1.5 relative z-10">
          <div className="flex items-center justify-between text-xs text-neutral-400">
            <span>Cycle Timeline Progress</span>
            <span className="font-semibold text-purple-300">{cycleInfo.progressPercent}% elapsed</span>
          </div>
          <div className="w-full h-2 rounded-full bg-neutral-800 overflow-hidden">
            <div 
              className="h-full bg-gradient-to-r from-purple-600 to-indigo-500 rounded-full transition-all duration-500"
              style={{ width: `${cycleInfo.progressPercent}%` }}
            />
          </div>
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        
        {/* Total Spent */}
        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">Total Cycle Spend</span>
            <div className="w-8 h-8 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center">
              <ArrowUpRight className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-rose-400 tracking-tight">
            ₹{totalSpent.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-[11px] text-neutral-500 mt-1">
            Across {cycleTransactions.filter(t => t.transactionType === 'EXPENSE').length} expense transactions
          </p>
        </div>

        {/* Total Earned */}
        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">Cycle Inflow</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <ArrowDownLeft className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-black text-emerald-400 tracking-tight">
            ₹{totalEarned.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-[11px] text-neutral-500 mt-1">
            Salary & credits recorded
          </p>
        </div>

        {/* Net Savings */}
        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold text-neutral-400 uppercase tracking-wider">Net Cash Flow</span>
            <div className={`w-8 h-8 rounded-xl ${netSavings >= 0 ? 'bg-purple-500/10 text-purple-400' : 'bg-rose-500/10 text-rose-400'} flex items-center justify-center`}>
              <PiggyBank className="w-4 h-4" />
            </div>
          </div>
          <div className={`text-2xl font-black tracking-tight ${netSavings >= 0 ? 'text-purple-300' : 'text-rose-400'}`}>
            {netSavings >= 0 ? '+' : ''}₹{netSavings.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <p className="text-[11px] text-neutral-500 mt-1">
            {netSavings >= 0 ? 'Surplus retained this cycle' : 'Deficit in current cycle'}
          </p>
        </div>
      </div>

      {/* Cycle Budget Progress Box */}
      {overallBudget && (
        <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
                <PiggyBank className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-neutral-100">Overall Cycle Budget</h3>
                <p className="text-[11px] text-neutral-400">Limit: ₹{budgetLimit.toLocaleString('en-IN')}</p>
              </div>
            </div>
            <div>
              <span className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                isOverBudget 
                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' 
                  : isBudgetWarning 
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30' 
                  : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              }`}>
                {isOverBudget ? 'Exceeded Budget' : isBudgetWarning ? 'Warning (>80%)' : `${budgetPercent}% Used`}
              </span>
            </div>
          </div>

          <div className="space-y-2">
            <div className="w-full h-3.5 rounded-full bg-neutral-950 overflow-hidden p-0.5 border border-neutral-800">
              <div 
                className={`h-full rounded-full transition-all duration-500 ${
                  isOverBudget 
                    ? 'bg-rose-500' 
                    : isBudgetWarning 
                    ? 'bg-amber-500' 
                    : 'bg-gradient-to-r from-purple-500 to-indigo-500'
                }`}
                style={{ width: `${Math.min(100, budgetPercent)}%` }}
              />
            </div>

            <div className="flex items-center justify-between text-xs text-neutral-400">
              <span>Spent: <strong className="text-neutral-200">₹{totalSpent.toLocaleString('en-IN')}</strong></span>
              <span>
                Remaining: <strong className={isOverBudget ? 'text-rose-400' : 'text-emerald-400'}>
                  ₹{budgetRemaining.toLocaleString('en-IN')}
                </strong>
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Account Balances Grid */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Wallet className="w-4 h-4 text-purple-400" />
            <h3 className="text-sm font-bold text-neutral-200">Accounts & Wallets</h3>
          </div>
          <button 
            onClick={() => onNavigate('accounts')} 
            className="text-xs font-semibold text-purple-400 hover:text-purple-300 flex items-center gap-1"
          >
            <span>Manage All ({accounts.length})</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {accounts.filter(a => !a.isArchived).map((acc) => (
            <div 
              key={acc.id}
              className="p-4 rounded-2xl bg-neutral-900/80 border border-neutral-800 hover:border-neutral-700 transition"
            >
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-semibold text-neutral-200 truncate">{acc.name}</span>
                <span className="text-[10px] font-bold text-neutral-400 uppercase">
                  {acc.type.replace('_', ' ')}
                </span>
              </div>
              <div className="text-lg font-black text-neutral-100 mt-1">
                ₹{acc.currentBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </div>
              <div className="text-[11px] text-neutral-500 mt-0.5">
                {acc.institutionName || 'Self Managed'} {acc.maskedNumber ? `• •••• ${acc.maskedNumber}` : ''}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Activity Section */}
      <div className="space-y-3 pt-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-purple-400" />
            <h3 className="text-sm font-bold text-neutral-200">Recent Transactions</h3>
          </div>
          <button 
            onClick={() => onNavigate('transactions')} 
            className="text-xs font-semibold text-purple-400 hover:text-purple-300 flex items-center gap-1"
          >
            <span>View All ({transactions.length})</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="divide-y divide-neutral-800/80 rounded-3xl bg-neutral-900/90 border border-neutral-800 overflow-hidden shadow-lg">
          {recentTransactions.length === 0 ? (
            <div className="p-8 text-center text-neutral-500 text-xs">
              No transactions recorded yet. Tap <strong>+ Add Transaction</strong> or test the <strong>SMS Engine</strong>.
            </div>
          ) : (
            recentTransactions.map((txn) => {
              const cat = getCategory(txn.categoryId);
              const acc = getAccount(txn.accountId);
              const isExp = txn.transactionType === TransactionType.EXPENSE || txn.transactionType === TransactionType.CASH_WITHDRAWAL;
              const dateStr = new Date(txn.occurredTimestamp).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric'
              });

              return (
                <div
                  key={txn.id}
                  onClick={() => onSelectTransaction(txn)}
                  className="p-4 flex items-center justify-between hover:bg-neutral-800/40 cursor-pointer transition"
                >
                  <div className="flex items-center gap-3.5 min-w-0">
                    <div 
                      className="w-10 h-10 rounded-2xl flex items-center justify-center text-white shrink-0 shadow-sm"
                      style={{ backgroundColor: cat?.colorHex || '#607D8B' }}
                    >
                      <span className="font-bold text-xs">
                        {(cat?.name || txn.merchant || 'T').substring(0, 2).toUpperCase()}
                      </span>
                    </div>

                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-neutral-100 truncate">
                          {txn.merchant || 'Transaction'}
                        </span>
                        {txn.latitude != null && (
                          <MapPin className="w-3 h-3 text-purple-400 shrink-0" />
                        )}
                      </div>
                      <p className="text-[11px] text-neutral-400 truncate">
                        {cat?.name || 'Uncategorized'} • {acc?.name || txn.maskedAccountIdentifier || 'Account'} • {dateStr}
                      </p>
                    </div>
                  </div>

                  <div className="text-right shrink-0">
                    <div className={`text-xs sm:text-sm font-black ${isExp ? 'text-rose-400' : 'text-emerald-400'}`}>
                      {isExp ? '-' : '+'}₹{txn.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </div>
                    <span className="text-[10px] text-neutral-500 uppercase font-semibold">
                      {txn.transactionType.replace('_', ' ')}
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
