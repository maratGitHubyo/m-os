import { useEffect, useState } from 'react';
import { fetchEvents } from '../api/events';
import { Badge } from '../components/ui/Badge';
import { PageHeader } from '../components/ui/PageHeader';
import { PageState } from '../components/ui/PageState';
import { eventStatus, formatEnum, gameEventType, translateError } from '../i18n/ru';
import type { GameEvent } from '../types';

export function EventsPage() {
  const [events, setEvents] = useState<GameEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchEvents();
        if (!cancelled) {
          setEvents(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? translateError(err.message) : 'Не удалось загрузить события',
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="events-page">
      <PageHeader
        title="События"
        hint="Лента игровых событий. Живые обновления приходят в уведомлениях."
      />

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка событий…"
        empty={events.length === 0}
        emptyTitle="Тишина на канале"
        emptyMessage="Нет запланированных событий."
      >
        <ul className="event-list">
          {events.map((event) => (
            <li key={event.id} className="event-card">
              <div className="event-card__header">
                <h2>{event.title}</h2>
                <Badge tone={event.status.toLowerCase()}>
                  {formatEnum(event.status, eventStatus)}
                </Badge>
              </div>
              <p className="event-card__type">{formatEnum(event.type, gameEventType)}</p>
              {event.description && <p>{event.description}</p>}
              {event.startAt && (
                <p className="event-card__meta">
                  {new Date(event.startAt).toLocaleString()}
                  {event.endAt ? ` — ${new Date(event.endAt).toLocaleString()}` : ''}
                </p>
              )}
            </li>
          ))}
        </ul>
      </PageState>
    </section>
  );
}
