import { useEffect, useState } from 'react';
import { fetchEvents } from '../api/events';
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
      <h1>События</h1>
      <p className="page-hint">
        Запланированные и активные игровые события. Обновления приходят в уведомлениях.
      </p>

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка событий…"
        empty={events.length === 0}
        emptyMessage="Нет запланированных событий."
      >
        <ul className="event-list">
          {events.map((event) => (
            <li key={event.id} className="event-card">
              <div className="event-card__header">
                <h2>{event.title}</h2>
                <span className={`badge badge--${event.status.toLowerCase()}`}>
                  {formatEnum(event.status, eventStatus)}
                </span>
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
