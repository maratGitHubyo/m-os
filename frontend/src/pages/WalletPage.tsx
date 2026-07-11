import { useCallback, useEffect, useState } from 'react';
import { fetchSessionPlayers } from '../api/players';
import { fetchMyTransactions, fetchMyWallet, fetchTransfers, transferCoins } from '../api/wallet';
import { PageState } from '../components/ui/PageState';
import { translateError } from '../i18n/ru';
import { showToast } from '../stores/toastStore';
import { useAuth } from '../stores/authStore';
import type { CoinTransaction, CoinTransfer, SessionPlayer, Wallet } from '../types';

function formatTransfer(transfer: CoinTransfer, currentUserId: string): string {
  if (transfer.senderUserId === currentUserId) {
    return `-${transfer.amount} М-коинов для ${transfer.receiverNickname}`;
  }

  return `+${transfer.amount} М-коинов от ${transfer.senderNickname}`;
}

export function WalletPage() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<CoinTransaction[]>([]);
  const [transfers, setTransfers] = useState<CoinTransfer[]>([]);
  const [players, setPlayers] = useState<SessionPlayer[]>([]);
  const [receiverUserId, setReceiverUserId] = useState('');
  const [amount, setAmount] = useState('10');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadWalletData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const [walletData, txPage, transferPage, sessionPlayers] = await Promise.all([
        fetchMyWallet(),
        fetchMyTransactions(),
        fetchTransfers(),
        fetchSessionPlayers(),
      ]);

      setWallet(walletData);
      setTransactions(txPage.content);
      setTransfers(transferPage.content);
      setPlayers(sessionPlayers.filter((player) => player.id !== user?.id));
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить кошелёк',
      );
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    void loadWalletData();
  }, [loadWalletData]);

  const handleTransfer = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!receiverUserId) {
      showToast('Выберите игрока', 'error');
      return;
    }

    const parsedAmount = Number(amount);
    if (!parsedAmount || parsedAmount < 1) {
      showToast('Сумма должна быть не менее 1', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await transferCoins({ receiverUserId, amount: parsedAmount });
      showToast('Перевод отправлен');
      setAmount('10');
      await loadWalletData();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выполнить перевод',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="wallet-page">
      <h1>Кошелёк</h1>
      <p className="page-hint">Баланс М-коинов, переводы и история транзакций.</p>

      <PageState loading={loading} error={error} loadingLabel="Загрузка кошелька…">
        {wallet && user && (
        <>
          <article className="card wallet-balance">
            <h2>Текущий баланс</h2>
            <p className="dashboard-stat">{wallet.balance} М-коинов</p>
          </article>

          <article className="card wallet-transfer">
            <h2>Перевод M-coins</h2>
            <form className="wallet-transfer__form" onSubmit={handleTransfer}>
              <label className="form-field">
                <span>Игрок</span>
                <select
                  value={receiverUserId}
                  onChange={(event) => setReceiverUserId(event.target.value)}
                  required
                >
                  <option value="">Выберите игрока</option>
                  {players.map((player) => (
                    <option key={player.id} value={player.id}>
                      {player.nickname}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-field">
                <span>Сумма</span>
                <input
                  type="number"
                  min={1}
                  value={amount}
                  onChange={(event) => setAmount(event.target.value)}
                  required
                />
              </label>
              <button type="submit" className="btn btn--primary" disabled={submitting}>
                {submitting ? 'Отправка…' : 'Отправить'}
              </button>
            </form>
          </article>

          <h2 className="section-title">Переводы</h2>
          {transfers.length === 0 ? (
            <p className="empty-state">Переводов пока нет.</p>
          ) : (
            <ul className="transaction-list">
              {transfers.map((transfer) => (
                <li key={transfer.id} className="transaction-item">
                  <div>
                    <strong>{formatTransfer(transfer, user.id)}</strong>
                    <span className="transaction-item__meta">
                      {new Date(transfer.createdAt).toLocaleString()}
                    </span>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <h2 className="section-title">Транзакции</h2>
          {transactions.length === 0 ? (
            <p className="empty-state">Транзакций пока нет.</p>
          ) : (
            <ul className="transaction-list">
              {transactions.map((tx) => (
                <li key={tx.id} className="transaction-item">
                  <div>
                    <strong>{tx.description ?? tx.type}</strong>
                    <span className="transaction-item__meta">
                      {new Date(tx.createdAt).toLocaleString()}
                    </span>
                  </div>
                  <span className={tx.amount >= 0 ? 'amount amount--plus' : 'amount amount--minus'}>
                    {tx.amount >= 0 ? '+' : ''}
                    {tx.amount}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
        )}
      </PageState>
    </section>
  );
}
