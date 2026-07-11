import { useState } from 'react';
import { createNumber, grantNumber } from '../../api/admin/numbers';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
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
      showToast(`Number ${created.numberValue} created`);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create number', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleGrant = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!grantNumberId || !grantUserId) {
      showToast('Select number and player', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await grantNumber(grantNumberId, grantUserId);
      showToast('Number granted');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to grant number', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Numbers"
        description="Create collectible numbers and grant to players. List shows numbers created in this session (no list API)."
      />

      <FormCard title="Create number">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Number value</span>
            <input
              type="number"
              min={1}
              value={numberValue}
              onChange={(event) => setNumberValue(event.target.value)}
              required
            />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Create number
          </button>
        </form>
      </FormCard>

      <FormCard title="Grant number">
        <form className="admin-form-grid" onSubmit={(event) => void handleGrant(event)}>
          <label className="form-field">
            <span>Number</span>
            <select
              className="form-select"
              value={grantNumberId}
              onChange={(event) => setGrantNumberId(event.target.value)}
              required
            >
              <option value="">Select number…</option>
              {numbers.map((number) => (
                <option key={number.id} value={number.id}>
                  #{number.numberValue}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Player</span>
            <PlayerSelect players={players} value={grantUserId} onChange={setGrantUserId} required />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Grant number
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Numbers (session)</h2>
      {numbers.length === 0 ? (
        <p className="empty-state">No numbers in this session yet.</p>
      ) : (
        <DataTable
          rows={numbers}
          rowKey={(row) => row.id}
          columns={[
            { key: 'value', header: 'Value', render: (row) => row.numberValue },
            { key: 'id', header: 'ID', render: (row) => <span className="mono">{row.id}</span> },
            {
              key: 'created',
              header: 'Created',
              render: (row) => new Date(row.createdAt).toLocaleString(),
            },
          ]}
        />
      )}
    </section>
  );
}
