import React from 'react';
import { X, ShieldCheck, Lock, Cpu, Database, EyeOff } from 'lucide-react';

interface PrivacyPolicyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const PrivacyPolicyModal: React.FC<PrivacyPolicyModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        id="privacy-policy-modal"
        className="w-full max-w-lg bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]"
      >
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/60">
          <div className="flex items-center gap-2 text-purple-400 font-bold text-sm">
            <ShieldCheck className="w-5 h-5" />
            <span>Spendora Offline Privacy Guarantee</span>
          </div>
          <button onClick={onClose} className="p-1 rounded-xl text-neutral-400 hover:text-neutral-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4 overflow-y-auto text-xs leading-relaxed text-neutral-300">
          <div className="p-4 rounded-2xl bg-purple-950/40 border border-purple-800/40 text-purple-200 flex items-center gap-3">
            <Lock className="w-6 h-6 text-purple-400 shrink-0" />
            <div>
              <p className="font-bold text-sm">Zero Cloud. Zero Telemetry. 100% On-Device.</p>
              <p className="text-[11px] text-purple-300/80 mt-0.5">Your financial ledger never leaves this device.</p>
            </div>
          </div>

          <div className="space-y-3">
            <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-1">
              <h4 className="font-bold text-neutral-100 flex items-center gap-1.5 text-sm">
                <Cpu className="w-4 h-4 text-purple-400" />
                1. Strict Offline Architecture
              </h4>
              <p className="text-neutral-400">
                Spendora is architected with complete isolation from remote servers, ad networks, and third-party analytical trackers. There are zero outbound network requests for transaction data.
              </p>
            </div>

            <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-1">
              <h4 className="font-bold text-neutral-100 flex items-center gap-1.5 text-sm">
                <Database className="w-4 h-4 text-indigo-400" />
                2. Deterministic Pattern Matching
              </h4>
              <p className="text-neutral-400">
                All financial transaction parsing occurs locally using deterministic regular expression pattern matching. Full raw SMS bodies are never permanently retained once transactions are confirmed.
              </p>
            </div>

            <div className="p-3.5 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-1">
              <h4 className="font-bold text-neutral-100 flex items-center gap-1.5 text-sm">
                <EyeOff className="w-4 h-4 text-emerald-400" />
                3. Raw SMS Excerpt Purge Policy
              </h4>
              <p className="text-neutral-400">
                Raw SMS excerpts are stored exclusively in temporary review cache for ambiguous alerts. As soon as you confirm or edit a transaction, the raw SMS text is permanently purged.
              </p>
            </div>
          </div>
        </div>

        <div className="p-4 border-t border-neutral-800 bg-neutral-900/60 flex justify-end">
          <button
            onClick={onClose}
            className="px-6 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30 transition"
          >
            I Understand & Agree
          </button>
        </div>
      </div>
    </div>
  );
};
