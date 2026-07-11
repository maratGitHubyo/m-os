import { DataTable } from '../../components/admin/DataTable';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';

export function PlayersPage() {
  const { players, loading, error } = useAdminPlayers();

  return (
    <section>
      <PageHeader
        title="Игроки"
        description="Участники сессии из рейтинга. Логин, роль, активность и монеты требуют отдельного API списка."
      />

      <PageState
        loading={loading}
        error={error}
        empty={players.length === 0}
        emptyMessage="В рейтинге нет игроков."
      >
        <DataTable
          rows={players}
          rowKey={(row) => row.userId}
          columns={[
            { key: 'nickname', header: 'Никнейм', render: (row) => row.nickname },
            { key: 'username', header: 'Логин', render: () => '—' },
            { key: 'role', header: 'Роль', render: () => '—' },
            { key: 'active', header: 'Активен', render: () => '—' },
            { key: 'coins', header: 'М-коины', render: () => '—' },
            { key: 'score', header: 'Общий счёт', render: (row) => row.totalScore },
          ]}
        />
      </PageState>
    </section>
  );
}
