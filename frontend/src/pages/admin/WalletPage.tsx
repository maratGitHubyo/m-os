import { useState } from 'react';
import { creditWallet, debitWallet } from '../../api/admin/wallet';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { DataTable } from '../../components/admin/DataTable';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { showToast } from '../../stores/toastStore';

export function AdminWalletPage() {
  const { players, loading, error, reload } = useAdminPlayers();
  const [userId, setUserId] = useState('');
  const [amount, setAmount] = useState('10');
  const [description, setDescription] = useState('Admin adjustment');
  const [submitting, setSubmitting] = useState(false);

  const handleOperation = async (type: 'credit' | 'debit') => {
    if (!userId) {
      showToast('Select a player', 'error');
      return;
    }

    const parsedAmount = Number(amount);
    if (!parsedAmount || parsedAmount < 1) {
      showToast('Amount must be at least 1', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const request = { amount: parsedAmount, description };
      if (type === 'credit') {
        await creditWallet(userId, request);
        showToast('Coins credited');
      } else {
        await debitWallet(userId, request);
        showToast('Coins debited');
      }
      await reload();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Wallet operation failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Wallet"
        description="Credit or debit coins for session players."
      />

      <PageState loading={loading} error={error} empty={players.length === 0}>
        <FormCard title="Wallet operation">
          <div className="admin-form-grid">
            <label className="form-field">
              <span>Player</span>
              <PlayerSelect players={players} value={userId} onChange={setUserId} required />
            </label>
            <label className="form-field">
              <span>Amount</span>
              <input
                type="number"
                min={1}
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
              />
            </label>
            <label className="form-field form-field--wide">
              <span>Description</span>
              <input
                type="text"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
              />
            </label>
          </div>
          <div className="admin-actions">
            <button
              type="button"
              className="btn btn--primary"
              disabled={submitting}
              onClick={() => void handleOperation('credit')}
            >
              Credit
            </button>
            <button
              type="button"
              className="btn btn--danger"
              disabled={submitting}
              onClick={() => void handleOperation('debit')}
            >
              Debit
            </button>
          </div>
        </FormCard>

        <h2 className="section-title">Players (leaderboard)</h2>
        <DataTable
          rows={players}
          rowKey={(row) => row.userId}
          columns={[
            { key: 'nickname', header: 'Player', render: (row) => row.nickname },
            { key: 'userId', header: 'User ID', render: (row) => <span className="mono">{row.userId}</span> },
            { key: 'score', header: 'Score', render: (row) => row.totalScore },
          ]}
        />
      </PageState>
    </section>
  );
}
