export enum TransactionType {
  EXPENSE = 'EXPENSE',
  INCOME = 'INCOME',
  TRANSFER = 'TRANSFER',
  CASH_WITHDRAWAL = 'CASH_WITHDRAWAL',
  REFUND = 'REFUND'
}

export enum TransactionSource {
  SMS_REALTIME = 'SMS_REALTIME',
  SMS_HISTORICAL = 'SMS_HISTORICAL',
  SMS_PARSED = 'SMS_PARSED',
  MANUAL_ENTRY = 'MANUAL_ENTRY',
  CSV_IMPORT = 'CSV_IMPORT'
}

export enum ReviewStatus {
  CONFIRMED = 'CONFIRMED',
  PENDING_REVIEW = 'PENDING_REVIEW',
  DISMISSED = 'DISMISSED'
}

export enum AccountType {
  BANK_ACCOUNT = 'BANK_ACCOUNT',
  SAVINGS_BANK = 'SAVINGS_BANK',
  CURRENT_BANK = 'CURRENT_BANK',
  CREDIT_CARD = 'CREDIT_CARD',
  WALLET = 'WALLET',
  DIGITAL_WALLET = 'DIGITAL_WALLET',
  CASH = 'CASH',
  CASH_WALLET = 'CASH_WALLET',
  INVESTMENT = 'INVESTMENT'
}

export enum CategoryType {
  EXPENSE = 'EXPENSE',
  INCOME = 'INCOME'
}

export enum BudgetPeriod {
  FINANCIAL_CYCLE = 'FINANCIAL_CYCLE',
  MONTHLY = 'MONTHLY',
  WEEKLY = 'WEEKLY'
}

export interface Transaction {
  id: string;
  amount: number;
  currency: string;
  transactionType: TransactionType;
  merchant: string;
  categoryId: string | null;
  accountId: string | null;
  maskedAccountIdentifier?: string;
  occurredTimestamp: number;
  cycleMonthYear: string; // e.g. "2026-09"
  source: TransactionSource;
  rawSmsExcerpt?: string | null;
  reviewStatus: ReviewStatus;
  confidenceScore: number;
  isSalaryCredit: boolean;
  dedupHash?: string;
  notes?: string | null;
  latitude?: number | null;
  longitude?: number | null;
  locationLabel?: string | null;
  transactionReferenceNumber?: string | null;
}

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  institutionName?: string;
  maskedNumber?: string;
  initialBalance?: number;
  currentBalance: number;
  currency?: string;
  isDefault?: boolean;
  isArchived: boolean;
  lastReconciledTimestamp?: number;
}

export interface Category {
  id: string;
  name: string;
  type: CategoryType | 'EXPENSE' | 'INCOME';
  icon?: string;
  iconName?: string;
  colorHex: string;
  isSystem?: boolean;
  isDefault?: boolean;
  isArchived: boolean;
  sortOrder?: number;
}

export interface Budget {
  id: string;
  categoryId: string | null; // null means overall cycle budget
  amountLimit: number;
  period: BudgetPeriod;
  cycleMonthYear: string;
  thresholdPercent: number; // e.g. 80
  isActive: boolean;
}

export interface FinancialCycleInfo {
  cycleStartDay: number; // 1 to 31
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  displayLabel: string; // e.g. "Sep 2026"
  cycleMonthYear: string; // e.g. "2026-09"
  daysRemaining: number;
  totalDays: number;
  progressPercent: number;
}

export interface RawSmsInput {
  sender: string;
  body: string;
  timestamp: number;
}

export interface EligibilityResult {
  isEligible: boolean;
  rejectionReason?: string | null;
  tags: string[];
}

export interface ParsedAmounts {
  transactionAmount?: number | null;
  balanceAmount?: number | null;
  currency: string;
}

export interface SmsParseResult {
  isEligible: boolean;
  isTransactional: boolean;
  rejectionReason?: string | null;
  amount?: number | null;
  remainingBalance?: number | null;
  balance?: number | null;
  transactionType: TransactionType;
  type?: TransactionType | null;
  merchant?: string | null;
  maskedAccount?: string | null;
  accountDigits?: string | null;
  referenceNumber?: string | null;
  confidenceScore: number;
  suggestedCategoryId?: string | null;
  isSalaryCredit: boolean;
  warningTags: string[];
  dedupHash: string;
}

export type ParsedSmsResult = SmsParseResult;

export interface AppSettings {
  cycleStartDay: number;
  financialCycleStartDay: number;
  isAppLockEnabled: boolean;
  theme: 'dark' | 'light';
  themeMode?: 'DARK' | 'LIGHT';
  currencySymbol: string;
}
