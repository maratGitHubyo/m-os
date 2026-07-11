import { useEffect, useState } from 'react';
import { fetchAdminDashboard } from '../../api/admin/dashboard';
import { DataTable } from '../../components/admin/DataTable';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import type { AdminDashboardResponse } from '../../types/admin';

export function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchAdminDashboard();
        if (!cancelled) {
          setData(response);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard');
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

  const top10 = data?.leaderboard.slice(0, 10) ?? [];

  return (
    <section>
      <PageHeader
        title="Admin Dashboard"
        description="Overview of the current game session."
      />

      <PageState loading={loading} error={error} empty={!data} emptyMessage="No dashboard data.">
        {data && (
          <>
            <div className="admin-stats-grid">
              <article className="card">
                <h2>Session</h2>
                <p className="dashboard-stat">{data.currentSession.name}</p>
                <span className={`badge badge--${data.currentSession.status?.toLowerCase() ?? 'starting'}`}>
                  {data.currentSession.status}
                </span>
              </article>
              <article className="card">
                <h2>Players</h2>
                <p className="dashboard-stat">{data.playersCount}</p>
              </article>
              <article className="card">
                <h2>Items</h2>
                <p className="dashboard-stat">{data.itemsCount}</p>
              </article>
              <article className="card">
                <h2>QR Codes</h2>
                <p className="dashboard-stat">{data.qrCount}</p>
              </article>
              <article className="card">
                <h2>Locations</h2>
                <p className="dashboard-stat">{data.locationsCount}</p>
              </article>
              <article className="card">
                <h2>Active Quests</h2>
                <p className="dashboard-stat">{data.activeQuests}</p>
              </article>
            </div>

            <h2 className="section-title">Top 10 Leaderboard</h2>
            {top10.length === 0 ? (
              <p className="empty-state">Leaderboard is empty.</p>
            ) : (
              <DataTable
                rows={top10}
                rowKey={(row) => row.userId}
                columns={[
                  { key: 'rank', header: 'Place', render: (row) => `#${row.rank}` },
                  { key: 'nickname', header: 'Player', render: (row) => row.nickname },
                  { key: 'points', header: 'Points', render: (row) => row.points },
                ]}
              />
            )}
          </>
        )}
      </PageState>
    </section>
  );
}
