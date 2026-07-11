import { useEffect, useState } from 'react';
import { fetchVictoryConditions } from '../api/victory';
import { PageState } from '../components/ui/PageState';
import { formatEnum, translateError, victoryType } from '../i18n/ru';
import type { VictoryCondition } from '../types';

function progressPercent(condition: VictoryCondition): number | null {
  if (
    condition.progressCurrent == null ||
    condition.progressTarget == null ||
    condition.progressTarget <= 0
  ) {
    return null;
  }
  return Math.min(
    100,
    Math.round((condition.progressCurrent / condition.progressTarget) * 100),
  );
}

export function VictoryPage() {
  const [conditions, setConditions] = useState<VictoryCondition[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchVictoryConditions();
        if (!cancelled) {
          setConditions(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? translateError(err.message)
              : 'Не удалось загрузить условия победы',
          );
          setConditions([]);
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

  const anyAchieved = conditions.some((condition) => condition.achieved);
  const myVictory = conditions.some((condition) => condition.achievedByMe);

  return (
    <section className="victory-page">
      <h1>Условия победы</h1>
      <p className="page-hint">
        Прогресс рассчитывается сервером. Выполните любое активное условие, чтобы победить.
      </p>

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка условий…"
        empty={conditions.length === 0}
        emptyMessage="Условия победы ещё не настроены."
      >
        <>
          <div className="victory-page__summary">
            {myVictory && <p className="status-ok">Вы выполнили условие победы!</p>}
            {!myVictory && anyAchieved && (
              <p className="victory-page__someone-won">Условие победы уже выполнено.</p>
            )}
            {!anyAchieved && <p className="victory-page__pending">Пока ни одно условие не выполнено.</p>}
          </div>

          <ul className="victory-list">
            {conditions.map((condition) => {
              const percent = progressPercent(condition);

              return (
                <li
                  key={condition.id}
                  className={`victory-card${condition.achieved ? ' victory-card--achieved' : ''}`}
                >
                  <div className="victory-card__header">
                    <span className="victory-card__type">
                      {formatEnum(condition.type, victoryType)}
                    </span>
                    <span
                      className={
                        condition.achieved
                          ? 'victory-card__badge victory-card__badge--done'
                          : 'victory-card__badge'
                      }
                    >
                      {condition.achieved ? 'Выполнено' : 'В процессе'}
                    </span>
                  </div>

                  <p className="victory-card__description">{condition.description}</p>

                  {!condition.achieved &&
                    condition.progressCurrent != null &&
                    condition.progressTarget != null && (
                      <div className="victory-card__progress">
                        <div className="victory-card__progress-label">
                          {condition.progressLabel}: {condition.progressCurrent} /{' '}
                          {condition.progressTarget}
                        </div>
                        {percent != null && (
                          <div className="victory-card__progress-bar" aria-hidden="true">
                            <div
                              className="victory-card__progress-fill"
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                        )}
                      </div>
                    )}

                  {condition.achieved && condition.achievedAt && (
                    <p className="victory-card__meta">
                      Выполнено {new Date(condition.achievedAt).toLocaleString()}
                      {condition.achievedByMe ? ' вами' : ''}
                    </p>
                  )}
                </li>
              );
            })}
          </ul>
        </>
      </PageState>
    </section>
  );
}
