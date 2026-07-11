import { useEffect, useState } from 'react';
import {
  fetchVictoryConditions,
  getStoredToken,
  setStoredToken,
} from '../api/victory';
import type { VictoryCondition } from '../types/victory';

function formatType(type: VictoryCondition['type']): string {
  return type.replaceAll('_', ' ').toLowerCase();
}

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
  const [tokenInput, setTokenInput] = useState(getStoredToken() ?? '');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const loadConditions = async (token: string) => {
    setLoading(true);
    setError(null);

    try {
      setStoredToken(token);
      const data = await fetchVictoryConditions(token);
      setConditions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load victory conditions');
      setConditions([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const token = getStoredToken();
    if (token) {
      void loadConditions(token);
    }
  }, []);

  const anyAchieved = conditions.some((c) => c.achieved);
  const myVictory = conditions.some((c) => c.achievedByMe);

  return (
    <section className="victory-page">
      <h1>Victory Conditions</h1>
      <p className="victory-page__hint">
        Progress is calculated by the server. Complete any active condition to win.
      </p>

      <div className="victory-page__auth">
        <input
          className="victory-page__token-input"
          type="password"
          placeholder="JWT token"
          value={tokenInput}
          onChange={(e) => setTokenInput(e.target.value)}
        />
        <button type="button" onClick={() => void loadConditions(tokenInput)}>
          Load
        </button>
      </div>

      {loading && <p className="status-loading">Loading conditions…</p>}
      {error && <p className="status-error">{error}</p>}

      {!loading && !error && conditions.length === 0 && (
        <p className="victory-page__empty">No victory conditions configured yet.</p>
      )}

      {!loading && !error && conditions.length > 0 && (
        <>
          <div className="victory-page__summary">
            {myVictory && <p className="status-ok">You achieved a victory condition!</p>}
            {!myVictory && anyAchieved && (
              <p className="victory-page__someone-won">A victory condition has been achieved.</p>
            )}
            {!anyAchieved && <p className="victory-page__pending">No conditions achieved yet.</p>}
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
                    <span className="victory-card__type">{formatType(condition.type)}</span>
                    <span
                      className={
                        condition.achieved ? 'victory-card__badge victory-card__badge--done' : 'victory-card__badge'
                      }
                    >
                      {condition.achieved ? 'Achieved' : 'In progress'}
                    </span>
                  </div>

                  <p className="victory-card__description">{condition.description}</p>

                  {!condition.achieved &&
                    condition.progressCurrent != null &&
                    condition.progressTarget != null && (
                      <div className="victory-card__progress">
                        <div className="victory-card__progress-label">
                          {condition.progressLabel}: {condition.progressCurrent} / {condition.progressTarget}
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
                      Achieved {new Date(condition.achievedAt).toLocaleString()}
                      {condition.achievedByMe ? ' by you' : ''}
                    </p>
                  )}
                </li>
              );
            })}
          </ul>
        </>
      )}
    </section>
  );
}
