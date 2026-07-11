import { useEffect, useState } from 'react';
import { fetchCurrentSession } from '../../api/locations';
import { finishSession, pauseSession, startSession } from '../../api/admin/session';
import { ConfirmDialog } from '../../components/admin/ConfirmDialog';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
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
      setError(err instanceof Error ? err.message : 'Failed to load session');
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
      showToast(`Session ${pendingAction}ed successfully`);
      setPendingAction(null);
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Action failed', 'error');
    } finally {
      setActing(false);
    }
  };

  const actionLabels: Record<SessionAction, string> = {
    start: 'Start session',
    pause: 'Pause session',
    finish: 'Finish session',
  };

  return (
    <section>
      <PageHeader
        title="Session Control"
        description="Start, pause, or finish the current game session."
      />

      <PageState loading={loading} error={error} empty={!session}>
        {session && (
          <>
            <article className="card">
              <h2>{session.name}</h2>
              <dl className="data-list">
                <div>
                  <dt>Status</dt>
                  <dd>
                    <span className={`badge badge--${session.status?.toLowerCase() ?? 'starting'}`}>
                      {session.status}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>Date</dt>
                  <dd>{session.date ?? '—'}</dd>
                </div>
                <div>
                  <dt>Session ID</dt>
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
                Start
              </button>
              <button
                type="button"
                className="btn btn--secondary"
                disabled={acting}
                onClick={() => setPendingAction('pause')}
              >
                Pause
              </button>
              <button
                type="button"
                className="btn btn--danger"
                disabled={acting}
                onClick={() => setPendingAction('finish')}
              >
                Finish
              </button>
            </div>
          </>
        )}
      </PageState>

      <ConfirmDialog
        open={pendingAction !== null}
        title={pendingAction ? actionLabels[pendingAction] : ''}
        message={
          pendingAction
            ? `Are you sure you want to ${pendingAction} the game session?`
            : ''
        }
        confirmLabel={acting ? 'Working…' : 'Confirm'}
        onCancel={() => setPendingAction(null)}
        onConfirm={() => void executeAction()}
      />
    </section>
  );
}
