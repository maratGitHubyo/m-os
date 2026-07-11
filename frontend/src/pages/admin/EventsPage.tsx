import { useCallback, useEffect, useState } from 'react';
import { createEvent, fetchAdminEvents, updateEventStatus } from '../../api/admin/events';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
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
      setError(err instanceof Error ? err.message : 'Failed to load events');
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
      showToast('Event created');
      setTitle('');
      setDescription('');
      await load();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create event', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusChange = async (eventId: string, status: GameEvent['status']) => {
    try {
      await updateEventStatus(eventId, { status });
      showToast(`Event status → ${status}`);
      await load();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Status update failed', 'error');
    }
  };

  return (
    <section>
      <PageHeader title="Events" description="Create game events and update their status." />

      <FormCard title="Create event">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Type</span>
            <select
              className="form-select"
              value={type}
              onChange={(event) => setType(event.target.value as GameEvent['type'])}
            >
              {eventTypes.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Title</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Description</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} required />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Create event
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Event list</h2>
      <PageState loading={loading} error={error} empty={events.length === 0}>
        <DataTable
          rows={events}
          rowKey={(row) => row.id}
          columns={[
            { key: 'title', header: 'Title', render: (row) => row.title },
            { key: 'type', header: 'Type', render: (row) => row.type },
            { key: 'status', header: 'Status', render: (row) => row.status },
            {
              key: 'actions',
              header: 'Change status',
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
                      {value}
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
