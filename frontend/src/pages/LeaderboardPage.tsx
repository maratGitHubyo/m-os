import { useEffect, useState } from 'react';
import { fetchLeaderboard } from '../api/score';
import { PageState } from '../components/ui/PageState';
import { translateError } from '../i18n/ru';
import type { LeaderboardEntry } from '../types';

export function LeaderboardPage() {
  const [entries, setEntries] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchLeaderboard();
        if (!cancelled) {
          setEntries(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? translateError(err.message) : 'Не удалось загрузить рейтинг',
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
    <section className="leaderboard-page">
      <h1>Рейтинг</h1>
      <p className="page-hint">Лучшие игроки по общему счёту.</p>

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка рейтинга…"
        empty={entries.length === 0}
        emptyMessage="Рейтинг пуст."
      >
        <table className="leaderboard-table">
          <thead>
            <tr>
              <th>Место</th>
              <th>Игрок</th>
              <th>Очки</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr key={entry.userId}>
                <td>#{entry.rank}</td>
                <td>{entry.nickname}</td>
                <td>{entry.points}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </PageState>
    </section>
  );
}
