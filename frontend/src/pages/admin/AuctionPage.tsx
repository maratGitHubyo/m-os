import { useCallback, useEffect, useState } from 'react';
import {
  cancelAuctionLot,
  createAuctionLot,
  fetchAdminAuctionState,
  openAuctionLot,
  sellAuctionLot,
  setAuctionMode,
} from '../../api/admin/auction';
import { ConfirmDialog } from '../../components/admin/ConfirmDialog';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { auctionLotStatus, formatEnum, translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { AuctionLot, AuctionState } from '../../types';

type LotAction = 'open' | 'sell' | 'cancel';

export function AdminAuctionPage() {
  const [state, setState] = useState<AuctionState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [title, setTitle] = useState('');
  const [startingPrice, setStartingPrice] = useState('50');
  const [minBidIncrement, setMinBidIncrement] = useState('50');
  const [pending, setPending] = useState<{ action: LotAction; lot: AuctionLot } | null>(null);
  const [acting, setActing] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setState(await fetchAdminAuctionState());
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

  const toggleMode = async () => {
    if (!state) {
      return;
    }
    setSubmitting(true);
    try {
      const updated = await setAuctionMode(!state.auctionModeEnabled);
      setState(updated);
      showToast(updated.auctionModeEnabled ? 'Аукцион открыт для гостей' : 'Аукцион скрыт');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось переключить режим',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await createAuctionLot({
        title: title.trim(),
        startingPrice: Number(startingPrice) || 50,
        minBidIncrement: Number(minBidIncrement) || 50,
      });
      showToast('Лот создан');
      setTitle('');
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать лот',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const executeLotAction = async () => {
    if (!pending) {
      return;
    }
    setActing(true);
    try {
      if (pending.action === 'open') {
        await openAuctionLot(pending.lot.id);
        showToast('Торги открыты');
      } else if (pending.action === 'sell') {
        await sellAuctionLot(pending.lot.id);
        showToast('Продано — монеты списаны');
      } else {
        await cancelAuctionLot(pending.lot.id);
        showToast('Лот снят');
      }
      setPending(null);
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выполнить действие',
        'error',
      );
    } finally {
      setActing(false);
    }
  };

  const modeEnabled = state?.auctionModeEnabled ?? false;

  return (
    <section>
      <PageHeader
        title="Аукцион"
        description="Откройте режим, создайте лоты по названию и ведите торги."
      />

      <PageState loading={loading} error={error} empty={!state}>
        {state && (
          <>
            <article className="card">
              <h2>Режим аукциона</h2>
              <p>
                Сейчас:{' '}
                <strong>{modeEnabled ? 'открыт для гостей' : 'скрыт'}</strong>
                {modeEnabled
                  ? ' · переводы и обмены заморожены'
                  : ' · гости не видят раздел «Аукцион»'}
              </p>
              <button
                type="button"
                className={modeEnabled ? 'btn btn--danger' : 'btn btn--primary'}
                disabled={submitting}
                onClick={() => void toggleMode()}
              >
                {modeEnabled ? 'Закрыть аукцион' : 'Открыть аукцион'}
              </button>
            </article>

            {state.openLot && (
              <article className="card">
                <h2>Активный лот: {state.openLot.title}</h2>
                <p>
                  Цена:{' '}
                  {state.openLot.currentPrice != null
                    ? `${state.openLot.currentPrice} (${state.openLot.currentLeaderNickname ?? '—'})`
                    : `ожидает ставок от ${state.openLot.startingPrice}`}
                </p>
                <div className="admin-actions">
                  <button
                    type="button"
                    className="btn btn--primary"
                    onClick={() => setPending({ action: 'sell', lot: state.openLot! })}
                  >
                    Продано
                  </button>
                  <button
                    type="button"
                    className="btn btn--secondary"
                    onClick={() => setPending({ action: 'cancel', lot: state.openLot! })}
                  >
                    Снять лот
                  </button>
                </div>
              </article>
            )}

            <FormCard title="Новый лот">
              <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
                <label className="form-field form-field--wide">
                  <span>Название</span>
                  <input
                    value={title}
                    onChange={(event) => setTitle(event.target.value)}
                    required
                    maxLength={255}
                  />
                </label>
                <label className="form-field">
                  <span>Стартовая цена</span>
                  <input
                    type="number"
                    min={0}
                    value={startingPrice}
                    onChange={(event) => setStartingPrice(event.target.value)}
                  />
                </label>
                <label className="form-field">
                  <span>Шаг ставки</span>
                  <input
                    type="number"
                    min={1}
                    value={minBidIncrement}
                    onChange={(event) => setMinBidIncrement(event.target.value)}
                  />
                </label>
                <button type="submit" className="btn btn--primary" disabled={submitting}>
                  Создать лот
                </button>
              </form>
            </FormCard>

            <h2 className="section-title">Все лоты</h2>
            <DataTable
              rows={state.lots}
              rowKey={(row) => row.id}
              columns={[
                { key: 'title', header: 'Название', render: (row) => row.title },
                {
                  key: 'status',
                  header: 'Статус',
                  render: (row) => formatEnum(row.status, auctionLotStatus),
                },
                {
                  key: 'price',
                  header: 'Цена',
                  render: (row) =>
                    row.finalPrice ?? row.currentPrice ?? `от ${row.startingPrice}`,
                },
                {
                  key: 'leader',
                  header: 'Лидер / победитель',
                  render: (row) => row.winnerNickname ?? row.currentLeaderNickname ?? '—',
                },
                {
                  key: 'actions',
                  header: 'Действия',
                  render: (row) => (
                    <div className="admin-actions">
                      {row.status === 'DRAFT' && (
                        <button
                          type="button"
                          className="btn btn--secondary"
                          disabled={!modeEnabled}
                          onClick={() => setPending({ action: 'open', lot: row })}
                        >
                          Открыть
                        </button>
                      )}
                      {(row.status === 'DRAFT' || row.status === 'OPEN') && (
                        <button
                          type="button"
                          className="btn btn--danger"
                          onClick={() => setPending({ action: 'cancel', lot: row })}
                        >
                          Снять
                        </button>
                      )}
                    </div>
                  ),
                },
              ]}
            />
          </>
        )}
      </PageState>

      <ConfirmDialog
        open={pending !== null}
        title={
          pending?.action === 'open'
            ? 'Открыть торги'
            : pending?.action === 'sell'
              ? 'Продано'
              : 'Снять лот'
        }
        message={
          pending?.action === 'open'
            ? `Открыть торги по лоту «${pending.lot.title}»?`
            : pending?.action === 'sell'
              ? `Списать монеты у лидера и закрыть «${pending.lot.title}»?`
              : `Снять лот «${pending?.lot.title}» без списания?`
        }
        confirmLabel={acting ? 'Выполняется…' : 'Подтвердить'}
        onCancel={() => setPending(null)}
        onConfirm={() => void executeLotAction()}
      />
    </section>
  );
}
