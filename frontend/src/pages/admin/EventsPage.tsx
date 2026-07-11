import { useCallback, useEffect, useState } from 'react';
import { createEvent, fetchAdminEvents, updateEventStatus } from '../../api/admin/events';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { eventStatus, formatEnum, gameEventType, translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { GameEvent } from '../../types';

const eventTypes: GameEvent['type'][] = [
  'ANNOUNCEMENT',
  'BONUS_PERIOD',
  'LOCATION_REVEAL',
  'LEADERBOARD_FREEZE',
  'AUCTION',
  'CUSTOM',
];

const eventStatuses: GameEvent['status'][] = [
  'SCHEDULED',
  'RUNNING',
  'PAUSED',
  'COMPLETED',
  'CANCELLED',
];

export function AdminEventsPage() {
  const [events, setEvents] = useState<GameEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [type, setType] = useState<GameEvent['type']>('BONUS_PERIOD');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminEvents();
      setEvents(data);
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить события',
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
      await createEvent({ type, title, description });
      showToast('Событие создано');
      setTitle('');
      setDescription('');
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать событие',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (eventId: string, status: GameEvent['status']) => {
    try {
      await updateEventStatus(eventId, { status });
      showToast(`Статус события → ${formatEnum(status, eventStatus)}`);
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось обновить статус',
        'error',
      );
    }
  };

  return (
    <section>
      <PageHeader title="События" description="Создание игровых событий и изменение их статуса." />

      <FormCard title="Создать событие">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Тип</span>
            <select
              className="form-select"
              value={type}
              onChange={(event) => setType(event.target.value as GameEvent['type'])}
            >
              {eventTypes.map((value) => (
                <option key={value} value={value}>
                  {formatEnum(value, gameEventType)}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Название</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Описание</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} required />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать событие
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Список событий</h2>
      <PageState loading={loading} error={error} empty={events.length === 0}>
        <DataTable
          rows={events}
          rowKey={(row) => row.id}
          columns={[
            { key: 'title', header: 'Название', render: (row) => row.title },
            { key: 'type', header: 'Тип', render: (row) => formatEnum(row.type, gameEventType) },
            {
              key: 'status',
              header: 'Статус',
              render: (row) => formatEnum(row.status, eventStatus),
            },
            {
              key: 'actions',
              header: 'Изменить статус',
              render: (row) => (
                <select
                  className="form-select form-select--inline"
                  value={row.status}
                  onChange={(event) =>
                    void handleStatusChange(row.id, event.target.value as GameEvent['status'])
                  }
                >
                  {eventStatuses.map((value) => (
                    <option key={value} value={value}>
                      {formatEnum(value, eventStatus)}
                    </option>
                  ))}
                </select>
              ),
            },
          ]}
        />
      </PageState>
    </section>
  );
}
