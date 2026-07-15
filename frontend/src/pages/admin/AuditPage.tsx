import { useCallback, useEffect, useState } from 'react';
import { fetchAuditLogs } from '../../api/admin/audit';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { translateError } from '../../i18n/ru';
import type { AuditAction, AuditLogEntry } from '../../types/admin';

const auditActions: AuditAction[] = [
  'COIN_CREDIT',
  'COIN_DEBIT',
  'COIN_TRANSFER',
  'ITEM_GRANT',
  'ITEM_TRANSFER',
  'QR_SCAN',
  'TRADE_COMPLETE',
  'QUEST_COMPLETE',
  'SECRET_REDEEM',
  'ADMIN_ACTION',
  'LOCATION_DISCOVER',
  'EVENT_CREATE',
  'EVENT_STATUS_CHANGE',
  'NUMBER_GRANT',
  'QUEST_CREATE',
  'QUEST_START',
  'QUEST_PROGRESS',
  'QUEST_CLOSE',
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
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить журнал аудита',
      );
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
      <PageHeader title="Журнал аудита" description="Просмотр журнала сессии с фильтрами." />

      <FormCard title="Фильтры">
        <div className="admin-form-grid">
          <label className="form-field">
            <span>Пользователь</span>
            <PlayerSelect players={players} value={userId} onChange={setUserId} />
          </label>
          <label className="form-field">
            <span>Действие</span>
            <select
              className="form-select"
              value={action}
              onChange={(event) => setAction(event.target.value as AuditAction | '')}
            >
              <option value="">Все действия</option>
              {auditActions.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field">
            <span>Размер страницы</span>
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
            Применить фильтры
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
              header: 'Время',
              render: (row) => new Date(row.createdAt).toLocaleString(),
            },
            {
              key: 'user',
              header: 'Пользователь',
              render: (row) => playerNickname(row.userId),
            },
            { key: 'action', header: 'Действие', render: (row) => row.action },
            { key: 'entity', header: 'Сущность', render: (row) => `${row.entityType} #${row.entityId}` },
            { key: 'description', header: 'Описание', render: (row) => row.description },
          ]}
        />

        <div className="pagination">
          <button
            type="button"
            className="btn btn--secondary btn--small"
            disabled={page <= 0}
            onClick={() => setPage((current) => current - 1)}
          >
            Назад
          </button>
          <span>
            Страница {page + 1} из {Math.max(totalPages, 1)}
          </span>
          <button
            type="button"
            className="btn btn--secondary btn--small"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((current) => current + 1)}
          >
            Вперёд
          </button>
        </div>
      </PageState>
    </section>
  );
}
