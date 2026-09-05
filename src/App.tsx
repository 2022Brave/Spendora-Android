import React, { useState, useEffect, useMemo } from 'react';
import { StorageService } from './services/storage';
import { FinancialCycleService } from './services/financialCycle';
import { SmsEngine, SAMPLE_SMS_TEMPLATES } from './services/smsEngine';
import { 
  Transaction, 
  Category, 
  Account, 
  Budget, 
  AppSettings, 
  FinancialCycleInfo, 
  ReviewStatus, 
  TransactionType, 
  TransactionSource,
  SmsParseResult
} from './types';

// Components
import { Navbar } from './components/Navbar';
import { DashboardScreen } from './components/DashboardScreen';
import { TransactionsScreen } from './components/TransactionsScreen';
import { BudgetsScreen } from './components/BudgetsScreen';
import { AnalyticsScreen } from './components/AnalyticsScreen';
import { SmsImportScreen } from './components/SmsImportScreen';
import { PendingReviewScreen } from './components/PendingReviewScreen';
import { AccountsScreen } from './components/AccountsScreen';
import { CategoriesScreen } from './components/CategoriesScreen';
import { SettingsScreen } from './components/SettingsScreen';

// Modals
import { TransactionModal } from './components/TransactionModal';
import { TransactionDetailModal } from './components/TransactionDetailModal';
import { BudgetModal } from './components/BudgetModal';
import { FinancialCycleModal } from './components/FinancialCycleModal';
import { PrivacyPolicyModal } from './components/PrivacyPolicyModal';
import { AppLockModal } from './components/AppLockModal';

export function App() {
  // Global Data State from Local-first Storage
  const [transactions, setTransactions] = useState<Transaction[]>(() => StorageService.getTransactions());
  const [categories, setCategories] = useState<Category[]>(() => StorageService.getCategories());
  const [accounts, setAccounts] = useState<Account[]>(() => StorageService.getAccounts());
  const [budgets, setBudgets] = useState<Budget[]>(() => StorageService.getBudgets());
  const [settings, setSettings] = useState<AppSettings>(() => StorageService.getSettings());

  // UI State
  const [currentTab, setCurrentTab] = useState<string>('dashboard');
  const [theme, setTheme] = useState<'dark' | 'light'>(settings.themeMode === 'LIGHT' ? 'light' : 'dark');
  const [isAppLocked, setIsAppLocked] = useState<boolean>(settings.isAppLockEnabled);

  // Modals state
  const [isTxnModalOpen, setIsTxnModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
  const [detailTransaction, setDetailTransaction] = useState<Transaction | null>(null);
  
  const [isBudgetModalOpen, setIsBudgetModalOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<Budget | null>(null);

  const [isCycleModalOpen, setIsCycleModalOpen] = useState(false);
  const [isPrivacyModalOpen, setIsPrivacyModalOpen] = useState(false);

  // Financial Cycle Calculation
  const cycleInfo: FinancialCycleInfo = useMemo(() => {
    return FinancialCycleService.getCycleInfo(settings.financialCycleStartDay);
  }, [settings.financialCycleStartDay]);

  // Pending reviews count
  const pendingTransactions = useMemo(() => {
    return transactions.filter(t => t.reviewStatus === ReviewStatus.PENDING_REVIEW);
  }, [transactions]);

  // Save changes to storage whenever entities change
  useEffect(() => {
    StorageService.saveTransactions(transactions);
  }, [transactions]);

  useEffect(() => {
    StorageService.saveCategories(categories);
  }, [categories]);

  useEffect(() => {
    StorageService.saveAccounts(accounts);
  }, [accounts]);

  useEffect(() => {
    StorageService.saveBudgets(budgets);
  }, [budgets]);

  useEffect(() => {
    StorageService.saveSettings(settings);
  }, [settings]);

  // Sync theme class
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  // ================= HANDLERS =================

  // Save Transaction (Add or Edit)
  const handleSaveTransaction = (txnData: Partial<Transaction>) => {
    if (editingTransaction) {
      // Update existing
      const updated = transactions.map(t => {
        if (t.id === editingTransaction.id) {
          return {
            ...t,
            ...txnData,
            id: t.id
          } as Transaction;
        }
        return t;
      });
      setTransactions(updated);
      setEditingTransaction(null);
    } else {
      // Create new
      const newTxn: Transaction = {
        id: 'txn_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6),
        amount: txnData.amount || 0,
        currency: txnData.currency || 'INR',
        transactionType: txnData.transactionType || TransactionType.EXPENSE,
        merchant: txnData.merchant || 'Unknown',
        categoryId: txnData.categoryId || null,
        accountId: txnData.accountId || null,
        maskedAccountIdentifier: txnData.maskedAccountIdentifier,
        occurredTimestamp: txnData.occurredTimestamp || Date.now(),
        cycleMonthYear: txnData.cycleMonthYear || cycleInfo.cycleMonthYear,
        source: txnData.source || TransactionSource.MANUAL_ENTRY,
        reviewStatus: txnData.reviewStatus || ReviewStatus.CONFIRMED,
        confidenceScore: txnData.confidenceScore ?? 1.0,
        isSalaryCredit: txnData.isSalaryCredit || false,
        notes: txnData.notes || null,
        latitude: txnData.latitude,
        longitude: txnData.longitude,
        locationLabel: txnData.locationLabel
      };

      // Also adjust Account Balance if linked
      if (newTxn.accountId) {
        setAccounts(prev => prev.map(acc => {
          if (acc.id === newTxn.accountId) {
            let bal = acc.currentBalance;
            if (newTxn.transactionType === TransactionType.EXPENSE || newTxn.transactionType === TransactionType.CASH_WITHDRAWAL) {
              bal -= newTxn.amount;
            } else if (newTxn.transactionType === TransactionType.INCOME || newTxn.transactionType === TransactionType.REFUND) {
              bal += newTxn.amount;
            }
            return { ...acc, currentBalance: Number(bal.toFixed(2)) };
          }
          return acc;
        }));
      }

      setTransactions(prev => [newTxn, ...prev]);
    }
  };

  // Delete Transaction
  const handleDeleteTransaction = (id: string) => {
    const txnToDelete = transactions.find(t => t.id === id);
    if (txnToDelete && txnToDelete.accountId) {
      // Revert account balance adjustment
      setAccounts(prev => prev.map(acc => {
        if (acc.id === txnToDelete.accountId) {
          let bal = acc.currentBalance;
          if (txnToDelete.transactionType === TransactionType.EXPENSE || txnToDelete.transactionType === TransactionType.CASH_WITHDRAWAL) {
            bal += txnToDelete.amount;
          } else if (txnToDelete.transactionType === TransactionType.INCOME) {
            bal -= txnToDelete.amount;
          }
          return { ...acc, currentBalance: Number(bal.toFixed(2)) };
        }
        return acc;
      }));
    }
    setTransactions(prev => prev.filter(t => t.id !== id));
    if (detailTransaction?.id === id) {
      setDetailTransaction(null);
    }
  };

  // Confirm Pending Review Transaction (and purge raw SMS excerpt per privacy policy!)
  const handleConfirmPending = (id: string) => {
    setTransactions(prev => prev.map(t => {
      if (t.id === id) {
        return {
          ...t,
          reviewStatus: ReviewStatus.CONFIRMED,
          rawSmsExcerpt: null // Privacy invariant: purge raw excerpt
        };
      }
      return t;
    }));
  };

  // Dismiss Pending Review Transaction
  const handleDismissPending = (id: string) => {
    setTransactions(prev => prev.map(t => {
      if (t.id === id) {
        return {
          ...t,
          reviewStatus: ReviewStatus.DISMISSED,
          rawSmsExcerpt: null
        };
      }
      return t;
    }));
  };

  // Dismiss All Pending
  const handleDismissAllPending = () => {
    setTransactions(prev => prev.map(t => {
      if (t.reviewStatus === ReviewStatus.PENDING_REVIEW) {
        return {
          ...t,
          reviewStatus: ReviewStatus.DISMISSED,
          rawSmsExcerpt: null
        };
      }
      return t;
    }));
  };

  // Commit Parsed SMS Transaction
  const handleCommitParsedSms = (result: SmsParseResult, rawText: string) => {
    if (!result.isTransactional || !result.amount) {
      return { success: false, message: 'Message is not a financial transaction.' };
    }

    // Check duplicate hash
    const isDuplicate = transactions.some(t => t.rawSmsExcerpt && t.rawSmsExcerpt.includes(result.referenceNumber || ''));
    if (isDuplicate) {
      return { success: false, message: 'Duplicate transaction detected; already present in ledger.' };
    }

    // Match account by masked number
    let matchedAcc = accounts.find(a => result.maskedAccount && a.maskedNumber === result.maskedAccount);
    if (!matchedAcc) {
      matchedAcc = accounts[0];
    }

    const isAutoConfirm = result.confidenceScore >= 0.85;
    const now = Date.now();
    const cycleMonthYear = FinancialCycleService.getCycleMonthYearForTimestamp(now, settings.financialCycleStartDay);

    const newTxn: Transaction = {
      id: 'txn_sms_' + now + '_' + Math.random().toString(36).substring(2, 6),
      amount: result.amount,
      currency: 'INR',
      transactionType: result.transactionType,
      merchant: result.merchant || 'Bank Transaction',
      categoryId: result.suggestedCategoryId || null,
      accountId: matchedAcc?.id || null,
      maskedAccountIdentifier: result.maskedAccount || matchedAcc?.maskedNumber,
      transactionReferenceNumber: result.referenceNumber || undefined,
      occurredTimestamp: now,
      cycleMonthYear,
      source: TransactionSource.SMS_PARSED,
      reviewStatus: isAutoConfirm ? ReviewStatus.CONFIRMED : ReviewStatus.PENDING_REVIEW,
      confidenceScore: result.confidenceScore,
      rawSmsExcerpt: isAutoConfirm ? null : rawText, // Purged if auto-confirmed, temporarily kept if pending review
      isSalaryCredit: result.isSalaryCredit,
      notes: result.remainingBalance ? `Balance after txn: ₹${result.remainingBalance}` : null
    };

    // Update account balance
    if (matchedAcc) {
      setAccounts(prev => prev.map(a => {
        if (a.id === matchedAcc!.id) {
          let bal = result.remainingBalance != null ? result.remainingBalance : a.currentBalance;
          if (result.remainingBalance == null) {
            if (result.transactionType === TransactionType.EXPENSE || result.transactionType === TransactionType.CASH_WITHDRAWAL) {
              bal -= result.amount!;
            } else if (result.transactionType === TransactionType.INCOME) {
              bal += result.amount!;
            }
          }
          return { ...a, currentBalance: Number(bal.toFixed(2)) };
        }
        return a;
      }));
    }

    setTransactions(prev => [newTxn, ...prev]);

    return { 
      success: true, 
      message: isAutoConfirm 
        ? 'Transaction successfully confirmed & added to ledger!' 
        : 'Transaction added to Pending Review (confidence < 85%).'
    };
  };

  // Batch Process Sample Bank Stream
  const handleBatchProcessSms = (samples: typeof SAMPLE_SMS_TEMPLATES) => {
    let count = 0;
    for (const sample of samples) {
      const parsed = SmsEngine.parse(sample.text);
      if (parsed.isTransactional && parsed.amount) {
        handleCommitParsedSms(parsed, sample.text);
        count++;
      }
    }
    return count;
  };

  // Budget Handlers
  const handleSaveBudget = (budgetData: Partial<Budget>) => {
    if (editingBudget) {
      setBudgets(prev => prev.map(b => b.id === editingBudget.id ? { ...b, ...budgetData } as Budget : b));
      setEditingBudget(null);
    } else {
      const newBudget: Budget = {
        id: 'budget_' + Date.now(),
        categoryId: budgetData.categoryId || null,
        amountLimit: budgetData.amountLimit || 10000,
        period: budgetData.period || 'FINANCIAL_CYCLE' as any,
        cycleMonthYear: budgetData.cycleMonthYear || cycleInfo.cycleMonthYear,
        thresholdPercent: budgetData.thresholdPercent || 80,
        isActive: true
      };
      setBudgets(prev => [...prev, newBudget]);
    }
  };

  const handleDeleteBudget = (id: string) => {
    setBudgets(prev => prev.filter(b => b.id !== id));
  };

  // Accounts Handlers
  const handleSaveAccount = (accountData: Partial<Account>) => {
    if (accountData.id) {
      setAccounts(prev => prev.map(a => a.id === accountData.id ? { ...a, ...accountData } as Account : a));
    } else {
      const newAcc: Account = {
        id: 'acc_' + Date.now(),
        name: accountData.name || 'New Account',
        institutionName: accountData.institutionName,
        type: accountData.type || 'SAVINGS_BANK' as any,
        maskedNumber: accountData.maskedNumber,
        currentBalance: accountData.currentBalance || 0,
        currency: 'INR',
        isArchived: false,
        lastReconciledTimestamp: Date.now()
      };
      setAccounts(prev => [...prev, newAcc]);
    }
  };

  const handleToggleArchiveAccount = (id: string) => {
    setAccounts(prev => prev.map(a => a.id === id ? { ...a, isArchived: !a.isArchived } : a));
  };

  // Category Handlers
  const handleAddCategory = (name: string, type: 'EXPENSE' | 'INCOME', colorHex: string) => {
    const newCat: Category = {
      id: 'cat_custom_' + Date.now(),
      name,
      iconName: 'Tag',
      colorHex,
      type,
      isDefault: false,
      isArchived: false
    };
    setCategories(prev => [...prev, newCat]);
  };

  const handleArchiveCategory = (id: string) => {
    setCategories(prev => prev.map(c => c.id === id ? { ...c, isArchived: true } : c));
  };

  // Settings & Financial Cycle
  const handleSaveCycleDay = (newDay: number) => {
    const updatedSettings = { ...settings, financialCycleStartDay: newDay };
    setSettings(updatedSettings);

    // Recalculate transaction cycleMonthYear values safely
    setTransactions(prev => prev.map(t => ({
      ...t,
      cycleMonthYear: FinancialCycleService.getCycleMonthYearForTimestamp(t.occurredTimestamp, newDay)
    })));
  };

  const handleUpdateSettings = (partial: Partial<AppSettings>) => {
    setSettings(prev => ({ ...prev, ...partial }));
    if (partial.themeMode) {
      setTheme(partial.themeMode === 'LIGHT' ? 'light' : 'dark');
    }
  };

  const handleToggleTheme = () => {
    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(nextTheme);
    handleUpdateSettings({ themeMode: nextTheme === 'light' ? 'LIGHT' : 'DARK' });
  };

  // Data Backup Export & Import
  const handleExportBackup = () => {
    const backupJson = StorageService.exportBackup();
    const blob = new Blob([backupJson], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `spendora_backup_${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportBackup = (jsonContent: string) => {
    const ok = StorageService.importBackup(jsonContent);
    if (ok) {
      setTransactions(StorageService.getTransactions());
      setCategories(StorageService.getCategories());
      setAccounts(StorageService.getAccounts());
      setBudgets(StorageService.getBudgets());
      setSettings(StorageService.getSettings());
    }
    return ok;
  };

  const handleResetData = () => {
    StorageService.resetToDefaults();
    setTransactions(StorageService.getTransactions());
    setCategories(StorageService.getCategories());
    setAccounts(StorageService.getAccounts());
    setBudgets(StorageService.getBudgets());
    setSettings(StorageService.getSettings());
  };

  // Active Category & Account lookup helpers for detail modal
  const detailCategory = categories.find(c => c.id === detailTransaction?.categoryId);
  const detailAccount = accounts.find(a => a.id === detailTransaction?.accountId);

  return (
    <div className={`min-h-screen ${theme === 'dark' ? 'bg-neutral-950 text-neutral-100' : 'bg-neutral-100 text-neutral-900'} flex flex-col font-sans transition-colors selection:bg-purple-600 selection:text-white`}>
      
      {/* Biometric App Lock Modal */}
      <AppLockModal 
        isLocked={isAppLocked}
        onUnlock={() => setIsAppLocked(false)}
      />

      {/* Main Top Navigation */}
      <Navbar 
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        pendingCount={pendingTransactions.length}
        onOpenAddModal={() => {
          setEditingTransaction(null);
          setIsTxnModalOpen(true);
        }}
        theme={theme}
        onToggleTheme={handleToggleTheme}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 pt-6">
        {currentTab === 'dashboard' && (
          <DashboardScreen
            cycleInfo={cycleInfo}
            transactions={transactions}
            accounts={accounts}
            categories={categories}
            budgets={budgets}
            pendingCount={pendingTransactions.length}
            onNavigate={setCurrentTab}
            onOpenAddModal={() => {
              setEditingTransaction(null);
              setIsTxnModalOpen(true);
            }}
            onSelectTransaction={(txn) => setDetailTransaction(txn)}
          />
        )}

        {currentTab === 'transactions' && (
          <TransactionsScreen
            transactions={transactions}
            categories={categories}
            accounts={accounts}
            onSelectTransaction={(txn) => setDetailTransaction(txn)}
            onOpenAddModal={() => {
              setEditingTransaction(null);
              setIsTxnModalOpen(true);
            }}
          />
        )}

        {currentTab === 'budgets' && (
          <BudgetsScreen
            budgets={budgets}
            categories={categories}
            transactions={transactions}
            cycleInfo={cycleInfo}
            onOpenAddBudget={(budget) => {
              setEditingBudget(budget || null);
              setIsBudgetModalOpen(true);
            }}
            onDeleteBudget={handleDeleteBudget}
          />
        )}

        {currentTab === 'analytics' && (
          <AnalyticsScreen
            transactions={transactions}
            categories={categories}
            cycleInfo={cycleInfo}
          />
        )}

        {currentTab === 'sms' && (
          <SmsImportScreen
            categories={categories}
            accounts={accounts}
            onCommitParsedTransaction={handleCommitParsedSms}
            onBatchProcess={handleBatchProcessSms}
          />
        )}

        {currentTab === 'pending' && (
          <PendingReviewScreen
            pendingTransactions={pendingTransactions}
            categories={categories}
            accounts={accounts}
            onConfirm={handleConfirmPending}
            onDismiss={handleDismissPending}
            onEdit={(txn) => {
              setEditingTransaction(txn);
              setIsTxnModalOpen(true);
            }}
            onClearAll={handleDismissAllPending}
          />
        )}

        {currentTab === 'accounts' && (
          <AccountsScreen
            accounts={accounts}
            onSaveAccount={handleSaveAccount}
            onToggleArchive={handleToggleArchiveAccount}
          />
        )}

        {currentTab === 'categories' && (
          <CategoriesScreen
            categories={categories}
            onAddCategory={handleAddCategory}
            onArchiveCategory={handleArchiveCategory}
          />
        )}

        {currentTab === 'settings' && (
          <SettingsScreen
            settings={settings}
            cycleInfo={cycleInfo}
            onUpdateSettings={handleUpdateSettings}
            onOpenCycleModal={() => setIsCycleModalOpen(true)}
            onOpenPrivacyModal={() => setIsPrivacyModalOpen(true)}
            onNavigate={setCurrentTab}
            onExportBackup={handleExportBackup}
            onImportBackup={handleImportBackup}
            onResetData={handleResetData}
          />
        )}
      </main>

      {/* Transaction Add/Edit Modal */}
      <TransactionModal
        isOpen={isTxnModalOpen}
        onClose={() => {
          setIsTxnModalOpen(false);
          setEditingTransaction(null);
        }}
        onSave={handleSaveTransaction}
        categories={categories}
        accounts={accounts}
        cycleStartDay={settings.financialCycleStartDay}
        editingTransaction={editingTransaction}
      />

      {/* Transaction Detail & Delete Modal */}
      <TransactionDetailModal
        transaction={detailTransaction}
        onClose={() => setDetailTransaction(null)}
        onEdit={(txn) => {
          setDetailTransaction(null);
          setEditingTransaction(txn);
          setIsTxnModalOpen(true);
        }}
        onDelete={handleDeleteTransaction}
        category={detailCategory}
        account={detailAccount}
      />

      {/* Budget Modal */}
      <BudgetModal
        isOpen={isBudgetModalOpen}
        onClose={() => {
          setIsBudgetModalOpen(false);
          setEditingBudget(null);
        }}
        onSave={handleSaveBudget}
        categories={categories}
        cycleMonthYear={cycleInfo.cycleMonthYear}
        editingBudget={editingBudget}
      />

      {/* Financial Cycle Start Day Modal */}
      <FinancialCycleModal
        isOpen={isCycleModalOpen}
        onClose={() => setIsCycleModalOpen(false)}
        currentDay={settings.financialCycleStartDay}
        onSaveDay={handleSaveCycleDay}
      />

      {/* Privacy Policy Guarantee Modal */}
      <PrivacyPolicyModal
        isOpen={isPrivacyModalOpen}
        onClose={() => setIsPrivacyModalOpen(false)}
      />
    </div>
  );
}

export default App;
