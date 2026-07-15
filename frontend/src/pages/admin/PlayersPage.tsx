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
        description="Участники сессии по балансу M-Coins."
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
            { key: 'rank', header: 'Место', render: (row) => `#${row.rank}` },
            { key: 'nickname', header: 'Никнейм', render: (row) => row.nickname },
            { key: 'balance', header: 'M-Coins', render: (row) => row.balance },
          ]}
        />
      </PageState>
    </section>
  );
}
