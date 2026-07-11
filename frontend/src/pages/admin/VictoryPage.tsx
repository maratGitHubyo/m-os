import { useCallback, useEffect, useState } from 'react';
import {
  createVictoryCondition,
  fetchVictoryAdminStatus,
} from '../../api/admin/victory';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { formatEnum, translateError, victoryType } from '../../i18n/ru';
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
  return condition.achieved ? 'Выполнено' : '—';
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
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить статус победы',
      );
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
      showToast('Условие победы создано');
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать условие',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const conditions = status?.conditions ?? [];

  return (
    <section>
      <PageHeader title="Победа" description="Создание условий победы и мониторинг прогресса." />

      <FormCard title="Создать условие победы">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Тип</span>
            <select
              className="form-select"
              value={type}
              onChange={(event) => setType(event.target.value as VictoryCondition['type'])}
            >
              {victoryTypes.map((value) => (
                <option key={value} value={value}>
                  {formatEnum(value, victoryType)}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field form-field--wide">
            <span>Описание</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Целевое значение (JSON)</span>
            <input value={targetJson} onChange={(event) => setTargetJson(event.target.value)} />
          </label>
          <label className="form-field form-field--checkbox">
            <input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} />
            <span>Активно</span>
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать условие
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Текущий прогресс</h2>
      <PageState loading={loading} error={error} empty={conditions.length === 0}>
        {status && (
          <p className="page-hint">
            Есть выполненные: {status.anyAchieved ? 'Да' : 'Нет'} · Сессия завершена:{' '}
            {status.sessionFinished ? 'Да' : 'Нет'}
          </p>
        )}
        <DataTable
          rows={conditions}
          rowKey={(row) => row.id}
          columns={[
            { key: 'type', header: 'Тип', render: (row) => formatEnum(row.type, victoryType) },
            { key: 'description', header: 'Описание', render: (row) => row.description },
            { key: 'progress', header: 'Прогресс', render: (row) => progressLabel(row) },
            { key: 'achieved', header: 'Выполнено', render: (row) => (row.achieved ? 'Да' : 'Нет') },
          ]}
        />
      </PageState>
    </section>
  );
}
