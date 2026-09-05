import React, { useState } from 'react';
import { X, Calendar, CheckCircle2, Info } from 'lucide-react';
import { FinancialCycleService } from '../services/financialCycle';

interface FinancialCycleModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentDay: number;
  onSaveDay: (newDay: number) => void;
}

export const FinancialCycleModal: React.FC<FinancialCycleModalProps> = ({
  isOpen,
  onClose,
  currentDay,
  onSaveDay
}) => {
  const [day, setDay] = useState<number>(currentDay);
  const [savedSuccess, setSavedSuccess] = useState(false);

  if (!isOpen) return null;

  const cyclePreview = FinancialCycleService.getCycleInfo(day);

  const handleSave = () => {
    onSaveDay(day);
    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div 
        id="financial-cycle-modal"
        className="w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl overflow-hidden flex flex-col"
      >
        <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between bg-neutral-900/60">
          <div className="flex items-center gap-2 text-purple-400 font-bold text-sm">
            <Calendar className="w-5 h-5" />
            <span>Financial Cycle Settings</span>
          </div>
          <button onClick={onClose} className="p-1 rounded-xl text-neutral-400 hover:text-neutral-200">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <div className="p-3.5 rounded-2xl bg-purple-950/20 border border-purple-800/30 text-xs text-purple-200 flex gap-2.5">
            <Info className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              Spendora aligns budgets with your salary pay cycle. Modifying your start day is <strong>100% non-destructive</strong>: all your transactions remain safe.
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-neutral-950 border border-neutral-800 space-y-2">
            <span className="text-[11px] font-bold text-neutral-400 uppercase tracking-wider">
              Cycle Preview with Day {day}
            </span>
            <div className="text-sm font-bold text-neutral-100 flex items-center justify-between">
              <span>{cyclePreview.displayLabel}</span>
              <span className="text-xs text-purple-400 font-medium">{cyclePreview.daysRemaining} days remaining</span>
            </div>
            <p className="text-xs text-neutral-400">
              {cyclePreview.startDate} to {cyclePreview.endDate}
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-2">
              Cycle Start Day of Month (1 - 31)
            </label>
            <div className="flex items-center gap-4">
              <input
                type="number"
                min="1"
                max="31"
                value={day}
                onChange={(e) => setDay(Math.max(1, Math.min(31, parseInt(e.target.value) || 1)))}
                className="w-24 bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-2.5 text-center font-bold text-lg text-purple-300 focus:border-purple-500 focus:outline-none"
              />
              <div className="flex-1">
                <input
                  type="range"
                  min="1"
                  max="31"
                  value={day}
                  onChange={(e) => setDay(parseInt(e.target.value))}
                  className="w-full accent-purple-500 cursor-pointer"
                />
                <div className="flex justify-between text-[10px] text-neutral-500 mt-1">
                  <span>1st (Calendar Month)</span>
                  <span>25th (Standard Salary)</span>
                  <span>31st</span>
                </div>
              </div>
            </div>
          </div>

          {savedSuccess && (
            <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              <span>Reporting period updated successfully!</span>
            </div>
          )}

          <div className="pt-2 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl border border-neutral-800 text-neutral-300 text-xs font-semibold hover:bg-neutral-800"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              className="px-6 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30 transition"
            >
              Save Start Day
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
