import { useCallback, useEffect, useMemo, useState } from 'react';
import { fetchInventory } from '../api/inventory';
import { fetchSessionPlayers } from '../api/players';
import {
  acceptTrade,
  cancelTrade,
  createTrade,
  declineTrade,
  fetchTrades,
} from '../api/trades';
import { PageState } from '../components/ui/PageState';
import { formatEnum, tradeStatus, translateError } from '../i18n/ru';
import { showToast } from '../stores/toastStore';
import { useAuth } from '../stores/authStore';
import type { Item, SessionPlayer, Trade } from '../types';

function tradeSummary(trade: Trade, nicknameById: Map<string, string>, currentUserId: string): string {
  const initiator = nicknameById.get(trade.initiatorId) ?? 'Игрок';
  const receiver = nicknameById.get(trade.receiverId) ?? 'Игрок';
  const role =
    trade.initiatorId === currentUserId
      ? `Вы → ${receiver}`
      : `${initiator} → Вы`;
  return role;
}

function tradeOffers(trade: Trade, nicknameById: Map<string, string>): string[] {
  const lines: string[] = [];

  for (const coin of trade.coins) {
    const name = nicknameById.get(coin.userId) ?? 'Игрок';
    lines.push(`${name}: ${coin.amount} М-коинов`);
  }

  for (const item of trade.items) {
    const owner = nicknameById.get(item.ownerId) ?? 'Игрок';
    lines.push(`${owner} — ${item.template.name}`);
  }

  return lines.length > 0 ? lines : ['Без предметов и монет'];
}

export function TradesPage() {
  const { user } = useAuth();
  const [trades, setTrades] = useState<Trade[]>([]);
  const [players, setPlayers] = useState<SessionPlayer[]>([]);
  const [inventory, setInventory] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);

  const [receiverId, setReceiverId] = useState('');
  const [initiatorCoins, setInitiatorCoins] = useState('0');
  const [selectedItemIds, setSelectedItemIds] = useState<string[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const [tradeList, playerList, items] = await Promise.all([
        fetchTrades(),
        fetchSessionPlayers(),
        fetchInventory(),
      ]);
      setTrades(tradeList);
      setPlayers(playerList.filter((player) => player.id !== user?.id));
      setInventory(items);
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить обмены',
      );
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    void load();
  }, [load]);

  const nicknameById = useMemo(
    () => new Map(players.concat(user ? [{ id: user.id, nickname: user.nickname }] : []).map((p) => [p.id, p.nickname])),
    [players, user],
  );

  const toggleItem = (itemId: string) => {
    setSelectedItemIds((current) =>
      current.includes(itemId) ? current.filter((id) => id !== itemId) : [...current, itemId],
    );
  };

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!receiverId) {
      showToast('Выберите получателя', 'error');
      return;
    }

    const coins = Number(initiatorCoins) || 0;
    if (coins < 0) {
      showToast('Сумма монет не может быть отрицательной', 'error');
      return;
    }

    if (coins === 0 && selectedItemIds.length === 0) {
      showToast('Укажите монеты или предметы для обмена', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await createTrade({
        receiverId,
        initiatorCoins: coins,
        initiatorItemIds: selectedItemIds,
        receiverItemIds: [],
        receiverCoins: 0,
      });
      showToast('Предложение обмена создано');
      setReceiverId('');
      setInitiatorCoins('0');
      setSelectedItemIds([]);
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать обмен',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const runAction = async (tradeId: string, action: 'accept' | 'decline' | 'cancel') => {
    setActionId(tradeId);
    try {
      if (action === 'accept') {
        await acceptTrade(tradeId);
        showToast('Обмен принят');
      } else if (action === 'decline') {
        await declineTrade(tradeId);
        showToast('Обмен отклонён');
      } else {
        await cancelTrade(tradeId);
        showToast('Обмен отменён');
      }
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выполнить действие',
        'error',
      );
    } finally {
      setActionId(null);
    }
  };

  const incoming = trades.filter(
    (trade) => trade.receiverId === user?.id && trade.status === 'PENDING',
  );
  const outgoing = trades.filter((trade) => trade.initiatorId === user?.id);

  return (
    <section className="trades-page">
      <h1>Обмены</h1>
      <p className="page-hint">
        Предлагайте обмен монет и предметов с другими игроками. Перевод монет без предметов — в
        разделе «Кошелёк».
      </p>

      <PageState loading={loading} error={error} loadingLabel="Загрузка обменов…">
        <>
          <article className="card player-action-card">
            <h2>Создать обмен</h2>
            <form className="player-form" onSubmit={handleCreate}>
              <label className="form-field">
                <span>Получатель</span>
                <select
                  value={receiverId}
                  onChange={(event) => setReceiverId(event.target.value)}
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
                <span>Ваши М-коины в обмене</span>
                <input
                  type="number"
                  min={0}
                  value={initiatorCoins}
                  onChange={(event) => setInitiatorCoins(event.target.value)}
                />
              </label>

              {inventory.length > 0 && (
                <fieldset className="trade-items-picker">
                  <legend>Ваши предметы</legend>
                  <ul className="checkbox-list">
                    {inventory.map((item) => (
                      <li key={item.id}>
                        <label>
                          <input
                            type="checkbox"
                            checked={selectedItemIds.includes(item.id)}
                            onChange={() => toggleItem(item.id)}
                          />
                          {item.template.name}
                        </label>
                      </li>
                    ))}
                  </ul>
                </fieldset>
              )}

              <button type="submit" className="btn btn--primary" disabled={submitting}>
                {submitting ? 'Создание…' : 'Предложить обмен'}
              </button>
            </form>
          </article>

          <h2 className="section-title">Входящие предложения</h2>
          {incoming.length === 0 ? (
            <p className="empty-state">Нет входящих предложений.</p>
          ) : (
            <ul className="trade-list">
              {incoming.map((trade) => (
                <li key={trade.id} className="trade-card">
                  <div className="trade-card__header">
                    <strong>{tradeSummary(trade, nicknameById, user!.id)}</strong>
                    <span className="badge">{formatEnum(trade.status, tradeStatus)}</span>
                  </div>
                  <ul className="simple-list">
                    {tradeOffers(trade, nicknameById).map((line) => (
                      <li key={line}>{line}</li>
                    ))}
                  </ul>
                  <div className="trade-card__actions">
                    <button
                      type="button"
                      className="btn btn--primary"
                      disabled={actionId === trade.id}
                      onClick={() => void runAction(trade.id, 'accept')}
                    >
                      Принять
                    </button>
                    <button
                      type="button"
                      className="btn btn--secondary"
                      disabled={actionId === trade.id}
                      onClick={() => void runAction(trade.id, 'decline')}
                    >
                      Отклонить
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}

          <h2 className="section-title">Мои предложения</h2>
          {outgoing.length === 0 ? (
            <p className="empty-state">Вы ещё не создавали обмены.</p>
          ) : (
            <ul className="trade-list">
              {outgoing.map((trade) => (
                <li key={trade.id} className="trade-card">
                  <div className="trade-card__header">
                    <strong>{tradeSummary(trade, nicknameById, user!.id)}</strong>
                    <span className="badge">{formatEnum(trade.status, tradeStatus)}</span>
                  </div>
                  <ul className="simple-list">
                    {tradeOffers(trade, nicknameById).map((line) => (
                      <li key={line}>{line}</li>
                    ))}
                  </ul>
                  {trade.status === 'PENDING' && (
                    <div className="trade-card__actions">
                      <button
                        type="button"
                        className="btn btn--secondary"
                        disabled={actionId === trade.id}
                        onClick={() => void runAction(trade.id, 'cancel')}
                      >
                        Отменить
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}

        </>
      </PageState>
    </section>
  );
}
