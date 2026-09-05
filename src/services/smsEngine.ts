import { 
  TransactionType, 
  RawSmsInput, 
  EligibilityResult, 
  ParsedAmounts, 
  SmsParseResult 
} from '../types';

export const SAMPLE_SMS_TEMPLATES = [
  {
    name: 'HDFC UPI Debit',
    bank: 'HDFC Bank',
    text: 'Sent Rs. 640.00 from HDFC Bank A/C **4821 to Swiggy on 04-Sep-26 via UPI. UPI Ref 624891028491. Avail Bal: Rs 88,500.00.'
  },
  {
    name: 'ICICI Credit Card',
    bank: 'ICICI Bank',
    text: 'Your ICICI Bank Coral Credit Card XX9034 was spent for INR 2,499.00 at Zomato on 03-Sep-26. Avail Limit: INR 1,87,501.00.'
  },
  {
    name: 'SBI Salary Credit',
    bank: 'State Bank of India',
    text: 'Dear Customer, your A/C 4821 is credited by Rs 85,000.00 on 01-Sep-26 by Salary transfer from Google Inc. Total Bal: Rs 92,400.00.'
  },
  {
    name: 'Axis Bank Transfer',
    bank: 'Axis Bank',
    text: 'INR 15,000.00 debited from Axis A/c no. XX4821 on 02-Sep-26 towards Rent payment. Info: NEFT-AXIS-99214. Avail bal: INR 35,400.'
  },
  {
    name: 'ATM Cash Withdrawal',
    bank: 'HDFC Bank',
    text: 'Cash withdrawal of Rs 2,000.00 from HDFC Bank ATM was successful for A/c **4821 on 02-Sep-26. Clear Bal is Rs 50,400.00.'
  },
  {
    name: 'Blinkit Quick Commerce',
    bank: 'Paytm Payments Bank',
    text: 'Paid Rs 412.00 to Blinkit using Paytm Wallet 7102 on 04-Sep-26. Txn ID: PT8829103. Wallet Balance: Rs 3,200.00.'
  },
  {
    name: 'Promotional Loan Spam',
    bank: 'Finance Spam (Rejected)',
    text: 'Congratulations! You are pre-approved for an instant personal loan of Rs. 5,00,000 at zero interest EMI. Apply now at https://bit.ly/spam'
  },
  {
    name: 'Bank OTP Code',
    bank: 'Security Code (Rejected)',
    text: '482910 is your OTP for transaction of Rs 1,450.00 at Amazon on ICICI Card. Valid for 10 mins. Never share your OTP with anyone.'
  }
];

export class SmsEngine {
  private static readonly OTP_PATTERNS = [
    /\b(otp|one\s*time\s*password|verification\s*code|security\s*code|login\s*code|secret\s*code)\b/i,
    /\b(is\s*your\s*otp|use\s*\d{4,8}\s*to\s*verify|valid\s*for\s*\d+\s*(?:mins?|minutes?)|do\s*not\s*share\s*(?:this)?\s*otp)\b/i,
    /\b(never\s*share\s*your\s*otp|authentication\s*code|cvv|mpin\b)/i,
    /\b(enter\s*code\s*\d{4,8}\b|authorization\s*code\b)/i
  ];

  private static readonly PROMO_PATTERNS = [
    /\b(pre-?approved\s*(?:personal)?\s*loan|instant\s*loan|apply\s*for\s*loan|credit\s*card\s*limit\s*increase)\b/i,
    /\b(congratulations!?\s*(?:you\s*are\s*eligible)?|special\s*offer|limited\s*period\s*offer|exclusive\s*deal)\b/i,
    /\b(apply\s*now|click\s*here\s*to\s*(?:avail|apply|claim)|call\s*now\s*to\s*avail|claim\s*now)\b/i,
    /\b(flat\s*\d+%\s*off|discount\s*on|shop\s*now\s*and\s*get|save\s*up\s*to)\b/i,
    /\b(avail\s*(?:personal)?\s*loan|pre-?qualified|insurance\s*cover\s*of|life\s*insurance\s*plan)\b/i,
    /\b(stand\s*a\s*chance\s*to\s*win|zero\s*interest\s*emi\b)/i
  ];

  private static readonly FAILED_PATTERNS = [
    /\b(declined\s*(?:due\s*to|for)?|transaction\s*failed|payment\s*failed|unsuccessful)\b/i,
    /\b(insufficient\s*(?:funds|balance)|exceeded\s*limit|incorrect\s*pin)\b/i,
    /\b(e-?mandate\s*(?:registered|created|modified)|standing\s*instruction\s*registered)\b/i,
    /\b(bill\s*generated|statement\s*has\s*been\s*sent|card\s*dispatched|cheque\s*book\s*dispatched)\b/i,
    /\b(auto-?debit\s*scheduled|payment\s*due\s*(?:date|on)|due\s*by\b)/i
  ];

  private static readonly KNOWN_MERCHANTS: Record<string, { name: string; categoryId: string }> = {
    swiggy: { name: 'Swiggy', categoryId: 'cat_dining' },
    zomato: { name: 'Zomato', categoryId: 'cat_dining' },
    amazon: { name: 'Amazon', categoryId: 'cat_shopping' },
    flipkart: { name: 'Flipkart', categoryId: 'cat_shopping' },
    uber: { name: 'Uber', categoryId: 'cat_transport' },
    ola: { name: 'Ola', categoryId: 'cat_transport' },
    blinkit: { name: 'Blinkit', categoryId: 'cat_groceries' },
    zepto: { name: 'Zepto', categoryId: 'cat_groceries' },
    instamart: { name: 'Instamart', categoryId: 'cat_groceries' },
    myntra: { name: 'Myntra', categoryId: 'cat_shopping' },
    bigbasket: { name: 'BigBasket', categoryId: 'cat_groceries' },
    netflix: { name: 'Netflix', categoryId: 'cat_subscriptions' },
    spotify: { name: 'Spotify', categoryId: 'cat_subscriptions' },
    starbucks: { name: 'Starbucks', categoryId: 'cat_dining' },
    dominos: { name: "Domino's", categoryId: 'cat_dining' },
    mcdonald: { name: "McDonald's", categoryId: 'cat_dining' },
    makemytrip: { name: 'MakeMyTrip', categoryId: 'cat_travel' },
    irctc: { name: 'IRCTC', categoryId: 'cat_travel' },
    bookmyshow: { name: 'BookMyShow', categoryId: 'cat_entertainment' },
    cred: { name: 'CRED', categoryId: 'cat_bills' },
    tatacliq: { name: 'Tata Neu / CliQ', categoryId: 'cat_shopping' }
  };

  /**
   * Evaluates eligibility of an incoming raw SMS.
   */
  static classifyEligibility(input: RawSmsInput): EligibilityResult {
    const body = input.body.trim();
    if (!body) {
      return { isEligible: false, rejectionReason: 'EMPTY_BODY', tags: ['EMPTY'] };
    }

    // 1. Check OTP
    for (const pat of this.OTP_PATTERNS) {
      if (pat.test(body)) {
        return { isEligible: false, rejectionReason: 'OTP_OR_SECURITY_CODE', tags: ['OTP'] };
      }
    }

    // 2. Check Promotional unless cashback credited
    const isCashbackCredited = /cashback/i.test(body) && /(credited|received)/i.test(body);
    if (!isCashbackCredited) {
      for (const pat of this.PROMO_PATTERNS) {
        if (pat.test(body)) {
          return { isEligible: false, rejectionReason: 'PROMOTIONAL_OR_MARKETING', tags: ['PROMOTION'] };
        }
      }
    }

    // 3. Check Failed / Service Alerts
    for (const pat of this.FAILED_PATTERNS) {
      if (pat.test(body)) {
        return { isEligible: false, rejectionReason: 'FAILED_OR_SERVICE_ALERT', tags: ['NON_FINANCIAL'] };
      }
    }

    // 4. Must contain currency
    const hasCurrency = /(?:inr|rs\.?|₹)\s*[0-9,]+(?:\.[0-9]{1,2})?/i.test(body);
    if (!hasCurrency) {
      return { isEligible: false, rejectionReason: 'NO_MONETARY_AMOUNT', tags: ['NO_AMOUNT'] };
    }

    // 5. Must contain financial action verb
    const hasVerb = /\b(debited|spent|paid|withdrawn|credited|received|transferred|transfer|sent|deposited|refunded|refund|reversed|reversal|cash\s*withdrawal|atm\s*withdrawal)\b/i.test(body);
    if (!hasVerb) {
      return { isEligible: false, rejectionReason: 'NO_FINANCIAL_ACTION_VERB', tags: ['NO_ACTION'] };
    }

    // 6. Check pure balance query
    const isBalanceOnly = /^[^a-zA-Z]*(?:your)?\s*(?:avail(?:able)?|clear|current)?\s*bal(?:ance)?\s*(?:is|in|of)?[:\s]*(?:inr|rs\.?|₹)?\s*[0-9,]+(?:\.[0-9]{1,2})?[^a-zA-Z]*$/i.test(body);
    if (isBalanceOnly) {
      return { isEligible: false, rejectionReason: 'BALANCE_ONLY_NOTIFICATION', tags: ['BALANCE_ONLY'] };
    }

    return { isEligible: true, tags: ['TRANSACTIONAL'] };
  }

  /**
   * Extracts transaction amount and remaining balance amount.
   */
  static extractAmounts(body: string): ParsedAmounts {
    const balancePatterns = [
      /(?:avail(?:able)?|clear|total|current|updated|remaining)?\s*bal(?:ance)?\s*(?:is|in|of)?[:\s]*(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)/i,
      /(?:avail(?:able)?|clear)\s*limit[:\s]*(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)/i,
      /(?:bal|balance)[:\s]*(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)/i
    ];

    let balanceAmount: number | null = null;
    let balanceMatchRange: [number, number] | null = null;

    for (const pat of balancePatterns) {
      const match = pat.exec(body);
      if (match && match[1]) {
        const val = parseFloat(match[1].replace(/,/g, ''));
        if (!isNaN(val)) {
          balanceAmount = val;
          balanceMatchRange = [match.index, match.index + match[0].length];
          break;
        }
      }
    }

    const txPatterns = [
      /(?:debited(?:\s*by)?|spent|paid|withdrawn|sent|transferred(?:\s*to)?|purchase\s*of|deposited(?:\s*to)?|credited(?:\s*with)?|credited(?:\s*by)?|received)\s*(?:of)?\s*(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)/i,
      /(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)\s*(?:debited|spent|paid|withdrawn|sent|credited|deposited|transferred)/i,
      /(?:cash\s*withdrawal\s*(?:of)?)\s*(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)/i,
      /(?:inr|rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)/i
    ];

    let transactionAmount: number | null = null;

    for (const pat of txPatterns) {
      const match = pat.exec(body);
      if (match && match[1]) {
        // Ensure this amount is not the balance amount
        if (balanceMatchRange && match.index >= balanceMatchRange[0] && match.index <= balanceMatchRange[1]) {
          continue;
        }
        const val = parseFloat(match[1].replace(/,/g, ''));
        if (!isNaN(val) && val > 0) {
          transactionAmount = val;
          break;
        }
      }
    }

    return {
      transactionAmount,
      balanceAmount,
      currency: 'INR'
    };
  }

  /**
   * Classifies transaction type.
   */
  static classifyType(body: string): TransactionType {
    if (/\b(atm\s*withdrawal|cash\s*withdrawal|withdrawn\s*(?:from|at)\s*atm)\b/i.test(body)) {
      return TransactionType.CASH_WITHDRAWAL;
    }
    if (/\b(refunded|refund|reversed|reversal)\b/i.test(body)) {
      return TransactionType.REFUND;
    }
    if (/\b(transferred\s*to\s*(?:a\/c|account)|transfer\s*to\s*self|own\s*account)\b/i.test(body)) {
      return TransactionType.TRANSFER;
    }
    if (/\b(credited|received|deposited|salary|cashback)\b/i.test(body)) {
      return TransactionType.INCOME;
    }
    return TransactionType.EXPENSE;
  }

  /**
   * Extracts merchant or payee from SMS body.
   */
  static extractMerchant(body: string): { name: string; categoryId?: string } {
    const lower = body.toLowerCase();

    // Check known merchants first
    for (const key of Object.keys(this.KNOWN_MERCHANTS)) {
      if (lower.includes(key)) {
        return this.KNOWN_MERCHANTS[key];
      }
    }

    // Pattern 1: "at <Merchant> on" or "to <Merchant> on"
    const atMatch = /(?:at|to|towards|vpa|info)\s+([A-Za-z0-9\s&'-]{3,28}?)(?:\s+on|\s+via|\s+ref|\s+using|\s+avl|\s+bal|\.|$)/i.exec(body);
    if (atMatch && atMatch[1]) {
      const raw = atMatch[1].trim();
      if (!/^(the|a|my|bank|account|a\/c)$/i.test(raw)) {
        // Clean merchant
        const cleaned = raw.replace(/\b(ltd|pvt|corp|bank|branch|a\/c|account)\b/gi, '').trim();
        return { name: cleaned || raw };
      }
    }

    // Pattern 2: Salary credit
    if (/salary/i.test(body)) {
      const salMatch = /salary(?:\s*transfer\s*(?:from)?)?\s*([A-Za-z0-9\s&]{3,25})?/i.exec(body);
      const org = salMatch && salMatch[1] ? salMatch[1].trim() : 'Employer';
      return { name: `${org} / Salary`, categoryId: 'cat_salary' };
    }

    if (/\b(atm\s*withdrawal|cash\s*withdrawal)\b/i.test(body)) {
      return { name: 'ATM Cash Withdrawal', categoryId: 'cat_cash_withdrawal' };
    }

    return { name: 'Unknown Payee' };
  }

  /**
   * Extracts masked account number (e.g. XX4821 or 4821).
   */
  static extractAccountIdentifier(body: string): string | null {
    const pat = /(?:a\/c|acct|card|ending|xx|\*{2,4})\s*(?:no\.?)?\s*[:#-]?\s*[*xX]{0,6}([0-9]{4})\b/i;
    const m = pat.exec(body);
    return m ? m[1] : null;
  }

  /**
   * Extracts reference / UPI / UTR / RRN number.
   */
  static extractReferenceNumber(body: string): string | null {
    const pat = /\b(?:ref(?:\s*no\.?)?|rrn|utr|upi\s*ref|txn\s*id)\s*[:#-]?\s*([a-zA-Z0-9]{6,20})\b/i;
    const m = pat.exec(body);
    return m ? m[1] : null;
  }

  /**
   * Computes a deterministic deduplication hash.
   */
  static computeDedupHash(amount: number, timestamp: number, merchant: string, accountDigits?: string | null): string {
    const roundedAmt = Math.round(amount * 100);
    const timeBucket = Math.floor(timestamp / (1000 * 60 * 3));
    const cleanMerchant = (merchant || '').toLowerCase().replace(/[^a-z0-9]/g, '');
    const acc = accountDigits || 'none';
    const str = `${roundedAmt}_${timeBucket}_${cleanMerchant}_${acc}`;
    
    let hash = 5381;
    for (let i = 0; i < str.length; i++) {
      hash = ((hash << 5) + hash) + str.charCodeAt(i);
      hash = hash & hash;
    }
    return Math.abs(hash).toString(16);
  }

  /**
   * Unified single method: Parse an SMS text and return complete structured result.
   */
  static parse(text: string, timestamp: number = Date.now()): SmsParseResult {
    const input: RawSmsInput = {
      sender: 'BANK',
      body: text,
      timestamp
    };

    const eligibility = this.classifyEligibility(input);
    if (!eligibility.isEligible) {
      return {
        isEligible: false,
        isTransactional: false,
        rejectionReason: eligibility.rejectionReason,
        transactionType: TransactionType.EXPENSE,
        confidenceScore: 0.0,
        isSalaryCredit: false,
        warningTags: eligibility.tags,
        dedupHash: ''
      };
    }

    const amounts = this.extractAmounts(input.body);
    const type = this.classifyType(input.body);
    const merchantInfo = this.extractMerchant(input.body);
    const accountDigits = this.extractAccountIdentifier(input.body);
    const referenceNumber = this.extractReferenceNumber(input.body);

    const warningTags: string[] = [];
    let score = 1.0;

    if (!amounts.transactionAmount) {
      score -= 0.5;
      warningTags.push('NO_AMOUNT');
    }
    if (merchantInfo.name === 'Unknown Payee') {
      score -= 0.25;
      warningTags.push('UNKNOWN_MERCHANT');
    }
    if (!accountDigits) {
      score -= 0.15;
      warningTags.push('NO_ACCOUNT_ID');
    }
    if (score < 0.75) {
      warningTags.push('LOW_CONFIDENCE');
    }

    const dedupHash = this.computeDedupHash(
      amounts.transactionAmount || 0,
      input.timestamp,
      merchantInfo.name,
      accountDigits
    );

    const isSalary = type === TransactionType.INCOME && (/salary/i.test(merchantInfo.name) || /salary/i.test(text));

    let suggestedCategory = merchantInfo.categoryId || null;
    if (!suggestedCategory) {
      if (type === TransactionType.CASH_WITHDRAWAL) suggestedCategory = 'cat_cash_withdrawal';
      else if (isSalary) suggestedCategory = 'cat_salary';
      else if (type === TransactionType.INCOME) suggestedCategory = 'cat_other_income';
      else if (type === TransactionType.REFUND) suggestedCategory = 'cat_refund';
    }

    return {
      isEligible: true,
      isTransactional: Boolean(amounts.transactionAmount && amounts.transactionAmount > 0),
      amount: amounts.transactionAmount,
      remainingBalance: amounts.balanceAmount,
      balance: amounts.balanceAmount,
      transactionType: type,
      type,
      merchant: merchantInfo.name,
      maskedAccount: accountDigits,
      accountDigits,
      referenceNumber,
      confidenceScore: Math.max(0, Math.min(1.0, score)),
      suggestedCategoryId: suggestedCategory,
      isSalaryCredit: isSalary,
      warningTags,
      dedupHash
    };
  }

  static parseSms(input: RawSmsInput): SmsParseResult {
    return this.parse(input.body, input.timestamp);
  }
}
