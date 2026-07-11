import { useEffect, useState } from 'react';
import { fetchCurrentSession } from '../../api/locations';
import { finishSession, pauseSession, startSession } from '../../api/admin/session';
import { ConfirmDialog } from '../../components/admin/ConfirmDialog';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { formatEnum, sessionStatus, translateError, ui } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { GameSessionInfo } from '../../types';

type SessionAction = 'start' | 'pause' | 'finish';

export function SessionPage() {
  const [session, setSession] = useState<GameSessionInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<SessionAction | null>(null);
  const [acting, setActing] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchCurrentSession();
      setSession(data);
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить сессию',
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const executeAction = async () => {
    if (!pendingAction) {
      return;
    }

    setActing(true);
    try {
      const updated =
        pendingAction === 'start'
          ? await startSession()
          : pendingAction === 'pause'
            ? await pauseSession()
            : await finishSession();
      setSession(updated);
      showToast(
        pendingAction === 'start'
          ? 'Сессия запущена'
          : pendingAction === 'pause'
            ? 'Сессия приостановлена'
            : 'Сессия завершена',
      );
      setPendingAction(null);
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выполнить действие',
        'error',
      );
    } finally {
      setActing(false);
    }
  };

  const actionLabels: Record<SessionAction, string> = {
    start: 'Запустить сессию',
    pause: 'Приостановить сессию',
    finish: 'Завершить сессию',
  };

  const actionMessages: Record<SessionAction, string> = {
    start: 'Вы уверены, что хотите запустить игровую сессию?',
    pause: 'Вы уверены, что хотите приостановить игровую сессию?',
    finish: 'Вы уверены, что хотите завершить игровую сессию?',
  };

  return (
    <section>
      <PageHeader
        title="Управление сессией"
        description="Запуск, пауза или завершение текущей игровой сессии."
      />

      <PageState loading={loading} error={error} empty={!session}>
        {session && (
          <>
            <article className="card">
              <h2>{session.name}</h2>
              <dl className="data-list">
                <div>
                  <dt>Статус</dt>
                  <dd>
                    <span className={`badge badge--${session.status?.toLowerCase() ?? 'starting'}`}>
                      {session.status ? formatEnum(session.status, sessionStatus) : '—'}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>Дата</dt>
                  <dd>{session.date ?? '—'}</dd>
                </div>
                <div>
                  <dt>ID сессии</dt>
                  <dd className="mono">{session.id}</dd>
                </div>
              </dl>
            </article>

            <div className="admin-actions">
              <button
                type="button"
                className="btn btn--primary"
                disabled={acting}
                onClick={() => setPendingAction('start')}
              >
                Запустить
              </button>
              <button
                type="button"
                className="btn btn--secondary"
                disabled={acting}
                onClick={() => setPendingAction('pause')}
              >
                Пауза
              </button>
              <button
                type="button"
                className="btn btn--danger"
                disabled={acting}
                onClick={() => setPendingAction('finish')}
              >
                Завершить
              </button>
            </div>
          </>
        )}
      </PageState>

      <ConfirmDialog
        open={pendingAction !== null}
        title={pendingAction ? actionLabels[pendingAction] : ''}
        message={pendingAction ? actionMessages[pendingAction] : ''}
        confirmLabel={acting ? 'Выполняется…' : ui.confirm}
        onCancel={() => setPendingAction(null)}
        onConfirm={() => void executeAction()}
      />
    </section>
  );
}
