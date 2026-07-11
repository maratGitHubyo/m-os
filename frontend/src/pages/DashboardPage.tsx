import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchCurrentSession } from '../api/locations';
import { fetchMyQuests } from '../api/quests';
import { fetchMyScores } from '../api/score';
import { fetchVictoryConditions } from '../api/victory';
import { fetchMyWallet } from '../api/wallet';
import { PageState } from '../components/ui/PageState';
import { useAuth } from '../stores/authStore';
import type {
  GameSessionInfo,
  QuestProgress,
  Score,
  VictoryCondition,
  Wallet,
} from '../types';

export function DashboardPage() {
  const { user, session } = useAuth();
  const [sessionInfo, setSessionInfo] = useState<GameSessionInfo | null>(null);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [scores, setScores] = useState<Score[]>([]);
  const [quests, setQuests] = useState<QuestProgress[]>([]);
  const [victory, setVictory] = useState<VictoryCondition[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const [currentSession, myWallet, myScores, myQuests, conditions] = await Promise.all([
          fetchCurrentSession(),
          fetchMyWallet(),
          fetchMyScores(),
          fetchMyQuests(),
          fetchVictoryConditions(),
        ]);

        if (!cancelled) {
          setSessionInfo(currentSession);
          setWallet(myWallet);
          setScores(myScores);
          setQuests(myQuests);
          setVictory(conditions);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard');
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

  const totalScore = scores.find((score) => score.category === 'TOTAL')?.points ?? 0;
  const activeQuests = quests.filter((quest) => quest.status === 'ACTIVE');
  const achievedVictory = victory.filter((condition) => condition.achieved);

  return (
    <section className="dashboard-page">
      <h1>Dashboard</h1>
      <p className="page-hint">Your game overview at a glance.</p>

      <PageState loading={loading} error={error} loadingLabel="Loading dashboard…">
        <div className="dashboard-grid">
          <article className="card">
            <h2>Profile</h2>
            <dl className="data-list">
              <div>
                <dt>Nickname</dt>
                <dd>{user?.nickname ?? '—'}</dd>
              </div>
              <div>
                <dt>Username</dt>
                <dd>{user?.username ?? '—'}</dd>
              </div>
              <div>
                <dt>Role</dt>
                <dd>{user?.role ?? '—'}</dd>
              </div>
            </dl>
          </article>

          <article className="card">
            <h2>Session</h2>
            <dl className="data-list">
              <div>
                <dt>Game</dt>
                <dd>{sessionInfo?.name ?? session?.name ?? '—'}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>
                  <span className={`badge badge--${(sessionInfo?.status ?? 'STARTING').toLowerCase()}`}>
                    {sessionInfo?.status ?? '—'}
                  </span>
                </dd>
              </div>
            </dl>
          </article>

          <article className="card">
            <h2>Wallet</h2>
            <p className="dashboard-stat">{wallet?.balance ?? 0} coins</p>
            <Link to="/wallet" className="card-link">
              View wallet →
            </Link>
          </article>

          <article className="card">
            <h2>Score</h2>
            <p className="dashboard-stat">{totalScore} pts</p>
            <Link to="/score" className="card-link">
              Leaderboard →
            </Link>
          </article>

          <article className="card card--wide">
            <h2>Active Quests</h2>
            {activeQuests.length === 0 ? (
              <p className="empty-state">No active quests. Start one from the quests page.</p>
            ) : (
              <ul className="simple-list">
                {activeQuests.map((quest) => (
                  <li key={quest.id}>
                    <strong>{quest.quest.title}</strong>
                    <span>{quest.quest.type.replaceAll('_', ' ').toLowerCase()}</span>
                  </li>
                ))}
              </ul>
            )}
            <Link to="/quests" className="card-link">
              All quests →
            </Link>
          </article>

          <article className="card card--wide">
            <h2>Victory</h2>
            {victory.length === 0 ? (
              <p className="empty-state">No victory conditions configured.</p>
            ) : (
              <ul className="simple-list">
                {victory.slice(0, 3).map((condition) => (
                  <li key={condition.id}>
                    <strong>{condition.description}</strong>
                    <span>{condition.achieved ? 'Achieved' : 'In progress'}</span>
                  </li>
                ))}
              </ul>
            )}
            {achievedVictory.length > 0 && (
              <p className="status-ok">{achievedVictory.length} condition(s) achieved</p>
            )}
            <Link to="/victory" className="card-link">
              Victory conditions →
            </Link>
          </article>
        </div>
      </PageState>
    </section>
  );
}
