import { useEffect, useState } from 'react';
import { fetchMyTransactions, fetchMyWallet } from '../api/wallet';
import { PageState } from '../components/ui/PageState';
import { translateError } from '../i18n/ru';
import type { CoinTransaction, Wallet } from '../types';

export function WalletPage() {
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<CoinTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const [walletData, txPage] = await Promise.all([
          fetchMyWallet(),
          fetchMyTransactions(),
        ]);

        if (!cancelled) {
          setWallet(walletData);
          setTransactions(txPage.content);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? translateError(err.message) : 'Не удалось загрузить кошелёк',
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="wallet-page">
      <h1>Кошелёк</h1>
      <p className="page-hint">Баланс М-коинов и история транзакций.</p>

      <PageState loading={loading} error={error} loadingLabel="Загрузка кошелька…">
        {wallet && (
        <>
          <article className="card wallet-balance">
            <h2>Текущий баланс</h2>
            <p className="dashboard-stat">{wallet.balance} М-коинов</p>
          </article>

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
