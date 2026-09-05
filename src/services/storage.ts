import { 
  Transaction, 
  Account, 
  Category, 
  Budget, 
  AppSettings, 
  TransactionType, 
  TransactionSource, 
  ReviewStatus, 
  AccountType, 
  CategoryType, 
  BudgetPeriod 
} from '../types';
import { FinancialCycleService } from './financialCycle';

const STORAGE_KEYS = {
  TRANSACTIONS: 'spendora_transactions',
  ACCOUNTS: 'spendora_accounts',
  CATEGORIES: 'spendora_categories',
  BUDGETS: 'spendora_budgets',
  SETTINGS: 'spendora_settings'
};

export const DEFAULT_CATEGORIES: Category[] = [
  // Expenses (21)
  { id: 'cat_food', name: 'Food', type: CategoryType.EXPENSE, icon: 'Utensils', iconName: 'Utensils', colorHex: '#FF5722', isSystem: true, isDefault: true, isArchived: false, sortOrder: 1 },
  { id: 'cat_groceries', name: 'Groceries', type: CategoryType.EXPENSE, icon: 'ShoppingCart', iconName: 'ShoppingCart', colorHex: '#4CAF50', isSystem: true, isDefault: true, isArchived: false, sortOrder: 2 },
  { id: 'cat_dining', name: 'Dining', type: CategoryType.EXPENSE, icon: 'Coffee', iconName: 'Coffee', colorHex: '#FF9800', isSystem: true, isDefault: true, isArchived: false, sortOrder: 3 },
  { id: 'cat_transport', name: 'Transport', type: CategoryType.EXPENSE, icon: 'Bus', iconName: 'Bus', colorHex: '#00BCD4', isSystem: true, isDefault: true, isArchived: false, sortOrder: 4 },
  { id: 'cat_fuel', name: 'Fuel', type: CategoryType.EXPENSE, icon: 'Fuel', iconName: 'Fuel', colorHex: '#E91E63', isSystem: true, isDefault: true, isArchived: false, sortOrder: 5 },
  { id: 'cat_shopping', name: 'Shopping', type: CategoryType.EXPENSE, icon: 'ShoppingBag', iconName: 'ShoppingBag', colorHex: '#9C27B0', isSystem: true, isDefault: true, isArchived: false, sortOrder: 6 },
  { id: 'cat_bills', name: 'Bills', type: CategoryType.EXPENSE, icon: 'Receipt', iconName: 'Receipt', colorHex: '#607D8B', isSystem: true, isDefault: true, isArchived: false, sortOrder: 7 },
  { id: 'cat_rent', name: 'Rent', type: CategoryType.EXPENSE, icon: 'Home', iconName: 'Home', colorHex: '#795548', isSystem: true, isDefault: true, isArchived: false, sortOrder: 8 },
  { id: 'cat_utilities', name: 'Utilities', type: CategoryType.EXPENSE, icon: 'Zap', iconName: 'Zap', colorHex: '#FFC107', isSystem: true, isDefault: true, isArchived: false, sortOrder: 9 },
  { id: 'cat_entertainment', name: 'Entertainment', type: CategoryType.EXPENSE, icon: 'Film', iconName: 'Film', colorHex: '#673AB7', isSystem: true, isDefault: true, isArchived: false, sortOrder: 10 },
  { id: 'cat_health', name: 'Health', type: CategoryType.EXPENSE, icon: 'Activity', iconName: 'Activity', colorHex: '#009688', isSystem: true, isDefault: true, isArchived: false, sortOrder: 11 },
  { id: 'cat_medicine', name: 'Medicine', type: CategoryType.EXPENSE, icon: 'Pill', iconName: 'Pill', colorHex: '#F44336', isSystem: true, isDefault: true, isArchived: false, sortOrder: 12 },
  { id: 'cat_education', name: 'Education', type: CategoryType.EXPENSE, icon: 'GraduationCap', iconName: 'GraduationCap', colorHex: '#3F51B5', isSystem: true, isDefault: true, isArchived: false, sortOrder: 13 },
  { id: 'cat_travel', name: 'Travel', type: CategoryType.EXPENSE, icon: 'Plane', iconName: 'Plane', colorHex: '#2196F3', isSystem: true, isDefault: true, isArchived: false, sortOrder: 14 },
  { id: 'cat_subscriptions', name: 'Subscriptions', type: CategoryType.EXPENSE, icon: 'Award', iconName: 'Award', colorHex: '#8E24AA', isSystem: true, isDefault: true, isArchived: false, sortOrder: 15 },
  { id: 'cat_personal_care', name: 'Personal Care', type: CategoryType.EXPENSE, icon: 'Sparkles', iconName: 'Sparkles', colorHex: '#D81B60', isSystem: true, isDefault: true, isArchived: false, sortOrder: 16 },
  { id: 'cat_insurance', name: 'Insurance', type: CategoryType.EXPENSE, icon: 'Shield', iconName: 'Shield', colorHex: '#455A64', isSystem: true, isDefault: true, isArchived: false, sortOrder: 17 },
  { id: 'cat_emi', name: 'EMI', type: CategoryType.EXPENSE, icon: 'Landmark', iconName: 'Landmark', colorHex: '#C2185B', isSystem: true, isDefault: true, isArchived: false, sortOrder: 18 },
  { id: 'cat_investment', name: 'Investment', type: CategoryType.EXPENSE, icon: 'TrendingUp', iconName: 'TrendingUp', colorHex: '#2E7D32', isSystem: true, isDefault: true, isArchived: false, sortOrder: 19 },
  { id: 'cat_cash_withdrawal', name: 'Cash Withdrawal', type: CategoryType.EXPENSE, icon: 'Banknote', iconName: 'Banknote', colorHex: '#546E7A', isSystem: true, isDefault: true, isArchived: false, sortOrder: 20 },
  { id: 'cat_other', name: 'Other', type: CategoryType.EXPENSE, icon: 'MoreHorizontal', iconName: 'MoreHorizontal', colorHex: '#9E9E9E', isSystem: true, isDefault: true, isArchived: false, sortOrder: 21 },

  // Income (6)
  { id: 'cat_salary', name: 'Salary', type: CategoryType.INCOME, icon: 'DollarSign', iconName: 'DollarSign', colorHex: '#43A047', isSystem: true, isDefault: true, isArchived: false, sortOrder: 22 },
  { id: 'cat_freelance', name: 'Freelance', type: CategoryType.INCOME, icon: 'Laptop', iconName: 'Laptop', colorHex: '#1E88E5', isSystem: true, isDefault: true, isArchived: false, sortOrder: 23 },
  { id: 'cat_business', name: 'Business', type: CategoryType.INCOME, icon: 'Store', iconName: 'Store', colorHex: '#FB8C00', isSystem: true, isDefault: true, isArchived: false, sortOrder: 24 },
  { id: 'cat_interest', name: 'Interest', type: CategoryType.INCOME, icon: 'PiggyBank', iconName: 'PiggyBank', colorHex: '#00897B', isSystem: true, isDefault: true, isArchived: false, sortOrder: 25 },
  { id: 'cat_refund', name: 'Refund', type: CategoryType.INCOME, icon: 'RotateCcw', iconName: 'RotateCcw', colorHex: '#8E24AA', isSystem: true, isDefault: true, isArchived: false, sortOrder: 26 },
  { id: 'cat_other_income', name: 'Other Income', type: CategoryType.INCOME, icon: 'Coins', iconName: 'Coins', colorHex: '#7CB342', isSystem: true, isDefault: true, isArchived: false, sortOrder: 27 }
];

export const DEFAULT_ACCOUNTS: Account[] = [
  {
    id: 'acc_hdfc',
    name: 'HDFC Salary A/c',
    type: AccountType.BANK_ACCOUNT,
    institutionName: 'HDFC Bank',
    maskedNumber: '4821',
    initialBalance: 75000,
    currentBalance: 88500,
    currency: 'INR',
    isDefault: true,
    isArchived: false
  },
  {
    id: 'acc_icici',
    name: 'ICICI Coral Card',
    type: AccountType.CREDIT_CARD,
    institutionName: 'ICICI Bank',
    maskedNumber: '9034',
    initialBalance: 0,
    currentBalance: 12450,
    currency: 'INR',
    isDefault: false,
    isArchived: false
  },
  {
    id: 'acc_paytm',
    name: 'Paytm Wallet',
    type: AccountType.WALLET,
    institutionName: 'Paytm Payments Bank',
    maskedNumber: '7102',
    initialBalance: 1500,
    currentBalance: 3200,
    currency: 'INR',
    isDefault: false,
    isArchived: false
  },
  {
    id: 'acc_cash',
    name: 'Cash in Hand',
    type: AccountType.CASH,
    initialBalance: 5000,
    currentBalance: 4200,
    currency: 'INR',
    isDefault: false,
    isArchived: false
  }
];

export class StorageService {
  static getSettings(): AppSettings {
    const raw = localStorage.getItem(STORAGE_KEYS.SETTINGS);
    if (!raw) {
      const def: AppSettings = {
        cycleStartDay: 1,
        financialCycleStartDay: 1,
        isAppLockEnabled: false,
        theme: 'dark',
        themeMode: 'DARK',
        currencySymbol: '₹'
      };
      localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(def));
      return def;
    }
    const parsed = JSON.parse(raw);
    const day = parsed.financialCycleStartDay || parsed.cycleStartDay || 1;
    return {
      cycleStartDay: day,
      financialCycleStartDay: day,
      isAppLockEnabled: Boolean(parsed.isAppLockEnabled),
      theme: parsed.theme || (parsed.themeMode === 'LIGHT' ? 'light' : 'dark'),
      themeMode: parsed.themeMode || (parsed.theme === 'light' ? 'LIGHT' : 'DARK'),
      currencySymbol: parsed.currencySymbol || '₹'
    };
  }

  static saveSettings(settings: AppSettings): void {
    const day = settings.financialCycleStartDay || settings.cycleStartDay || 1;
    const toSave: AppSettings = {
      ...settings,
      cycleStartDay: day,
      financialCycleStartDay: day,
      themeMode: settings.theme === 'light' ? 'LIGHT' : 'DARK'
    };
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(toSave));
  }

  static getCategories(): Category[] {
    const raw = localStorage.getItem(STORAGE_KEYS.CATEGORIES);
    if (!raw) {
      localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(DEFAULT_CATEGORIES));
      return DEFAULT_CATEGORIES;
    }
    return JSON.parse(raw);
  }

  static saveCategories(categories: Category[]): void {
    localStorage.setItem(STORAGE_KEYS.CATEGORIES, JSON.stringify(categories));
  }

  static getAccounts(): Account[] {
    const raw = localStorage.getItem(STORAGE_KEYS.ACCOUNTS);
    if (!raw) {
      localStorage.setItem(STORAGE_KEYS.ACCOUNTS, JSON.stringify(DEFAULT_ACCOUNTS));
      return DEFAULT_ACCOUNTS;
    }
    return JSON.parse(raw);
  }

  static saveAccounts(accounts: Account[]): void {
    localStorage.setItem(STORAGE_KEYS.ACCOUNTS, JSON.stringify(accounts));
  }

  static getBudgets(): Budget[] {
    const raw = localStorage.getItem(STORAGE_KEYS.BUDGETS);
    if (!raw) {
      const currentCycle = FinancialCycleService.getCycleInfo(this.getSettings().financialCycleStartDay);
      const defBudgets: Budget[] = [
        {
          id: 'b_overall',
          categoryId: null, // Overall
          amountLimit: 50000,
          period: BudgetPeriod.FINANCIAL_CYCLE,
          cycleMonthYear: currentCycle.cycleMonthYear,
          thresholdPercent: 80,
          isActive: true
        },
        {
          id: 'b_food',
          categoryId: 'cat_dining',
          amountLimit: 8000,
          period: BudgetPeriod.FINANCIAL_CYCLE,
          cycleMonthYear: currentCycle.cycleMonthYear,
          thresholdPercent: 80,
          isActive: true
        },
        {
          id: 'b_groceries',
          categoryId: 'cat_groceries',
          amountLimit: 12000,
          period: BudgetPeriod.FINANCIAL_CYCLE,
          cycleMonthYear: currentCycle.cycleMonthYear,
          thresholdPercent: 80,
          isActive: true
        },
        {
          id: 'b_shopping',
          categoryId: 'cat_shopping',
          amountLimit: 10000,
          period: BudgetPeriod.FINANCIAL_CYCLE,
          cycleMonthYear: currentCycle.cycleMonthYear,
          thresholdPercent: 80,
          isActive: true
        }
      ];
      localStorage.setItem(STORAGE_KEYS.BUDGETS, JSON.stringify(defBudgets));
      return defBudgets;
    }
    return JSON.parse(raw);
  }

  static saveBudgets(budgets: Budget[]): void {
    localStorage.setItem(STORAGE_KEYS.BUDGETS, JSON.stringify(budgets));
  }

  static getTransactions(): Transaction[] {
    const raw = localStorage.getItem(STORAGE_KEYS.TRANSACTIONS);
    if (!raw) {
      const initialTxns = this.generateSampleTransactions();
      localStorage.setItem(STORAGE_KEYS.TRANSACTIONS, JSON.stringify(initialTxns));
      return initialTxns;
    }
    return JSON.parse(raw);
  }

  static saveTransactions(transactions: Transaction[]): void {
    localStorage.setItem(STORAGE_KEYS.TRANSACTIONS, JSON.stringify(transactions));
  }

  private static generateSampleTransactions(): Transaction[] {
    const now = Date.now();
    const day = 24 * 60 * 60 * 1000;
    const cycle = FinancialCycleService.getCycleInfo(1);

    return [
      {
        id: 'txn_1',
        amount: 85000,
        currency: 'INR',
        transactionType: TransactionType.INCOME,
        merchant: 'Google Inc / Salary',
        categoryId: 'cat_salary',
        accountId: 'acc_hdfc',
        maskedAccountIdentifier: '4821',
        occurredTimestamp: now - 12 * day,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.SMS_REALTIME,
        reviewStatus: ReviewStatus.CONFIRMED,
        confidenceScore: 0.98,
        isSalaryCredit: true,
        notes: 'Monthly corporate salary credit',
        transactionReferenceNumber: 'SAL-2026-90412'
      },
      {
        id: 'txn_2',
        amount: 640,
        currency: 'INR',
        transactionType: TransactionType.EXPENSE,
        merchant: 'Swiggy',
        categoryId: 'cat_dining',
        accountId: 'acc_hdfc',
        maskedAccountIdentifier: '4821',
        occurredTimestamp: now - 1 * day,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.SMS_REALTIME,
        reviewStatus: ReviewStatus.CONFIRMED,
        confidenceScore: 0.95,
        isSalaryCredit: false,
        notes: 'Lunch delivery',
        transactionReferenceNumber: 'UPI-624891028491'
      },
      {
        id: 'txn_3',
        amount: 2499,
        currency: 'INR',
        transactionType: TransactionType.EXPENSE,
        merchant: 'Zomato',
        categoryId: 'cat_dining',
        accountId: 'acc_icici',
        maskedAccountIdentifier: '9034',
        occurredTimestamp: now - 2 * day,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.SMS_REALTIME,
        reviewStatus: ReviewStatus.CONFIRMED,
        confidenceScore: 0.92,
        isSalaryCredit: false,
        notes: 'Weekend dinner'
      },
      {
        id: 'txn_4',
        amount: 412,
        currency: 'INR',
        transactionType: TransactionType.EXPENSE,
        merchant: 'Blinkit',
        categoryId: 'cat_groceries',
        accountId: 'acc_paytm',
        maskedAccountIdentifier: '7102',
        occurredTimestamp: now - 3 * day,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.SMS_REALTIME,
        reviewStatus: ReviewStatus.CONFIRMED,
        confidenceScore: 0.94,
        isSalaryCredit: false,
        notes: 'Daily groceries'
      },
      {
        id: 'txn_5',
        amount: 15000,
        currency: 'INR',
        transactionType: TransactionType.EXPENSE,
        merchant: 'Rent Payment',
        categoryId: 'cat_rent',
        accountId: 'acc_hdfc',
        maskedAccountIdentifier: '4821',
        occurredTimestamp: now - 4 * day,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.MANUAL_ENTRY,
        reviewStatus: ReviewStatus.CONFIRMED,
        confidenceScore: 1.0,
        isSalaryCredit: false,
        notes: 'Monthly apartment rent transfer',
        transactionReferenceNumber: 'NEFT-AXIS-99214'
      },
      {
        id: 'txn_6',
        amount: 2000,
        currency: 'INR',
        transactionType: TransactionType.CASH_WITHDRAWAL,
        merchant: 'ATM Cash Withdrawal',
        categoryId: 'cat_cash_withdrawal',
        accountId: 'acc_hdfc',
        maskedAccountIdentifier: '4821',
        occurredTimestamp: now - 8 * day,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.SMS_REALTIME,
        reviewStatus: ReviewStatus.CONFIRMED,
        confidenceScore: 0.90,
        isSalaryCredit: false,
        notes: 'HDFC ATM Cash dispensing'
      },
      {
        id: 'txn_pending_1',
        amount: 1250,
        currency: 'INR',
        transactionType: TransactionType.EXPENSE,
        merchant: 'UPI / VPA PY-MERCHANT-842',
        categoryId: null,
        accountId: 'acc_hdfc',
        maskedAccountIdentifier: '4821',
        occurredTimestamp: now - 4 * 3600 * 1000,
        cycleMonthYear: cycle.cycleMonthYear,
        source: TransactionSource.SMS_REALTIME,
        rawSmsExcerpt: 'Rs. 1,250.00 debited from A/c XX4821 via UPI to PY-MERCHANT-842 on 04-Sep-26. Avail Bal: Rs 88,500.',
        reviewStatus: ReviewStatus.PENDING_REVIEW,
        confidenceScore: 0.62,
        isSalaryCredit: false,
        notes: 'Ambiguous payee detected in SMS'
      }
    ];
  }

  static exportBackup(): string {
    const backupData = {
      version: '1.0.0',
      exportedAt: new Date().toISOString(),
      generator: 'SPENDORA_OFFLINE_BACKUP',
      settings: this.getSettings(),
      accounts: this.getAccounts(),
      categories: this.getCategories(),
      budgets: this.getBudgets(),
      transactions: this.getTransactions()
    };
    return JSON.stringify(backupData, null, 2);
  }

  static importBackup(jsonStr: string): boolean {
    const res = this.restoreBackup(jsonStr);
    return res.success;
  }

  static restoreBackup(jsonStr: string): { success: boolean; error?: string } {
    try {
      const data = JSON.parse(jsonStr);
      if (!data.accounts || !data.categories || !data.transactions) {
        return { success: false, error: 'Invalid backup format: missing core collections.' };
      }
      if (Array.isArray(data.accounts)) this.saveAccounts(data.accounts);
      if (Array.isArray(data.categories)) this.saveCategories(data.categories);
      if (Array.isArray(data.budgets)) this.saveBudgets(data.budgets);
      if (Array.isArray(data.transactions)) this.saveTransactions(data.transactions);
      if (data.settings) this.saveSettings(data.settings);
      return { success: true };
    } catch (e) {
      return { success: false, error: (e as Error).message || 'Corrupted JSON file' };
    }
  }

  static resetToDefaults(): void {
    localStorage.removeItem(STORAGE_KEYS.TRANSACTIONS);
    localStorage.removeItem(STORAGE_KEYS.ACCOUNTS);
    localStorage.removeItem(STORAGE_KEYS.CATEGORIES);
    localStorage.removeItem(STORAGE_KEYS.BUDGETS);
    localStorage.removeItem(STORAGE_KEYS.SETTINGS);
    this.getSettings();
    this.getCategories();
    this.getAccounts();
    this.getBudgets();
    this.getTransactions();
  }
}
