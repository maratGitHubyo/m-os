import { useCallback, useEffect, useState } from 'react';
import {
  createVictoryCondition,
  fetchVictoryAdminStatus,
} from '../../api/admin/victory';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { showToast } from '../../stores/toastStore';
import type { VictoryCondition } from '../../types';
import type { VictoryAdminStatusResponse } from '../../types/admin';

const victoryTypes: VictoryCondition['type'][] = [
  'COLLECT_ALL_NUMBERS',
  'COLLECT_UNIQUE_ITEMS',
  'REACH_SCORE',
  'FIND_ALL_LOCATIONS',
  'CUSTOM',
];

function progressLabel(condition: VictoryCondition): string {
  if (condition.progressCurrent != null && condition.progressTarget != null) {
    return `${condition.progressCurrent} / ${condition.progressTarget}`;
  }
  return condition.achieved ? 'Achieved' : '—';
}

export function AdminVictoryPage() {
  const [status, setStatus] = useState<VictoryAdminStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [type, setType] = useState<VictoryCondition['type']>('REACH_SCORE');
  const [description, setDescription] = useState('');
  const [targetJson, setTargetJson] = useState('{"category":"TOTAL","threshold":100}');
  const [active, setActive] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchVictoryAdminStatus();
      setStatus(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load victory status');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const targetValue = JSON.parse(targetJson) as Record<string, unknown>;
      await createVictoryCondition({
        type,
        description,
        targetValue,
        active,
      });
      showToast('Victory condition created');
      await load();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create condition', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const conditions = status?.conditions ?? [];

  return (
    <section>
      <PageHeader title="Victory" description="Create victory conditions and monitor progress." />

      <FormCard title="Create victory condition">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Type</span>
            <select
              className="form-select"
              value={type}
              onChange={(event) => setType(event.target.value as VictoryCondition['type'])}
            >
              {victoryTypes.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field form-field--wide">
            <span>Description</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Target value (JSON)</span>
            <input value={targetJson} onChange={(event) => setTargetJson(event.target.value)} />
          </label>
          <label className="form-field form-field--checkbox">
            <input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} />
            <span>Active</span>
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Create condition
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Current progress</h2>
      <PageState loading={loading} error={error} empty={conditions.length === 0}>
        {status && (
          <p className="page-hint">
            Any achieved: {status.anyAchieved ? 'Yes' : 'No'} · Session finished:{' '}
            {status.sessionFinished ? 'Yes' : 'No'}
          </p>
        )}
        <DataTable
          rows={conditions}
          rowKey={(row) => row.id}
          columns={[
            { key: 'type', header: 'Type', render: (row) => row.type },
            { key: 'description', header: 'Description', render: (row) => row.description },
            { key: 'progress', header: 'Progress', render: (row) => progressLabel(row) },
            { key: 'achieved', header: 'Achieved', render: (row) => (row.achieved ? 'Yes' : 'No') },
          ]}
        />
      </PageState>
    </section>
  );
}
