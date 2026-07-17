import { useEffect, useState } from 'react';
import { fetchCollectionStats } from '../../api/admin/items';
import { DataTable } from '../../components/admin/DataTable';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { translateError } from '../../i18n/ru';
import type { ItemCollectionStatsResponse } from '../../types/admin';

export function CollectionStatsPage() {
  const [data, setData] = useState<ItemCollectionStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetchCollectionStats();
        if (!cancelled) {
          setData(response);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? translateError(err.message)
              : 'Не удалось загрузить статистику коллекции',
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
    <section>
      <PageHeader
        title="Статистика коллекции"
        description="Кто сколько предметов собрал по редкостям. Выше — у кого больше всего, при равенстве — у кого больше легендарных."
      />

      <PageState
        loading={loading}
        error={error}
        empty={!data}
        emptyMessage="Нет данных статистики."
      >
        {data && (
          <>
            <div className="admin-stats-grid">
              <article className="card">
                <h2>Собрано</h2>
                <p className="dashboard-stat">
                  {data.collected.total}/{data.catalog.total}
                </p>
                <p className="muted">ещё не найдено: {data.collected.unclaimed}</p>
              </article>
              <article className="card">
                <h2>Легендарные</h2>
                <p className="dashboard-stat">
                  {data.collected.legendary}/{data.catalog.legendary}
                </p>
              </article>
              <article className="card">
                <h2>Эпические</h2>
                <p className="dashboard-stat">
                  {data.collected.epic}/{data.catalog.epic}
                </p>
              </article>
              <article className="card">
                <h2>Редкие</h2>
                <p className="dashboard-stat">
                  {data.collected.rare}/{data.catalog.rare}
                </p>
              </article>
              <article className="card">
                <h2>Обычные</h2>
                <p className="dashboard-stat">
                  {data.collected.common}/{data.catalog.common}
                </p>
              </article>
            </div>

            <DataTable
              rows={data.players}
              rowKey={(row) => row.userId}
              columns={[
                { key: 'rank', header: 'Место', render: (row) => `#${row.rank}` },
                { key: 'nickname', header: 'Игрок', render: (row) => row.nickname },
                {
                  key: 'legendary',
                  header: 'Легендарные',
                  render: (row) => row.legendary,
                },
                { key: 'epic', header: 'Эпические', render: (row) => row.epic },
                { key: 'rare', header: 'Редкие', render: (row) => row.rare },
                { key: 'common', header: 'Обычные', render: (row) => row.common },
                { key: 'total', header: 'Всего', render: (row) => row.total },
              ]}
            />
          </>
        )}
      </PageState>
    </section>
  );
}
