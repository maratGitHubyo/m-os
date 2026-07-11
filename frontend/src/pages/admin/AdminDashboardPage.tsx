import { useEffect, useState } from 'react';
import { fetchAdminDashboard } from '../../api/admin/dashboard';
import { DataTable } from '../../components/admin/DataTable';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { formatEnum, sessionStatus, translateError } from '../../i18n/ru';
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
          setError(
            err instanceof Error ? translateError(err.message) : 'Не удалось загрузить панель',
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

  const top10 = data?.leaderboard.slice(0, 10) ?? [];

  return (
    <section>
      <PageHeader
        title="Панель администратора"
        description="Обзор текущей игровой сессии."
      />

      <PageState loading={loading} error={error} empty={!data} emptyMessage="Нет данных панели.">
        {data && (
          <>
            <div className="admin-stats-grid">
              <article className="card">
                <h2>Сессия</h2>
                <p className="dashboard-stat">{data.currentSession.name}</p>
                <span className={`badge badge--${data.currentSession.status?.toLowerCase() ?? 'starting'}`}>
                  {data.currentSession.status
                    ? formatEnum(data.currentSession.status, sessionStatus)
                    : '—'}
                </span>
              </article>
              <article className="card">
                <h2>Игроки</h2>
                <p className="dashboard-stat">{data.playersCount}</p>
              </article>
              <article className="card">
                <h2>Предметы</h2>
                <p className="dashboard-stat">{data.itemsCount}</p>
              </article>
              <article className="card">
                <h2>QR-коды</h2>
                <p className="dashboard-stat">{data.qrCount}</p>
              </article>
              <article className="card">
                <h2>Локации</h2>
                <p className="dashboard-stat">{data.locationsCount}</p>
              </article>
              <article className="card">
                <h2>Активные квесты</h2>
                <p className="dashboard-stat">{data.activeQuests}</p>
              </article>
            </div>

            <h2 className="section-title">Топ-10 рейтинга</h2>
            {top10.length === 0 ? (
              <p className="empty-state">Рейтинг пуст.</p>
            ) : (
              <DataTable
                rows={top10}
                rowKey={(row) => row.userId}
                columns={[
                  { key: 'rank', header: 'Место', render: (row) => `#${row.rank}` },
                  { key: 'nickname', header: 'Игрок', render: (row) => row.nickname },
                  { key: 'points', header: 'Очки', render: (row) => row.points },
                ]}
              />
            )}
          </>
        )}
      </PageState>
    </section>
  );
}
