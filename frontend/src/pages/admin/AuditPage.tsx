import { useCallback, useEffect, useState } from 'react';
import { fetchAuditLogs } from '../../api/admin/audit';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import type { AuditAction, AuditLogEntry } from '../../types/admin';

const auditActions: AuditAction[] = [
  'COIN_CREDIT',
  'COIN_DEBIT',
  'ITEM_GRANT',
  'ITEM_TRANSFER',
  'QR_SCAN',
  'TRADE_COMPLETE',
  'QUEST_COMPLETE',
  'SECRET_REDEEM',
  'ADMIN_ACTION',
  'LOCATION_DISCOVER',
  'SCORE_AWARD',
  'SCORE_ADD',
  'SCORE_SUBTRACT',
  'EVENT_CREATE',
  'EVENT_STATUS_CHANGE',
  'VICTORY_ACHIEVED',
  'NUMBER_GRANT',
  'QUEST_CREATE',
  'QUEST_START',
  'QUEST_PROGRESS',
  'TRADE_CREATE',
  'TRADE_ACCEPT',
  'TRADE_DECLINE',
  'TRADE_CANCEL',
  'SESSION_START',
  'SESSION_PAUSE',
  'SESSION_FINISH',
];

export function AuditPage() {
  const { players } = useAdminPlayers();
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [userId, setUserId] = useState('');
  const [action, setAction] = useState<AuditAction | ''>('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchAuditLogs({
        userId: userId || undefined,
        action: action || undefined,
        page,
        size,
      });
      setLogs(response.content);
      setTotalPages(response.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  }, [userId, action, page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  const playerNickname = (id: string): string => {
    return players.find((player) => player.userId === id)?.nickname ?? id.slice(0, 8);
  };

  return (
    <section>
      <PageHeader title="Audit Log" description="Review session audit trail with filters." />

      <FormCard title="Filters">
        <div className="admin-form-grid">
          <label className="form-field">
            <span>User</span>
            <PlayerSelect players={players} value={userId} onChange={setUserId} />
          </label>
          <label className="form-field">
            <span>Action</span>
            <select
              className="form-select"
              value={action}
              onChange={(event) => setAction(event.target.value as AuditAction | '')}
            >
              <option value="">All actions</option>
              {auditActions.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Page size</span>
            <select
              className="form-select"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value));
              }}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </label>
          <button type="button" className="btn btn--secondary" onClick={() => void load()}>
            Apply filters
          </button>
        </div>
      </FormCard>

      <PageState loading={loading} error={error} empty={logs.length === 0}>
        <DataTable
          rows={logs}
          rowKey={(row) => row.id}
          columns={[
            {
              key: 'time',
              header: 'Time',
              render: (row) => new Date(row.createdAt).toLocaleString(),
            },
            {
              key: 'user',
              header: 'User',
              render: (row) => playerNickname(row.userId),
            },
            { key: 'action', header: 'Action', render: (row) => row.action },
            { key: 'entity', header: 'Entity', render: (row) => `${row.entityType} #${row.entityId}` },
            { key: 'description', header: 'Description', render: (row) => row.description },
          ]}
        />

        <div className="pagination">
          <button
            type="button"
            className="btn btn--secondary btn--small"
            disabled={page <= 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Previous
          </button>
          <span>
            Page {page + 1} of {Math.max(totalPages, 1)}
          </span>
          <button
            type="button"
            className="btn btn--secondary btn--small"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Next
          </button>
        </div>
      </PageState>
    </section>
  );
}
