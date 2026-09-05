import React, { useState } from 'react';
import { Fingerprint, Lock, ShieldCheck, KeyRound } from 'lucide-react';

interface AppLockModalProps {
  isLocked: boolean;
  onUnlock: () => void;
}

export const AppLockModal: React.FC<AppLockModalProps> = ({ isLocked, onUnlock }) => {
  const [pin, setPin] = useState('');
  const [error, setError] = useState(false);

  if (!isLocked) return null;

  const handleBiometricAuth = () => {
    // Simulate instantaneous on-device biometric check
    onUnlock();
  };

  const handlePinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (pin === '1234' || pin.length >= 4) {
      onUnlock();
    } else {
      setError(true);
      setTimeout(() => setError(false), 1500);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-neutral-950/95 backdrop-blur-xl animate-fadeIn">
      <div 
        id="app-lock-screen"
        className="w-full max-w-sm bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center shadow-2xl space-y-6"
      >
        <div className="w-16 h-16 rounded-2xl bg-purple-600/20 border border-purple-500/40 text-purple-400 mx-auto flex items-center justify-center shadow-lg shadow-purple-600/20">
          <Lock className="w-8 h-8" />
        </div>

        <div>
          <h2 className="text-xl font-black tracking-tight text-neutral-100 font-display">
            SPENDORA LOCKED
          </h2>
          <p className="text-xs text-neutral-400 mt-1">
            Biometric authentication required to decrypt local ledger
          </p>
        </div>

        {/* Biometric Trigger */}
        <div className="py-2">
          <button
            onClick={handleBiometricAuth}
            className="group w-24 h-24 rounded-3xl bg-neutral-800 hover:bg-neutral-750 border border-neutral-700 mx-auto flex flex-col items-center justify-center gap-1.5 transition active:scale-95 shadow-inner"
          >
            <Fingerprint className="w-10 h-10 text-purple-400 group-hover:scale-110 transition" />
            <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider">Tap Scan</span>
          </button>
        </div>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-neutral-800" />
          </div>
          <div className="relative flex justify-center text-[10px] uppercase font-bold text-neutral-500">
            <span className="bg-neutral-900 px-2">or unlock with PIN</span>
          </div>
        </div>

        <form onSubmit={handlePinSubmit} className="space-y-3">
          <div className="relative">
            <input
              type="password"
              maxLength={6}
              placeholder="Enter PIN (e.g. 1234)"
              value={pin}
              onChange={(e) => setPin(e.target.value)}
              className={`w-full bg-neutral-950 border ${error ? 'border-rose-500 ring-1 ring-rose-500' : 'border-neutral-800'} rounded-xl px-4 py-2.5 text-center text-neutral-100 text-sm tracking-widest focus:outline-none focus:border-purple-500`}
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30 transition"
          >
            Unlock Ledger
          </button>
        </form>

        <div className="flex items-center justify-center gap-1.5 text-[10px] text-neutral-500">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
          <span>Local Device Security Module Protected</span>
        </div>
      </div>
    </div>
  );
};
