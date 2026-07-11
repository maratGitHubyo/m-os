import { useState } from 'react';
import { createNumber, grantNumber } from '../../api/admin/numbers';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { CollectibleNumberInfo } from '../../types/admin';

export function NumbersPage() {
  const { players } = useAdminPlayers();
  const [numbers, setNumbers] = useState<CollectibleNumberInfo[]>([]);
  const [numberValue, setNumberValue] = useState('1');
  const [grantNumberId, setGrantNumberId] = useState('');
  const [grantUserId, setGrantUserId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const created = await createNumber({ numberValue: Number(numberValue) });
      setNumbers((current) => [...current, created]);
      showToast(`Число ${created.numberValue} создано`);
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать число',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleGrant = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!grantNumberId || !grantUserId) {
      showToast('Выберите число и игрока', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await grantNumber(grantNumberId, grantUserId);
      showToast('Число выдано');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выдать число',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Числа"
        description="Создание коллекционных чисел и выдача игрокам. Список показывает числа, созданные в этой сессии (нет API списка)."
      />

      <FormCard title="Создать число">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Значение числа</span>
            <input
              type="number"
              min={1}
              value={numberValue}
              onChange={(event) => setNumberValue(event.target.value)}
              required
            />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать число
          </button>
        </form>
      </FormCard>

      <FormCard title="Выдать число">
        <form className="admin-form-grid" onSubmit={(event) => void handleGrant(event)}>
          <label className="form-field">
            <span>Число</span>
            <select
              className="form-select"
              value={grantNumberId}
              onChange={(event) => setGrantNumberId(event.target.value)}
              required
            >
              <option value="">Выберите число…</option>
              {numbers.map((number) => (
                <option key={number.id} value={number.id}>
                  #{number.numberValue}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Игрок</span>
            <PlayerSelect players={players} value={grantUserId} onChange={setGrantUserId} required />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Выдать число
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Числа (сессия)</h2>
      {numbers.length === 0 ? (
        <p className="empty-state">В этой сессии пока нет чисел.</p>
      ) : (
        <DataTable
          rows={numbers}
          rowKey={(row) => row.id}
          columns={[
            { key: 'value', header: 'Значение', render: (row) => row.numberValue },
            { key: 'id', header: 'ID', render: (row) => <span className="mono">{row.id}</span> },
            {
              key: 'created',
              header: 'Создано',
              render: (row) => new Date(row.createdAt).toLocaleString(),
            },
          ]}
        />
      )}
    </section>
  );
}
