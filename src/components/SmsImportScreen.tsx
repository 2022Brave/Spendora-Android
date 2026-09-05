import React, { useState } from 'react';
import { 
  MessageSquareCode, 
  Sparkles, 
  CheckCircle2, 
  AlertCircle, 
  ShieldCheck, 
  ArrowRight, 
  Copy, 
  Play, 
  RefreshCw,
  Hash,
  Database,
  Check,
  Ban
} from 'lucide-react';
import { SmsEngine, SAMPLE_SMS_TEMPLATES } from '../services/smsEngine';
import { SmsParseResult, Category, Account } from '../types';

interface SmsImportScreenProps {
  categories: Category[];
  accounts: Account[];
  onCommitParsedTransaction: (result: SmsParseResult, rawText: string) => { success: boolean; message: string };
  onBatchProcess: (samples: typeof SAMPLE_SMS_TEMPLATES) => number;
}

export const SmsImportScreen: React.FC<SmsImportScreenProps> = ({
  categories,
  accounts,
  onCommitParsedTransaction,
  onBatchProcess
}) => {
  const [inputText, setInputText] = useState(SAMPLE_SMS_TEMPLATES[0].text);
  const [parseResult, setParseResult] = useState<SmsParseResult | null>(() => SmsEngine.parse(SAMPLE_SMS_TEMPLATES[0].text));
  const [commitMessage, setCommitMessage] = useState<{ success: boolean; text: string } | null>(null);
  const [batchCountMessage, setBatchCountMessage] = useState<string | null>(null);

  const handleParse = (text: string) => {
    setInputText(text);
    setCommitMessage(null);
    const res = SmsEngine.parse(text);
    setParseResult(res);
  };

  const handleCommit = () => {
    if (!parseResult || !parseResult.isTransactional) return;
    const res = onCommitParsedTransaction(parseResult, inputText);
    setCommitMessage({ success: res.success, text: res.message });
  };

  const handleRunBatch = () => {
    const addedCount = onBatchProcess(SAMPLE_SMS_TEMPLATES);
    setBatchCountMessage(`Processed sample bank stream: ${addedCount} new transactions added to ledger.`);
    setTimeout(() => setBatchCountMessage(null), 4000);
  };

  const getCategoryName = (catId?: string | null) => categories.find(c => c.id === catId)?.name || 'Uncategorized';

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-black text-neutral-100 font-display">
              Deterministic SMS Engine
            </h1>
            <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
              Regex V2 Pipeline
            </span>
          </div>
          <p className="text-xs text-neutral-400 mt-0.5">
            100% on-device deterministic regex parser for banking and UPI SMS alerts
          </p>
        </div>

        <button
          onClick={handleRunBatch}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-neutral-800 hover:bg-neutral-700 text-purple-300 border border-purple-500/30 text-xs font-bold transition shadow-md self-start sm:self-auto"
        >
          <Play className="w-3.5 h-3.5 text-purple-400" />
          <span>Simulate Bank SMS Stream</span>
        </button>
      </div>

      {batchCountMessage && (
        <div className="p-3.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2 animate-fadeIn">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>{batchCountMessage}</span>
        </div>
      )}

      {/* Preset Templates Carousel */}
      <div className="space-y-2">
        <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider">
          Select Bank Sample SMS
        </label>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
          {SAMPLE_SMS_TEMPLATES.map((tpl) => (
            <button
              key={tpl.name}
              onClick={() => handleParse(tpl.text)}
              className={`p-2.5 text-left rounded-xl border text-xs transition truncate ${
                inputText === tpl.text
                  ? 'bg-purple-600/20 border-purple-500 text-purple-200 shadow-sm'
                  : 'bg-neutral-900 border-neutral-800 text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800'
              }`}
            >
              <span className="font-bold block truncate">{tpl.name}</span>
              <span className="text-[10px] text-neutral-500 block truncate">{tpl.bank}</span>
            </button>
          ))}
        </div>
      </div>

      {/* SMS Input Box */}
      <div className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold text-neutral-300 flex items-center gap-2">
            <MessageSquareCode className="w-4 h-4 text-purple-400" />
            <span>Raw SMS Text Input</span>
          </label>
          <button
            onClick={() => handleParse(inputText)}
            className="text-xs text-purple-400 hover:text-purple-300 font-semibold flex items-center gap-1"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Re-parse</span>
          </button>
        </div>

        <textarea
          rows={3}
          value={inputText}
          onChange={(e) => handleParse(e.target.value)}
          placeholder="Paste incoming bank SMS here..."
          className="w-full bg-neutral-950 border border-neutral-800 rounded-2xl p-3.5 text-xs text-neutral-200 font-mono focus:border-purple-500 focus:outline-none leading-relaxed"
        />

        <div className="flex items-center justify-between text-[11px] text-neutral-500">
          <span>Deterministic regex matching ensures zero data leakage</span>
          <span>{inputText.length} characters</span>
        </div>
      </div>

      {/* Parse Result Inspector */}
      {parseResult && (
        <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-5">
          <div className="flex items-center justify-between pb-4 border-b border-neutral-800">
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-2xl flex items-center justify-center font-bold text-sm ${
                parseResult.isTransactional 
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' 
                  : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
              }`}>
                {parseResult.isTransactional ? <Check className="w-5 h-5" /> : <Ban className="w-5 h-5" />}
              </div>
              <div>
                <h3 className="text-sm font-bold text-neutral-100">
                  {parseResult.isTransactional ? 'Valid Transaction Detected' : 'Non-Transactional Message'}
                </h3>
                <p className="text-xs text-neutral-400">
                  {parseResult.isTransactional 
                    ? `Confidence Score: ${Math.round(parseResult.confidenceScore * 100)}% (${parseResult.confidenceScore >= 0.85 ? 'Auto-Confirm Ready' : 'Pending Review Needed'})`
                    : 'Classified as promotional spam or service OTP. Ignored.'
                  }
                </p>
              </div>
            </div>

            {parseResult.isTransactional && (
              <button
                id="add-parsed-txn-btn"
                onClick={handleCommit}
                className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95"
              >
                <span>Commit to Ledger</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}
          </div>

          {commitMessage && (
            <div className={`p-3.5 rounded-2xl text-xs flex items-center gap-2.5 animate-fadeIn ${
              commitMessage.success 
                ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300' 
                : 'bg-amber-500/10 border border-amber-500/30 text-amber-300'
            }`}>
              {commitMessage.success ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <AlertCircle className="w-4 h-4 text-amber-400" />}
              <span>{commitMessage.text}</span>
            </div>
          )}

          {/* Extracted Fields Table */}
          {parseResult.isTransactional ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Extracted Amount</span>
                <span className="text-lg font-black text-neutral-100 mt-0.5 block">
                  ₹{parseResult.amount?.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </span>
              </div>

              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Transaction Type</span>
                <span className="text-sm font-black text-purple-300 mt-1 block">
                  {parseResult.transactionType}
                </span>
              </div>

              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Merchant / Payee</span>
                <span className="text-sm font-bold text-neutral-100 mt-1 block truncate">
                  {parseResult.merchant || 'Unknown'}
                </span>
              </div>

              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Masked Account</span>
                <span className="text-sm font-mono text-neutral-300 mt-1 block">
                  {parseResult.maskedAccount ? `•••• ${parseResult.maskedAccount}` : 'Not specified'}
                </span>
              </div>

              {parseResult.remainingBalance != null && (
                <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                  <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Remaining Balance</span>
                  <span className="text-sm font-bold text-emerald-400 mt-1 block">
                    ₹{parseResult.remainingBalance.toLocaleString('en-IN')}
                  </span>
                </div>
              )}

              {parseResult.referenceNumber && (
                <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                  <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Reference / UTR</span>
                  <span className="text-xs font-mono text-neutral-400 mt-1 block truncate">
                    {parseResult.referenceNumber}
                  </span>
                </div>
              )}

              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Suggested Category</span>
                <span className="text-xs font-bold text-indigo-300 mt-1 block">
                  {getCategoryName(parseResult.suggestedCategoryId)}
                </span>
              </div>

              <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800">
                <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Deduplication Hash</span>
                <span className="text-[10px] font-mono text-neutral-400 mt-1 block truncate">
                  {parseResult.dedupHash}
                </span>
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 text-xs text-neutral-400 leading-relaxed">
              Spendora's safety filter recognized this SMS as informational or spam. Such messages are automatically dropped without recording, preventing ghost debits or duplicate notifications.
            </div>
          )}
        </div>
      )}
    </div>
  );
};
