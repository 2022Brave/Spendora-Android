import React, { useState, useMemo } from 'react';
import { 
  TrendingUp, 
  PieChart, 
  Calendar, 
  DollarSign, 
  ArrowUpRight, 
  Clock, 
  Award,
  Zap
} from 'lucide-react';
import { 
  Transaction, 
  Category, 
  FinancialCycleInfo, 
  TransactionType 
} from '../types';

interface AnalyticsScreenProps {
  transactions: Transaction[];
  categories: Category[];
  cycleInfo: FinancialCycleInfo;
}

export const AnalyticsScreen: React.FC<AnalyticsScreenProps> = ({
  transactions,
  categories,
  cycleInfo
}) => {
  const [selectedCycle, setSelectedCycle] = useState<string>(cycleInfo.cycleMonthYear);

  // Available cycles
  const availableCycles = useMemo(() => {
    const set = new Set(transactions.map(t => t.cycleMonthYear));
    if (!set.has(cycleInfo.cycleMonthYear)) {
      set.add(cycleInfo.cycleMonthYear);
    }
    return Array.from(set).sort().reverse();
  }, [transactions, cycleInfo]);

  // Filter transactions for chosen cycle
  const cycleTransactions = useMemo(() => {
    return transactions.filter(
      t => t.cycleMonthYear === selectedCycle && t.reviewStatus !== 'DISMISSED'
    );
  }, [transactions, selectedCycle]);

  const expenseTxns = cycleTransactions.filter(
    t => t.transactionType === TransactionType.EXPENSE || t.transactionType === TransactionType.CASH_WITHDRAWAL
  );

  const totalExpense = expenseTxns.reduce((sum, t) => sum + t.amount, 0);
  const totalIncome = cycleTransactions
    .filter(t => t.transactionType === TransactionType.INCOME)
    .reduce((sum, t) => sum + t.amount, 0);

  // Daily Average (over total days in cycle)
  const daysInCycle = cycleInfo.totalDays || 30;
  const dailyAverage = totalExpense > 0 ? (totalExpense / daysInCycle) : 0;

  // Largest Transaction
  const largestTxn = [...expenseTxns].sort((a, b) => b.amount - a.amount)[0] || null;

  // Category Breakdown
  const categoryStats = useMemo(() => {
    const map = new Map<string, { category: Category | null; amount: number; count: number }>();

    for (const txn of expenseTxns) {
      const catId = txn.categoryId || 'uncategorized';
      const existing = map.get(catId) || {
        category: categories.find(c => c.id === catId) || null,
        amount: 0,
        count: 0
      };
      existing.amount += txn.amount;
      existing.count += 1;
      map.set(catId, existing);
    }

    return Array.from(map.values())
      .sort((a, b) => b.amount - a.amount);
  }, [expenseTxns, categories]);

  // Daily spending trend data (grouped by date)
  const dailyTrendData = useMemo(() => {
    const dayMap = new Map<string, number>();
    for (const t of expenseTxns) {
      const dateKey = new Date(t.occurredTimestamp).toISOString().substring(0, 10);
      dayMap.set(dateKey, (dayMap.get(dateKey) || 0) + t.amount);
    }

    const sortedDates = Array.from(dayMap.keys()).sort();
    return sortedDates.map(date => ({
      date,
      label: new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      amount: dayMap.get(date) || 0
    }));
  }, [expenseTxns]);

  // SVG Chart Calculations
  const maxTrendAmount = Math.max(...dailyTrendData.map(d => d.amount), 100);

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      
      {/* Header & Cycle Switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-neutral-100 font-display">
            Financial Analytics
          </h1>
          <p className="text-xs text-neutral-400 mt-0.5">
            Spending patterns, category distributions, and daily velocity
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Calendar className="w-4 h-4 text-purple-400" />
          <select
            value={selectedCycle}
            onChange={(e) => setSelectedCycle(e.target.value)}
            className="bg-neutral-900 border border-neutral-800 rounded-xl px-3 py-2 text-xs font-bold text-neutral-200 focus:border-purple-500 focus:outline-none"
          >
            {availableCycles.map(c => (
              <option key={c} value={c}>Reporting Cycle: {c}</option>
            ))}
          </select>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block mb-1">Total Cycle Spend</span>
          <div className="text-2xl font-black text-rose-400 tracking-tight">
            ₹{totalExpense.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            {expenseTxns.length} debits recorded
          </span>
        </div>

        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block mb-1">Cycle Daily Velocity</span>
          <div className="text-2xl font-black text-purple-300 tracking-tight">
            ₹{dailyAverage.toLocaleString('en-IN', { maximumFractionDigits: 0 })}/day
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            Averaged over {daysInCycle} cycle days
          </span>
        </div>

        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block mb-1">Cycle Inflow / Income</span>
          <div className="text-2xl font-black text-emerald-400 tracking-tight">
            ₹{totalIncome.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block">
            Savings Rate: {totalIncome > 0 ? Math.max(0, Math.round(((totalIncome - totalExpense) / totalIncome) * 100)) : 0}%
          </span>
        </div>

        <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md">
          <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block mb-1">Largest Transaction</span>
          <div className="text-2xl font-black text-neutral-100 tracking-tight truncate">
            {largestTxn ? `₹${largestTxn.amount.toLocaleString('en-IN')}` : '₹0'}
          </div>
          <span className="text-[11px] text-neutral-500 mt-1 block truncate">
            {largestTxn ? largestTxn.merchant : 'No debits'}
          </span>
        </div>
      </div>

      {/* Daily Spending Trend Chart */}
      <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-neutral-100 flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-purple-400" />
              <span>Daily Spending Timeline</span>
            </h3>
            <p className="text-xs text-neutral-400 mt-0.5">Expenditure peaks across the selected financial cycle</p>
          </div>
        </div>

        {dailyTrendData.length === 0 ? (
          <div className="h-48 flex items-center justify-center text-xs text-neutral-500">
            No expenses recorded in this reporting period.
          </div>
        ) : (
          <div className="pt-4">
            <div className="h-52 w-full flex items-end gap-2 sm:gap-4 overflow-x-auto pb-6 pt-2 scrollbar-thin">
              {dailyTrendData.map((point) => {
                const heightPercent = Math.max(8, Math.round((point.amount / maxTrendAmount) * 100));
                return (
                  <div key={point.date} className="flex flex-col items-center gap-2 group min-w-[36px] flex-1">
                    <div className="relative w-full flex flex-col items-center">
                      {/* Tooltip on hover */}
                      <div className="opacity-0 group-hover:opacity-100 absolute -top-8 px-2 py-1 bg-neutral-800 border border-neutral-700 text-purple-300 font-bold text-[10px] rounded-lg shadow-lg whitespace-nowrap transition pointer-events-none z-20">
                        ₹{point.amount.toLocaleString('en-IN')}
                      </div>

                      {/* Bar Column */}
                      <div className="w-full max-w-[28px] h-36 bg-neutral-950/60 rounded-xl flex items-end p-1">
                        <div 
                          className="w-full bg-gradient-to-t from-purple-700 to-indigo-500 rounded-lg group-hover:from-purple-500 group-hover:to-indigo-400 transition-all duration-300 shadow-sm"
                          style={{ height: `${heightPercent}%` }}
                        />
                      </div>
                    </div>

                    <span className="text-[10px] text-neutral-400 whitespace-nowrap font-medium">
                      {point.label}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* Category Breakdown & Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Category Ranking List */}
        <div className="lg:col-span-2 p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-neutral-100 flex items-center gap-2">
              <PieChart className="w-4 h-4 text-purple-400" />
              <span>Category Expenditure Breakdown</span>
            </h3>
            <span className="text-xs text-neutral-400">{categoryStats.length} Categories</span>
          </div>

          {categoryStats.length === 0 ? (
            <div className="p-8 text-center text-xs text-neutral-500">
              No categorized expenses in this cycle.
            </div>
          ) : (
            <div className="space-y-3 pt-1">
              {categoryStats.map((item) => {
                const pct = totalExpense > 0 ? Math.round((item.amount / totalExpense) * 100) : 0;
                const name = item.category?.name || 'Uncategorized';
                const color = item.category?.colorHex || '#9C27B0';

                return (
                  <div key={name} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-2 font-semibold text-neutral-200">
                        <span 
                          className="w-2.5 h-2.5 rounded-full" 
                          style={{ backgroundColor: color }}
                        />
                        <span>{name}</span>
                        <span className="text-[10px] text-neutral-500 font-normal">
                          ({item.count} txn{item.count > 1 ? 's' : ''})
                        </span>
                      </div>
                      <div className="text-right">
                        <span className="font-bold text-neutral-100">
                          ₹{item.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </span>
                        <span className="text-neutral-500 ml-1.5 font-medium">({pct}%)</span>
                      </div>
                    </div>

                    <div className="w-full h-2 rounded-full bg-neutral-950 overflow-hidden">
                      <div 
                        className="h-full rounded-full transition-all duration-300"
                        style={{ width: `${pct}%`, backgroundColor: color }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Category Share Donut / Highlights */}
        <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-4 flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-bold text-neutral-100">Top Spend Summary</h3>
            <p className="text-xs text-neutral-400 mt-0.5">Primary outflow drivers</p>
          </div>

          <div className="space-y-3 py-4">
            {categoryStats.slice(0, 4).map((item, idx) => {
              const pct = totalExpense > 0 ? Math.round((item.amount / totalExpense) * 100) : 0;
              const name = item.category?.name || 'Uncategorized';
              const color = item.category?.colorHex || '#9C27B0';

              return (
                <div key={name} className="p-3 rounded-2xl bg-neutral-950 border border-neutral-800/80 flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div 
                      className="w-8 h-8 rounded-xl flex items-center justify-center text-white text-xs font-bold"
                      style={{ backgroundColor: color }}
                    >
                      #{idx + 1}
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-neutral-200">{name}</h4>
                      <p className="text-[10px] text-neutral-400">{pct}% of cycle total</p>
                    </div>
                  </div>
                  <span className="text-xs font-black text-neutral-100">
                    ₹{item.amount.toLocaleString('en-IN')}
                  </span>
                </div>
              );
            })}
          </div>

          <div className="p-3.5 rounded-2xl bg-purple-950/20 border border-purple-800/30 text-[11px] text-purple-300 leading-relaxed">
            All analytics calculations execute deterministically on-device without cloud communication.
          </div>
        </div>
      </div>
    </div>
  );
};
