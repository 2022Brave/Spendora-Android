import React, { useState } from 'react';
import { 
  Tag, 
  Plus, 
  Check, 
  Trash2, 
  Archive, 
  AlertCircle,
  Sparkles
} from 'lucide-react';
import { Category } from '../types';

interface CategoriesScreenProps {
  categories: Category[];
  onAddCategory: (name: string, type: 'EXPENSE' | 'INCOME', colorHex: string) => void;
  onArchiveCategory: (id: string) => void;
}

const PRESET_COLORS = [
  '#E91E63', '#9C27B0', '#673AB7', '#3F51B5', 
  '#2196F3', '#009688', '#4CAF50', '#8BC34A', 
  '#FF9800', '#FF5722', '#795548', '#607D8B'
];

export const CategoriesScreen: React.FC<CategoriesScreenProps> = ({
  categories,
  onAddCategory,
  onArchiveCategory
}) => {
  const [filterType, setFilterType] = useState<'ALL' | 'EXPENSE' | 'INCOME'>('ALL');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState<'EXPENSE' | 'INCOME'>('EXPENSE');
  const [color, setColor] = useState('#9C27B0');
  const [error, setError] = useState<string | null>(null);

  const displayedCategories = categories.filter(c => {
    if (c.isArchived) return false;
    if (filterType === 'ALL') return true;
    return c.type === filterType;
  });

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please enter a category name');
      return;
    }
    onAddCategory(name.trim(), type, color);
    setName('');
    setIsAddOpen(false);
  };

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-neutral-100 font-display">
            Categories Directory
          </h1>
          <p className="text-xs text-neutral-400 mt-0.5">
            27 built-in financial categories + custom user classifications
          </p>
        </div>

        <button
          onClick={() => {
            setError(null);
            setIsAddOpen(true);
          }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Category</span>
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2">
        {['ALL', 'EXPENSE', 'INCOME'].map(t => (
          <button
            key={t}
            onClick={() => setFilterType(t as any)}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition ${
              filterType === t
                ? 'bg-purple-600 text-white'
                : 'bg-neutral-900 text-neutral-400 hover:bg-neutral-800'
            }`}
          >
            {t === 'ALL' ? 'All Categories' : `${t.charAt(0) + t.slice(1).toLowerCase()} Only`}
          </button>
        ))}
      </div>

      {/* Categories Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
        {displayedCategories.map((c) => (
          <div
            key={c.id}
            className="p-4 rounded-2xl bg-neutral-900/90 border border-neutral-800 hover:border-neutral-700 transition flex flex-col items-center text-center justify-between gap-3 group relative"
          >
            <div 
              className="w-12 h-12 rounded-2xl flex items-center justify-center text-white text-sm font-bold shadow-md shadow-purple-900/20"
              style={{ backgroundColor: c.colorHex }}
            >
              {c.name.substring(0, 2).toUpperCase()}
            </div>

            <div className="w-full">
              <h4 className="text-xs font-bold text-neutral-100 truncate">{c.name}</h4>
              <span className={`text-[10px] font-semibold uppercase mt-0.5 block ${c.type === 'INCOME' ? 'text-emerald-400' : 'text-neutral-500'}`}>
                {c.type}
              </span>
            </div>

            {!c.isDefault && (
              <button
                onClick={() => onArchiveCategory(c.id)}
                className="opacity-0 group-hover:opacity-100 absolute top-2 right-2 p-1 text-neutral-400 hover:text-rose-400 transition"
                title="Archive custom category"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        ))}
      </div>

      {/* Add Category Modal */}
      {isAddOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-sm bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-6 space-y-4">
            <h2 className="text-base font-bold text-neutral-100">Create Custom Category</h2>

            {error && (
              <div className="p-2.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleAddSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                  Category Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Pet Care, Gaming"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2 text-xs text-neutral-100 focus:border-purple-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                  Type
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setType('EXPENSE')}
                    className={`py-2 rounded-xl text-xs font-bold border transition ${
                      type === 'EXPENSE'
                        ? 'bg-rose-500/20 text-rose-300 border-rose-500/40'
                        : 'border-neutral-800 text-neutral-400 hover:bg-neutral-800'
                    }`}
                  >
                    Expense
                  </button>
                  <button
                    type="button"
                    onClick={() => setType('INCOME')}
                    className={`py-2 rounded-xl text-xs font-bold border transition ${
                      type === 'INCOME'
                        ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                        : 'border-neutral-800 text-neutral-400 hover:bg-neutral-800'
                    }`}
                  >
                    Income
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-2">
                  Theme Color
                </label>
                <div className="grid grid-cols-6 gap-2">
                  {PRESET_COLORS.map(c => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setColor(c)}
                      className={`w-8 h-8 rounded-xl flex items-center justify-center transition ${
                        color === c ? 'ring-2 ring-white scale-105' : 'hover:opacity-80'
                      }`}
                      style={{ backgroundColor: c }}
                    >
                      {color === c && <Check className="w-4 h-4 text-white" />}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-2 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsAddOpen(false)}
                  className="px-4 py-2 rounded-xl border border-neutral-800 text-neutral-300 text-xs font-semibold hover:bg-neutral-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30"
                >
                  Add Category
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
