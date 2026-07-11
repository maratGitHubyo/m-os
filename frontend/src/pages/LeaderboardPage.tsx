import { useEffect, useState } from 'react';
import { fetchLeaderboard } from '../api/score';
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
          setError(err instanceof Error ? err.message : 'Failed to load leaderboard');
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
      <h1>Leaderboard</h1>
      <p className="page-hint">Top players by total score.</p>

      {loading && <p className="status-loading">Loading leaderboard…</p>}
      {error && <p className="status-error">{error}</p>}

      {!loading && !error && entries.length === 0 && (
        <p className="empty-state">Leaderboard is empty.</p>
      )}

      {!loading && !error && entries.length > 0 && (
        <table className="leaderboard-table">
          <thead>
            <tr>
              <th>Place</th>
              <th>Player</th>
              <th>Points</th>
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
      )}
    </section>
  );
}
