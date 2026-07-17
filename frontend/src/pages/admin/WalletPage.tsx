import { useState } from 'react';
import { creditAllWallets, creditWallet, debitWallet } from '../../api/admin/wallet';
import { ConfirmDialog } from '../../components/admin/ConfirmDialog';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { DataTable } from '../../components/admin/DataTable';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';

export function AdminWalletPage() {
  const { players, loading, error, reload } = useAdminPlayers();
  const [userId, setUserId] = useState('');
  const [amount, setAmount] = useState('10');
  const [description, setDescription] = useState('Корректировка администратора');
  const [submitting, setSubmitting] = useState(false);

  const [bulkAmount, setBulkAmount] = useState('50');
  const [bulkDescription, setBulkDescription] = useState('Начисление всем игрокам');
  const [bulkSubmitting, setBulkSubmitting] = useState(false);
  const [pendingBulkAmount, setPendingBulkAmount] = useState<number | null>(null);

  const handleOperation = async (type: 'credit' | 'debit') => {
    if (!userId) {
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
      const request = { amount: parsedAmount, description };
      if (type === 'credit') {
        await creditWallet(userId, request);
        showToast('М-коины начислены');
      } else {
        await debitWallet(userId, request);
        showToast('М-коины списаны');
      }
      await reload();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Ошибка операции с кошельком',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const requestCreditAll = () => {
    const parsedAmount = Number(bulkAmount);
    if (!Number.isInteger(parsedAmount) || parsedAmount < 1) {
      showToast('Сумма должна быть целым числом не менее 1', 'error');
      return;
    }
    setPendingBulkAmount(parsedAmount);
  };

  const handleCreditAll = async () => {
    if (pendingBulkAmount == null) {
      return;
    }

    setBulkSubmitting(true);
    try {
      const result = await creditAllWallets({
        amount: pendingBulkAmount,
        description: bulkDescription || 'Начисление всем игрокам',
      });
      showToast(
        `Начислено ${result.amountPerPlayer} × ${result.playerCount} = ${result.totalCredited} M`,
      );
      setPendingBulkAmount(null);
      await reload();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось начислить всем',
        'error',
      );
    } finally {
      setBulkSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Кошелёк"
        description="Начисление и списание М-коинов для игроков сессии."
      />

      <PageState loading={loading} error={error} empty={players.length === 0}>
        <FormCard title="Начислить всем игрокам">
          <div className="admin-form-grid">
            <label className="form-field">
              <span>Сумма каждому</span>
              <input
                type="number"
                min={1}
                value={bulkAmount}
                onChange={(event) => setBulkAmount(event.target.value)}
              />
            </label>
            <label className="form-field form-field--wide">
              <span>Описание</span>
              <input
                type="text"
                value={bulkDescription}
                onChange={(event) => setBulkDescription(event.target.value)}
              />
            </label>
          </div>
          <div className="admin-actions">
            <button
              type="button"
              className="btn btn--primary"
              disabled={bulkSubmitting}
              onClick={requestCreditAll}
            >
              {bulkSubmitting ? 'Начисляем…' : 'Начислить всем'}
            </button>
          </div>
        </FormCard>

        <FormCard title="Операция с кошельком">
          <div className="admin-form-grid">
            <label className="form-field">
              <span>Игрок</span>
              <PlayerSelect players={players} value={userId} onChange={setUserId} required />
            </label>
            <label className="form-field">
              <span>Сумма</span>
              <input
                type="number"
                min={1}
                value={amount}
                onChange={(event) => setAmount(event.target.value)}
              />
            </label>
            <label className="form-field form-field--wide">
              <span>Описание</span>
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
              Начислить
            </button>
            <button
              type="button"
              className="btn btn--danger"
              disabled={submitting}
              onClick={() => void handleOperation('debit')}
            >
              Списать
            </button>
          </div>
        </FormCard>

        <h2 className="section-title">Игроки (рейтинг)</h2>
        <DataTable
          rows={players}
          rowKey={(row) => row.userId}
          columns={[
            { key: 'nickname', header: 'Игрок', render: (row) => row.nickname },
            { key: 'userId', header: 'ID пользователя', render: (row) => <span className="mono">{row.userId}</span> },
            { key: 'balance', header: 'M-Coins', render: (row) => row.balance },
          ]}
        />
      </PageState>

      <ConfirmDialog
        open={pendingBulkAmount !== null}
        title="Начислить всем"
        message={
          pendingBulkAmount != null
            ? `Начислить ${pendingBulkAmount} M-коинов каждому из ${players.length} игроков?`
            : ''
        }
        confirmLabel={bulkSubmitting ? 'Начисляем…' : 'Начислить'}
        onCancel={() => {
          if (!bulkSubmitting) {
            setPendingBulkAmount(null);
          }
        }}
        onConfirm={() => void handleCreditAll()}
      />
    </section>
  );
}
