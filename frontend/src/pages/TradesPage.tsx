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
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { PageHeader } from '../components/ui/PageHeader';
import { PageState } from '../components/ui/PageState';
import { formatEnum, tradeStatus, translateError } from '../i18n/ru';
import { showToast } from '../stores/toastStore';
import { useAuth } from '../stores/authStore';
import type { Item, SessionPlayer, Trade } from '../types';

type WizardStep = 1 | 2 | 3 | 4;

const wizardLabels = ['Игрок', 'Предметы', 'Монеты', 'Подтверждение'] as const;

function tradeSummary(trade: Trade, nicknameById: Map<string, string>, currentUserId: string): string {
  const initiator = nicknameById.get(trade.initiatorId) ?? 'Игрок';
  const receiver = nicknameById.get(trade.receiverId) ?? 'Игрок';
  return trade.initiatorId === currentUserId
    ? `Вы → ${receiver}`
    : `${initiator} → Вы`;
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

  const [step, setStep] = useState<WizardStep>(1);
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
    () =>
      new Map(
        players
          .concat(user ? [{ id: user.id, nickname: user.nickname }] : [])
          .map((p) => [p.id, p.nickname]),
      ),
    [players, user],
  );

  const selectedItems = inventory.filter((item) => selectedItemIds.includes(item.id));
  const receiverNickname = players.find((p) => p.id === receiverId)?.nickname ?? '—';
  const coinAmount = Number(initiatorCoins) || 0;

  const toggleItem = (itemId: string) => {
    setSelectedItemIds((current) =>
      current.includes(itemId) ? current.filter((id) => id !== itemId) : [...current, itemId],
    );
  };

  const resetWizard = () => {
    setStep(1);
    setReceiverId('');
    setInitiatorCoins('0');
    setSelectedItemIds([]);
  };

  const goNext = () => {
    if (step === 1 && !receiverId) {
      showToast('Выберите игрока', 'error');
      return;
    }
    if (step === 3 && coinAmount < 0) {
      showToast('Сумма монет не может быть отрицательной', 'error');
      return;
    }
    if (step < 4) {
      setStep((current) => (current + 1) as WizardStep);
    }
  };

  const goBack = () => {
    if (step > 1) {
      setStep((current) => (current - 1) as WizardStep);
    }
  };

  const handleCreate = async () => {
    if (!receiverId) {
      showToast('Выберите получателя', 'error');
      return;
    }

    if (coinAmount < 0) {
      showToast('Сумма монет не может быть отрицательной', 'error');
      return;
    }

    if (coinAmount === 0 && selectedItemIds.length === 0) {
      showToast('Укажите монеты или предметы для обмена', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await createTrade({
        receiverId,
        initiatorCoins: coinAmount,
        initiatorItemIds: selectedItemIds,
        receiverItemIds: [],
        receiverCoins: 0,
      });
      showToast('Предложение обмена создано');
      resetWizard();
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
      <PageHeader
        title="Обмены"
        hint="Предлагайте обмен монет и предметов. Чистый перевод монет — в «Кошельке»."
      />

      <PageState loading={loading} error={error} loadingLabel="Загрузка обменов…">
        <>
          <Card className="trade-wizard player-action-card">
            <h2>Новый обмен</h2>
            <ol className="trade-wizard__steps" aria-label="Шаги создания обмена">
              {wizardLabels.map((label, index) => {
                const stepNumber = (index + 1) as WizardStep;
                const stateClass =
                  step === stepNumber
                    ? 'trade-wizard__step trade-wizard__step--active'
                    : step > stepNumber
                      ? 'trade-wizard__step trade-wizard__step--done'
                      : 'trade-wizard__step';
                return (
                  <li key={label} className={stateClass}>
                    <span aria-hidden="true">{stepNumber}</span>
                    {label}
                  </li>
                );
              })}
            </ol>

            <div className="trade-wizard__panel">
              {step === 1 && (
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
              )}

              {step === 2 && (
                <>
                  {inventory.length === 0 ? (
                    <p className="page-hint">В инвентаре пока нет предметов — можно отдать только монеты.</p>
                  ) : (
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
                </>
              )}

              {step === 3 && (
                <label className="form-field">
                  <span>Ваши М-коины в обмене</span>
                  <input
                    type="number"
                    min={0}
                    value={initiatorCoins}
                    onChange={(event) => setInitiatorCoins(event.target.value)}
                  />
                </label>
              )}

              {step === 4 && (
                <dl className="trade-summary">
                  <div>
                    <dt>Получатель</dt>
                    <dd>{receiverNickname}</dd>
                  </div>
                  <div>
                    <dt>М-коины</dt>
                    <dd>{coinAmount}</dd>
                  </div>
                  <div>
                    <dt>Предметы</dt>
                    <dd>
                      {selectedItems.length === 0
                        ? 'Нет'
                        : selectedItems.map((item) => item.template.name).join(', ')}
                    </dd>
                  </div>
                </dl>
              )}

              <div className="trade-wizard__nav">
                {step > 1 && (
                  <Button variant="secondary" onClick={goBack} disabled={submitting}>
                    Назад
                  </Button>
                )}
                {step < 4 ? (
                  <Button onClick={goNext}>Далее</Button>
                ) : (
                  <Button disabled={submitting} onClick={() => void handleCreate()}>
                    {submitting ? 'Создание…' : 'Предложить обмен'}
                  </Button>
                )}
              </div>
            </div>
          </Card>

          <h2 className="section-title">Входящие предложения</h2>
          {incoming.length === 0 ? (
            <p className="empty-state">Нет входящих предложений.</p>
          ) : (
            <ul className="trade-list">
              {incoming.map((trade) => (
                <li key={trade.id} className="trade-card trade-card--pending">
                  <div className="trade-card__header">
                    <strong>{tradeSummary(trade, nicknameById, user!.id)}</strong>
                    <Badge tone="pending">{formatEnum(trade.status, tradeStatus)}</Badge>
                  </div>
                  <ul className="simple-list">
                    {tradeOffers(trade, nicknameById).map((line) => (
                      <li key={line}>{line}</li>
                    ))}
                  </ul>
                  <div className="trade-card__actions">
                    <Button
                      disabled={actionId === trade.id}
                      onClick={() => void runAction(trade.id, 'accept')}
                    >
                      Принять
                    </Button>
                    <Button
                      variant="secondary"
                      disabled={actionId === trade.id}
                      onClick={() => void runAction(trade.id, 'decline')}
                    >
                      Отклонить
                    </Button>
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
                <li
                  key={trade.id}
                  className={
                    trade.status === 'PENDING'
                      ? 'trade-card trade-card--pending'
                      : 'trade-card'
                  }
                >
                  <div className="trade-card__header">
                    <strong>{tradeSummary(trade, nicknameById, user!.id)}</strong>
                    <Badge tone={trade.status.toLowerCase()}>
                      {formatEnum(trade.status, tradeStatus)}
                    </Badge>
                  </div>
                  <ul className="simple-list">
                    {tradeOffers(trade, nicknameById).map((line) => (
                      <li key={line}>{line}</li>
                    ))}
                  </ul>
                  {trade.status === 'PENDING' && (
                    <div className="trade-card__actions">
                      <Button
                        variant="secondary"
                        disabled={actionId === trade.id}
                        onClick={() => void runAction(trade.id, 'cancel')}
                      >
                        Отменить
                      </Button>
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
