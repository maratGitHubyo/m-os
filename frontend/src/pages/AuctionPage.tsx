import { useCallback, useEffect, useState } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_URL } from '../api/client';
import { fetchAuctionState, placeBid } from '../api/auction';
import { fetchMyWallet } from '../api/wallet';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { PageHeader } from '../components/ui/PageHeader';
import { PageState } from '../components/ui/PageState';
import { auctionLotStatus, formatEnum, translateError } from '../i18n/ru';
import { getToken, subscribe, useAuth } from '../stores/authStore';
import { showToast } from '../stores/toastStore';
import type { AuctionBroadcast, AuctionLot, AuctionState, Wallet } from '../types';

export function AuctionPage() {
  const { session, user } = useAuth();
  const [state, setState] = useState<AuctionState | null>(null);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [customAmount, setCustomAmount] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [auctionState, walletData] = await Promise.all([fetchAuctionState(), fetchMyWallet()]);
      setState(auctionState);
      setWallet(walletData);
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить аукцион',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!session?.id) {
      return;
    }

    let client: Client | null = null;
    let cancelled = false;

    const connect = (token: string | null) => {
      if (!token || cancelled) {
        return;
      }

      client?.deactivate();
      client = new Client({
        webSocketFactory: () => new SockJS(`${API_URL}/ws`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        onConnect: () => {
          client?.subscribe(`/topic/session/${session.id}/auction`, (message: IMessage) => {
            try {
              const payload = JSON.parse(message.body) as AuctionBroadcast;
              if (payload.type === 'MODE' && !payload.auctionModeEnabled) {
                setError('Аукцион закрыт');
                setState(null);
                return;
              }
              void load();
            } catch {
              // ignore malformed messages
            }
          });
        },
      });
      client.activate();
    };

    connect(getToken());
    const unsubscribe = subscribe(() => connect(getToken()));

    return () => {
      cancelled = true;
      unsubscribe();
      client?.deactivate();
    };
  }, [session?.id, load]);

  const openLot = state?.openLot ?? null;

  const bid = async (amount: number) => {
    if (!openLot) {
      return;
    }

    setSubmitting(true);
    try {
      const updated = await placeBid(openLot.id, amount);
      setState((current) =>
        current
          ? {
              ...current,
              openLot: updated,
              lots: current.lots,
            }
          : current,
      );
      const walletData = await fetchMyWallet();
      setWallet(walletData);
      showToast(`Ставка ${amount} принята`);
      setCustomAmount('');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось сделать ставку',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCustomBid = async (event: React.FormEvent) => {
    event.preventDefault();
    const amount = Number(customAmount);
    if (!amount || amount < 1) {
      showToast('Укажите сумму ставки', 'error');
      return;
    }
    await bid(amount);
  };

  const quickAmounts = (lot: AuctionLot): number[] => {
    const min = lot.nextMinBid ?? lot.startingPrice;
    const step = lot.minBidIncrement;
    return [min, min + step, min + step * 2];
  };

  return (
    <section className="auction-page">
      <PageHeader
        title="Аукцион"
        hint="Ставки списываются только после «Продано». Переводы на время аукциона закрыты."
      />

      <PageState loading={loading} error={error} empty={false} skeleton="stat">
        {wallet && (
          <Card className="wallet-balance">
            <p className="stat-card__label">Ваш баланс</p>
            <p className="dashboard-stat">{wallet.balance}</p>
            <p className="page-hint">M-Coins</p>
          </Card>
        )}

        {openLot ? (
          <Card>
            <h2>{openLot.title}</h2>
            <dl className="data-list">
              <div>
                <dt>Текущая цена</dt>
                <dd>
                  {openLot.currentPrice != null
                    ? `${openLot.currentPrice} М-коинов`
                    : `от ${openLot.startingPrice}`}
                </dd>
              </div>
              <div>
                <dt>Лидер</dt>
                <dd>
                  {openLot.currentLeaderNickname ?? 'Ставок пока нет'}
                  {openLot.currentLeaderId === user?.id ? ' (вы)' : ''}
                </dd>
              </div>
              <div>
                <dt>Минимальная ставка</dt>
                <dd>{openLot.nextMinBid ?? openLot.startingPrice}</dd>
              </div>
            </dl>

            <div className="admin-actions">
              {quickAmounts(openLot).map((amount) => (
                <Button
                  key={amount}
                  disabled={submitting || (wallet != null && wallet.balance < amount)}
                  onClick={() => void bid(amount)}
                >
                  {amount}
                </Button>
              ))}
            </div>

            <form className="inline-form" onSubmit={(event) => void handleCustomBid(event)}>
              <label className="form-field">
                <span>Своя ставка</span>
                <input
                  type="number"
                  min={openLot.nextMinBid ?? 1}
                  value={customAmount}
                  onChange={(event) => setCustomAmount(event.target.value)}
                />
              </label>
              <Button type="submit" variant="secondary" disabled={submitting}>
                Поставить
              </Button>
            </form>

            {openLot.recentBids.length > 0 && (
              <>
                <h3 className="section-title">Последние ставки</h3>
                <ul className="simple-list">
                  {openLot.recentBids.map((bidRow) => (
                    <li key={bidRow.id}>
                      {bidRow.bidderNickname ?? 'Игрок'}: {bidRow.amount}
                    </li>
                  ))}
                </ul>
              </>
            )}
          </Card>
        ) : (
          <p className="empty-state">Сейчас нет открытого лота. Ждите, пока ведущий начнёт торги.</p>
        )}

        {state && state.lots.length > 0 && (
          <>
            <h2 className="section-title">Недавние лоты</h2>
            <ul className="simple-list">
              {state.lots.map((lot) => (
                <li key={lot.id}>
                  {lot.title} — {formatEnum(lot.status, auctionLotStatus)}
                  {lot.finalPrice != null
                    ? ` · ${lot.finalPrice} (${lot.winnerNickname ?? 'победитель'})`
                    : ''}
                </li>
              ))}
            </ul>
          </>
        )}
      </PageState>
    </section>
  );
}
