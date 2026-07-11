import { useEffect, useState } from 'react';
import { fetchMyTransactions, fetchMyWallet } from '../api/wallet';
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
          setError(err instanceof Error ? err.message : 'Failed to load wallet');
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
      <h1>Wallet</h1>
      <p className="page-hint">Your coin balance and transaction history.</p>

      {loading && <p className="status-loading">Loading wallet…</p>}
      {error && <p className="status-error">{error}</p>}

      {!loading && !error && wallet && (
        <>
          <article className="card wallet-balance">
            <h2>Current balance</h2>
            <p className="dashboard-stat">{wallet.balance} coins</p>
          </article>

          <h2 className="section-title">Transactions</h2>
          {transactions.length === 0 ? (
            <p className="empty-state">No transactions yet.</p>
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
    </section>
  );
}
