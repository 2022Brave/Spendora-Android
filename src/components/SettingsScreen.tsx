import React, { useRef, useState } from 'react';
import { 
  Settings, 
  Calendar, 
  ShieldCheck, 
  Lock, 
  Database, 
  Download, 
  Upload, 
  RefreshCw, 
  Tag, 
  CreditCard, 
  CheckCircle2, 
  AlertCircle,
  FileText
} from 'lucide-react';
import { AppSettings, FinancialCycleInfo } from '../types';

interface SettingsScreenProps {
  settings: AppSettings;
  cycleInfo: FinancialCycleInfo;
  onUpdateSettings: (newSettings: Partial<AppSettings>) => void;
  onOpenCycleModal: () => void;
  onOpenPrivacyModal: () => void;
  onNavigate: (tab: string) => void;
  onExportBackup: () => void;
  onImportBackup: (jsonContent: string) => boolean;
  onResetData: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  settings,
  cycleInfo,
  onUpdateSettings,
  onOpenCycleModal,
  onOpenPrivacyModal,
  onNavigate,
  onExportBackup,
  onImportBackup,
  onResetData
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [importStatus, setImportStatus] = useState<{ success: boolean; message: string } | null>(null);
  const [showConfirmReset, setShowConfirmReset] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const content = event.target?.result as string;
        const ok = onImportBackup(content);
        if (ok) {
          setImportStatus({ success: true, message: 'Backup successfully restored! Ledger updated.' });
        } else {
          setImportStatus({ success: false, message: 'Failed to restore: Invalid Spendora backup format.' });
        }
      } catch (err) {
        setImportStatus({ success: false, message: 'Corrupted backup JSON file.' });
      }
    };
    reader.readAsText(file);
    // Reset file input value
    e.target.value = '';
  };

  return (
    <div className="space-y-6 pb-12 animate-fadeIn max-w-4xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-black text-neutral-100 font-display">
          Settings & Architecture
        </h1>
        <p className="text-xs text-neutral-400 mt-0.5">
          Local configuration, offline security parameters, and data portability
        </p>
      </div>

      {importStatus && (
        <div className={`p-4 rounded-2xl text-xs flex items-center gap-2.5 animate-fadeIn ${
          importStatus.success 
            ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-300' 
            : 'bg-rose-500/10 border border-rose-500/30 text-rose-300'
        }`}>
          {importStatus.success ? <CheckCircle2 className="w-4 h-4 text-emerald-400" /> : <AlertCircle className="w-4 h-4 text-rose-400" />}
          <span>{importStatus.message}</span>
        </div>
      )}

      {/* Financial Cycle Setting */}
      <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
              <Calendar className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-neutral-100">Financial Cycle Configuration</h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                Current: Day {settings.financialCycleStartDay} rollover ({cycleInfo.displayLabel})
              </p>
            </div>
          </div>

          <button
            onClick={onOpenCycleModal}
            className="px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-md shadow-purple-600/30 transition"
          >
            Adjust Cycle Day
          </button>
        </div>
        <p className="text-[11px] text-neutral-500">
          Modifying your cycle start date recalculates your active cycle boundaries and budget caps immediately with zero data loss.
        </p>
      </div>

      {/* Security & App Lock */}
      <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-neutral-100">Biometric App Lock</h3>
              <p className="text-xs text-neutral-400 mt-0.5">
                Require biometric authentication or PIN to view transactions
              </p>
            </div>
          </div>

          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={settings.isAppLockEnabled}
              onChange={(e) => onUpdateSettings({ isAppLockEnabled: e.target.checked })}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-neutral-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-purple-600"></div>
          </label>
        </div>
      </div>

      {/* Sub-panels Navigation Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div 
          onClick={() => onNavigate('accounts')}
          className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 hover:border-neutral-700 transition cursor-pointer flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-600/20 text-indigo-400 flex items-center justify-center">
              <CreditCard className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-neutral-100">Manage Accounts</h4>
              <p className="text-[11px] text-neutral-400">Configure bank accounts, credit cards & wallets</p>
            </div>
          </div>
        </div>

        <div 
          onClick={() => onNavigate('categories')}
          className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 hover:border-neutral-700 transition cursor-pointer flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
              <Tag className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-neutral-100">Manage Categories</h4>
              <p className="text-[11px] text-neutral-400">27 system categories + custom classifications</p>
            </div>
          </div>
        </div>
      </div>

      {/* Data Backup & Portability */}
      <div className="p-6 rounded-3xl bg-neutral-900/90 border border-neutral-800 shadow-xl space-y-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center">
            <Database className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-neutral-100">Data Portability & Backup</h3>
            <p className="text-xs text-neutral-400 mt-0.5">
              Export and import your complete encrypted or plaintext offline database
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
          <button
            onClick={onExportBackup}
            className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 hover:border-purple-500/50 flex items-center gap-3 transition text-left group"
          >
            <Download className="w-5 h-5 text-purple-400 group-hover:scale-110 transition" />
            <div>
              <h4 className="text-xs font-bold text-neutral-200">Export JSON Backup</h4>
              <p className="text-[10px] text-neutral-500">Save full ledger to an offline file</p>
            </div>
          </button>

          <button
            onClick={() => fileInputRef.current?.click()}
            className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 hover:border-purple-500/50 flex items-center gap-3 transition text-left group"
          >
            <Upload className="w-5 h-5 text-purple-400 group-hover:scale-110 transition" />
            <div>
              <h4 className="text-xs font-bold text-neutral-200">Restore Backup File</h4>
              <p className="text-[10px] text-neutral-500">Import a previously saved Spendora JSON</p>
            </div>
          </button>
          <input
            type="file"
            ref={fileInputRef}
            accept=".json"
            onChange={handleFileChange}
            className="hidden"
          />
        </div>

        {/* Reset Ledger */}
        <div className="pt-3 border-t border-neutral-800/80 flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-neutral-300 block">Reset Ledger to Sample State</span>
            <span className="text-[10px] text-neutral-500">Restore default demo accounts and categories</span>
          </div>

          {showConfirmReset ? (
            <div className="flex items-center gap-2">
              <button
                onClick={() => {
                  onResetData();
                  setShowConfirmReset(false);
                }}
                className="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white text-xs font-bold transition"
              >
                Confirm Reset
              </button>
              <button
                onClick={() => setShowConfirmReset(false)}
                className="px-3 py-1.5 rounded-xl bg-neutral-800 text-neutral-300 text-xs font-semibold"
              >
                Cancel
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowConfirmReset(true)}
              className="px-3.5 py-1.5 rounded-xl bg-neutral-800 hover:bg-rose-500/20 text-neutral-400 hover:text-rose-400 border border-neutral-700 text-xs font-semibold transition"
            >
              Reset Data
            </button>
          )}
        </div>
      </div>

      {/* Offline Privacy Guarantee link */}
      <div 
        onClick={onOpenPrivacyModal}
        className="p-5 rounded-3xl bg-purple-950/20 border border-purple-800/30 hover:border-purple-600/40 transition cursor-pointer flex items-center justify-between"
      >
        <div className="flex items-center gap-3">
          <ShieldCheck className="w-6 h-6 text-purple-400 shrink-0" />
          <div>
            <h4 className="text-xs font-bold text-purple-200">Spendora Offline Privacy Guarantee</h4>
            <p className="text-[11px] text-neutral-400">Read our strict zero-cloud, 100% on-device data sovereignty policy</p>
          </div>
        </div>
        <span className="text-xs font-bold text-purple-400">View Guarantee &rarr;</span>
      </div>
    </div>
  );
};
