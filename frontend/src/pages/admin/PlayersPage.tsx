import { DataTable } from '../../components/admin/DataTable';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';

export function PlayersPage() {
  const { players, loading, error } = useAdminPlayers();

  return (
    <section>
      <PageHeader
        title="Players"
        description="Session participants from leaderboard. Username, role, active, and coins require a dedicated list API."
      />

      <PageState
        loading={loading}
        error={error}
        empty={players.length === 0}
        emptyMessage="No players on the leaderboard."
      >
        <DataTable
          rows={players}
          rowKey={(row) => row.userId}
          columns={[
            { key: 'nickname', header: 'Nickname', render: (row) => row.nickname },
            { key: 'username', header: 'Username', render: () => '—' },
            { key: 'role', header: 'Role', render: () => '—' },
            { key: 'active', header: 'Active', render: () => '—' },
            { key: 'coins', header: 'Coins', render: () => '—' },
            { key: 'score', header: 'Total score', render: (row) => row.totalScore },
          ]}
        />
      </PageState>
    </section>
  );
}
