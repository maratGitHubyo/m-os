import { useEffect, useState } from 'react';
import { fetchEvents } from '../api/events';
import type { GameEvent } from '../types';

function formatType(type: GameEvent['type']): string {
  return type.replaceAll('_', ' ').toLowerCase();
}

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
          setError(err instanceof Error ? err.message : 'Failed to load events');
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
      <h1>Events</h1>
      <p className="page-hint">Scheduled and active game events. Live updates appear as notifications.</p>

      {loading && <p className="status-loading">Loading events…</p>}
      {error && <p className="status-error">{error}</p>}

      {!loading && !error && events.length === 0 && (
        <p className="empty-state">No events scheduled.</p>
      )}

      {!loading && !error && events.length > 0 && (
        <ul className="event-list">
          {events.map((event) => (
            <li key={event.id} className="event-card">
              <div className="event-card__header">
                <h2>{event.title}</h2>
                <span className={`badge badge--${event.status.toLowerCase()}`}>{event.status}</span>
              </div>
              <p className="event-card__type">{formatType(event.type)}</p>
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
      )}
    </section>
  );
}
