import React, { useState } from 'react';
import { 
  Wallet, 
  CreditCard, 
  Building2, 
  Plus, 
  Archive, 
  Check, 
  Edit3, 
  Trash2,
  AlertCircle
} from 'lucide-react';
import { Account, AccountType } from '../types';

interface AccountsScreenProps {
  accounts: Account[];
  onSaveAccount: (account: Partial<Account>) => void;
  onToggleArchive: (accountId: string) => void;
}

export const AccountsScreen: React.FC<AccountsScreenProps> = ({
  accounts,
  onSaveAccount,
  onToggleArchive
}) => {
  const [activeTab, setActiveTab] = useState<'active' | 'archived'>('active');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingAccount, setEditingAccount] = useState<Account | null>(null);

  // Form state
  const [name, setName] = useState('');
  const [institution, setInstitution] = useState('');
  const [type, setType] = useState<AccountType>(AccountType.SAVINGS_BANK);
  const [maskedNumber, setMaskedNumber] = useState('');
  const [balance, setBalance] = useState('');
  const [error, setError] = useState<string | null>(null);

  const displayedAccounts = accounts.filter(a => activeTab === 'active' ? !a.isArchived : a.isArchived);

  const totalBalance = accounts
    .filter(a => !a.isArchived)
    .reduce((sum, a) => sum + a.currentBalance, 0);

  const openAddModal = (acc?: Account) => {
    if (acc) {
      setEditingAccount(acc);
      setName(acc.name);
      setInstitution(acc.institutionName || '');
      setType(acc.type);
      setMaskedNumber(acc.maskedNumber || '');
      setBalance(acc.currentBalance.toString());
    } else {
      setEditingAccount(null);
      setName('');
      setInstitution('');
      setType(AccountType.SAVINGS_BANK);
      setMaskedNumber('');
      setBalance('0');
    }
    setError(null);
    setIsModalOpen(true);
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please provide an account name.');
      return;
    }
    const parsedBal = parseFloat(balance);
    if (isNaN(parsedBal)) {
      setError('Please provide a valid balance number.');
      return;
    }

    onSaveAccount({
      ...(editingAccount ? { id: editingAccount.id } : {}),
      name: name.trim(),
      institutionName: institution.trim() || undefined,
      type,
      maskedNumber: maskedNumber.trim().slice(-4) || undefined,
      currentBalance: parsedBal,
      currency: 'INR',
      isArchived: editingAccount ? editingAccount.isArchived : false
    });

    setIsModalOpen(false);
  };

  return (
    <div className="space-y-6 pb-12 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-neutral-100 font-display">
            Accounts & Wallets
          </h1>
          <p className="text-xs text-neutral-400 mt-0.5">
            Total liquid balance: <strong className="text-purple-300">₹{totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</strong>
          </p>
        </div>

        <button
          onClick={() => openAddModal()}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold shadow-lg shadow-purple-600/30 transition active:scale-95 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Account</span>
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-neutral-800 pb-2">
        <button
          onClick={() => setActiveTab('active')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition ${
            activeTab === 'active'
              ? 'bg-neutral-800 text-purple-300'
              : 'text-neutral-400 hover:text-neutral-200'
          }`}
        >
          Active Accounts ({accounts.filter(a => !a.isArchived).length})
        </button>
        <button
          onClick={() => setActiveTab('archived')}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition ${
            activeTab === 'archived'
              ? 'bg-neutral-800 text-purple-300'
              : 'text-neutral-400 hover:text-neutral-200'
          }`}
        >
          Archived ({accounts.filter(a => a.isArchived).length})
        </button>
      </div>

      {/* Accounts List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {displayedAccounts.length === 0 ? (
          <div className="col-span-full p-12 text-center text-xs text-neutral-500 rounded-3xl bg-neutral-900 border border-neutral-800">
            No accounts in this view.
          </div>
        ) : (
          displayedAccounts.map((acc) => (
            <div 
              key={acc.id}
              className="p-5 rounded-3xl bg-neutral-900/90 border border-neutral-800 hover:border-neutral-700 shadow-xl space-y-4 transition flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2.5">
                    <div className="w-10 h-10 rounded-2xl bg-purple-600/20 text-purple-400 flex items-center justify-center border border-purple-500/20">
                      {acc.type === AccountType.CREDIT_CARD ? (
                        <CreditCard className="w-5 h-5" />
                      ) : acc.type === AccountType.CASH_WALLET ? (
                        <Wallet className="w-5 h-5" />
                      ) : (
                        <Building2 className="w-5 h-5" />
                      )}
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-neutral-100">{acc.name}</h3>
                      <p className="text-[11px] text-neutral-400">
                        {acc.institutionName || 'Self'} {acc.maskedNumber ? `• •••• ${acc.maskedNumber}` : ''}
                      </p>
                    </div>
                  </div>

                  <span className="text-[10px] uppercase font-bold px-2 py-0.5 rounded-full bg-neutral-800 text-neutral-400">
                    {acc.type.replace('_', ' ')}
                  </span>
                </div>

                <div className="pt-2">
                  <span className="text-[10px] font-bold text-neutral-500 uppercase tracking-wider block">Balance</span>
                  <div className="text-xl font-black text-neutral-100 mt-0.5">
                    ₹{acc.currentBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-neutral-800/80">
                <button
                  onClick={() => openAddModal(acc)}
                  className="p-2 rounded-xl text-neutral-400 hover:text-neutral-200 hover:bg-neutral-800 transition"
                  title="Edit Account"
                >
                  <Edit3 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => onToggleArchive(acc.id)}
                  className="px-3 py-1.5 rounded-xl text-xs font-semibold text-neutral-400 hover:text-purple-300 hover:bg-neutral-800 transition flex items-center gap-1.5"
                  title={acc.isArchived ? 'Restore' : 'Archive account (Preserves history)'}
                >
                  <Archive className="w-3.5 h-3.5" />
                  <span>{acc.isArchived ? 'Restore' : 'Archive'}</span>
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add / Edit Account Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
          <div className="w-full max-w-md bg-neutral-900 border border-neutral-800 rounded-3xl shadow-2xl p-6 space-y-4">
            <h2 className="text-lg font-bold text-neutral-100">
              {editingAccount ? 'Edit Account' : 'Add Financial Account'}
            </h2>

            {error && (
              <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400" />
                <span>{error}</span>
              </div>
            )}

            <form onSubmit={handleFormSubmit} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                  Account Name *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. HDFC Salary Account"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2 text-xs text-neutral-100 focus:border-purple-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                  Bank / Financial Institution
                </label>
                <input
                  type="text"
                  placeholder="e.g. HDFC Bank, ICICI, SBI"
                  value={institution}
                  onChange={(e) => setInstitution(e.target.value)}
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2 text-xs text-neutral-100 focus:border-purple-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                    Account Type
                  </label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value as AccountType)}
                    className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-2.5 py-2 text-xs text-neutral-200 focus:border-purple-500 focus:outline-none"
                  >
                    <option value={AccountType.SAVINGS_BANK}>Savings Bank</option>
                    <option value={AccountType.CURRENT_BANK}>Current Bank</option>
                    <option value={AccountType.CREDIT_CARD}>Credit Card</option>
                    <option value={AccountType.DIGITAL_WALLET}>Digital Wallet</option>
                    <option value={AccountType.CASH_WALLET}>Cash in Hand</option>
                    <option value={AccountType.INVESTMENT}>Investment</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                    Last 4 Digits
                  </label>
                  <input
                    type="text"
                    maxLength={4}
                    placeholder="e.g. 4092"
                    value={maskedNumber}
                    onChange={(e) => setMaskedNumber(e.target.value)}
                    className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2 text-xs text-neutral-100 focus:border-purple-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-neutral-400 uppercase tracking-wider mb-1">
                  Current Balance (₹) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="0.00"
                  value={balance}
                  onChange={(e) => setBalance(e.target.value)}
                  className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-3.5 py-2 text-xs text-neutral-100 focus:border-purple-500 focus:outline-none font-semibold"
                />
              </div>

              <div className="pt-3 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl border border-neutral-800 text-neutral-300 text-xs font-semibold hover:bg-neutral-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs shadow-lg shadow-purple-600/30"
                >
                  Save Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
