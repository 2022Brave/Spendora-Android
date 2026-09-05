import React, { useState, useMemo } from 'react';
import { 
  Search, 
  Filter, 
  Plus, 
  Calendar, 
  MapPin, 
  Tag, 
  CreditCard, 
  ArrowUpDown,
  Download,
  AlertCircle
} from 'lucide-react';
import { 
  Transaction, 
  TransactionType, 
  Category, 
  Account, 
  ReviewStatus 
} from '../types';

interface TransactionsScreenProps {
  transactions: Transaction[];
  categories: Category[];
  accounts: Account[];
  onSelectTransaction: (txn: Transaction) => void;
  onOpenAddModal: () => void;
}

export const TransactionsScreen: React.FC<TransactionsScreenProps> = ({
  transactions,
  categories,
  accounts,
  onSelectTransaction,
  onOpenAddModal
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedCycle, setSelectedCycle] = useState<string>('ALL');
  const [sortOrder, setSortOrder] = useState<'desc' | 'asc'>('desc');

  // Extract unique cycles
  const availableCycles = useMemo(() => {
    const set = new Set(transactions.map(t => t.cycleMonthYear));
    return Array.from(set).sort().reverse();
  }, [transactions]);

  // Filter & Sort
  const filteredTransactions = useMemo(() => {
    return transactions.filter(t => {
      if (t.reviewStatus === ReviewStatus.DISMISSED) return false;

      // Type filter
      if (selectedType !== 'ALL' && t.transactionType !== selectedType) {
        return false;
      }

      // Category filter
      if (selectedCategory !== 'ALL') {
        if (selectedCategory === 'UNCATEGORIZED') {
          if (t.categoryId) return false;
        } else if (t.categoryId !== selectedCategory) {
          return false;
        }
      }

      // Cycle filter
      if (selectedCycle !== 'ALL' && t.cycleMonthYear !== selectedCycle) {
        return false;
      }

      // Search
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const merchantMatch = t.merchant.toLowerCase().includes(query);
        const notesMatch = t.notes?.toLowerCase().includes(query);
        const refMatch = t.transactionReferenceNumber?.toLowerCase().includes(query);
        const accMatch = t.maskedAccountIdentifier?.includes(query);
        if (!merchantMatch && !notesMatch && !refMatch && !accMatch) {
          return false;
        }
      }

      return true;
    }).sort((a, b) => {
      return sortOrder === 'desc' 
        ? b.occurredTimestamp - a.occurredTimestamp
        : a.occurredTimestamp - b.occurredTimestamp;
    });
  }, [transactions, selectedType, selectedCategory, selectedCycle, searchQuery, sortOrder]);

  const totalFilteredExpense = filteredTransactions
    .filter(t => t.transactionType === TransactionType.EXPENSE || t.transactionType === TransactionType.CASH_WITHDRAWAL)
    .reduce((sum, t) => sum + t.amount, 0);

  const totalFilteredIncome = filteredTransactions
    .filter(t => t.transactionType === TransactionType.INCOME)
    .reduce((sum, t) => sum + t.amount, 0);

  const getCategory = (catId?: string | null) => categories.find(c => c.id === catId);
  const getAccount = (accId?: string | null) => accounts.find(a => a.id === accId);

  const exportFilteredCsv = () => {
    const headers = ['Date,Type,Merchant,Category,Account,Amount,Currency,Notes,Reference'];
    const rows = filteredTransactions.map(t => {
      const cat = getCategory(t.categoryId)?.name || 'Uncategorized';
      const acc = getAccount(t.accountId)?.name || t.maskedAccountIdentifier || '';
      const date = new Date(t.occurredTimestamp).toISOString();
      return `"${date}","${t.transactionType}","${t.merchant.replace(/"/g, '""')}","${cat}","${acc}",${t.amount},"${t.currency}","${(t.notes || '').replace(/"/g, '""')}","${t.transactionReferenceNumber || ''}"`;
    });
    const csvContent = "data:text/csv;charset=utf-8," + [headers, ...rows].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `spendora_transactions_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      
      {/* Header & Main Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-neutral-100 font-display">
            Transactions
          </h1>
          <p className="text-xs text-neutral-400 mt-0.5">
            Detailed ledger of all expenses, income, and transfers
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={exportFilteredCsv}
            title="Export CSV"
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-neutral-900 border border-neutral-800 text-neutral-300 hover:text-white text-xs font-semibold hover:bg-neutral-800 transition"
          >
            <Download className="w-3.5 h-3.5 text-purple-400" />
            <span className="hidden sm:inline">Export CSV</span>
          </button>

          <button
            onClick={onOpenAddModal}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95"
          >
            <Plus className="w-4 h-4" />
            <span>Add Transaction</span>
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-md space-y-3">
        {/* Search row */}
        <div className="flex items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-neutral-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by merchant, note, or account..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-xl pl-9 pr-4 py-2 text-xs text-neutral-200 placeholder:text-neutral-500 focus:border-purple-500 focus:outline-none"
            />
          </div>

          <button
            onClick={() => setSortOrder(prev => prev === 'desc' ? 'asc' : 'desc')}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-neutral-950 border border-neutral-800 text-neutral-300 text-xs font-medium hover:bg-neutral-800 transition shrink-0"
            title="Sort direction"
          >
            <ArrowUpDown className="w-3.5 h-3.5 text-purple-400" />
            <span className="hidden sm:inline">{sortOrder === 'desc' ? 'Newest First' : 'Oldest First'}</span>
          </button>
        </div>

        {/* Dropdowns row */}
        <div className="flex flex-wrap items-center gap-2 pt-1 text-xs">
          {/* Type Filter */}
          <div className="flex items-center gap-1 overflow-x-auto pb-1 sm:pb-0">
            {['ALL', TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.CASH_WITHDRAWAL].map(t => (
              <button
                key={t}
                onClick={() => setSelectedType(t)}
                className={`px-3 py-1.5 rounded-lg font-semibold whitespace-nowrap transition ${
                  selectedType === t
                    ? 'bg-purple-600 text-white shadow-sm'
                    : 'bg-neutral-950 text-neutral-400 hover:text-neutral-200 border border-neutral-800'
                }`}
              >
                {t === 'ALL' ? 'All Types' : t.replace('_', ' ')}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2 ml-auto w-full sm:w-auto">
            {/* Category Dropdown */}
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="bg-neutral-950 border border-neutral-800 rounded-xl px-2.5 py-1.5 text-neutral-300 text-xs focus:border-purple-500 focus:outline-none flex-1 sm:flex-initial"
            >
              <option value="ALL">All Categories</option>
              <option value="UNCATEGORIZED">Uncategorized</option>
              {categories.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>

            {/* Cycle Dropdown */}
            <select
              value={selectedCycle}
              onChange={(e) => setSelectedCycle(e.target.value)}
              className="bg-neutral-950 border border-neutral-800 rounded-xl px-2.5 py-1.5 text-neutral-300 text-xs focus:border-purple-500 focus:outline-none flex-1 sm:flex-initial"
            >
              <option value="ALL">All Cycles</option>
              {availableCycles.map(cycle => (
                <option key={cycle} value={cycle}>{cycle}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Filtered Summary stats */}
      <div className="flex items-center justify-between text-xs px-2 text-neutral-400 font-medium">
        <span>Showing <strong>{filteredTransactions.length}</strong> transactions</span>
        <div className="flex items-center gap-4">
          <span>Spent: <strong className="text-rose-400 font-bold">₹{totalFilteredExpense.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></span>
          <span>Earned: <strong className="text-emerald-400 font-bold">₹{totalFilteredIncome.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong></span>
        </div>
      </div>

      {/* Transactions List */}
      <div className="divide-y divide-neutral-800/80 rounded-3xl bg-neutral-900/90 border border-neutral-800 overflow-hidden shadow-xl">
        {filteredTransactions.length === 0 ? (
          <div className="p-12 text-center text-neutral-500 space-y-2">
            <AlertCircle className="w-8 h-8 mx-auto text-neutral-600" />
            <p className="text-sm font-semibold">No transactions match your search filters</p>
            <p className="text-xs text-neutral-600">Try clearing filters or search terms</p>
          </div>
        ) : (
          filteredTransactions.map((txn) => {
            const cat = getCategory(txn.categoryId);
            const acc = getAccount(txn.accountId);
            const isExp = txn.transactionType === TransactionType.EXPENSE || txn.transactionType === TransactionType.CASH_WITHDRAWAL;
            const dateStr = new Date(txn.occurredTimestamp).toLocaleString('en-US', {
              month: 'short',
              day: 'numeric',
              hour: '2-digit',
              minute: '2-digit'
            });

            return (
              <div
                key={txn.id}
                id={`transaction-row-${txn.id}`}
                onClick={() => onSelectTransaction(txn)}
                className="p-4 flex items-center justify-between hover:bg-neutral-800/50 cursor-pointer transition"
              >
                <div className="flex items-center gap-3.5 min-w-0">
                  <div 
                    className="w-10 h-10 rounded-2xl flex items-center justify-center text-white shrink-0 shadow-sm"
                    style={{ backgroundColor: cat?.colorHex || '#78909C' }}
                  >
                    <span className="font-bold text-xs">
                      {(cat?.name || txn.merchant || 'Tx').substring(0, 2).toUpperCase()}
                    </span>
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-neutral-100 truncate">
                        {txn.merchant || 'Transaction'}
                      </span>
                      {txn.latitude != null && (
                        <span title="GPS Tagged">
                          <MapPin className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 text-[11px] text-neutral-400 mt-0.5 truncate">
                      <span className="font-medium text-neutral-300">{cat?.name || 'Uncategorized'}</span>
                      <span>•</span>
                      <span>{acc?.name || (txn.maskedAccountIdentifier ? `•••• ${txn.maskedAccountIdentifier}` : 'Ledger')}</span>
                      <span>•</span>
                      <span>{dateStr}</span>
                    </div>
                    {txn.notes && (
                      <p className="text-[11px] text-neutral-500 italic truncate mt-0.5 max-w-sm">
                        "{txn.notes}"
                      </p>
                    )}
                  </div>
                </div>

                <div className="text-right shrink-0 pl-3">
                  <div className={`text-sm sm:text-base font-black tracking-tight ${isExp ? 'text-rose-400' : 'text-emerald-400'}`}>
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
  );
};
