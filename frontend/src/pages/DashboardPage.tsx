import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchCurrentSession } from '../api/locations';
import { fetchMyQuests } from '../api/quests';
import { fetchMyScores } from '../api/score';
import { fetchVictoryConditions } from '../api/victory';
import { fetchMyWallet } from '../api/wallet';
import { PageState } from '../components/ui/PageState';
import { formatEnum, questType, roleLabel, sessionStatus, translateError } from '../i18n/ru';
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
          setError(
            err instanceof Error
              ? translateError(err.message)
              : 'Не удалось загрузить главную',
          );
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
      <h1>Главная</h1>
      <p className="page-hint">Обзор вашей игры.</p>

      <PageState loading={loading} error={error} loadingLabel="Загрузка главной…">
        <div className="dashboard-grid">
          <article className="card">
            <h2>Профиль</h2>
            <dl className="data-list">
              <div>
                <dt>Никнейм</dt>
                <dd>{user?.nickname ?? '—'}</dd>
              </div>
              <div>
                <dt>Логин</dt>
                <dd>{user?.username ?? '—'}</dd>
              </div>
              <div>
                <dt>Роль</dt>
                <dd>{user?.role ? formatEnum(user.role, roleLabel) : '—'}</dd>
              </div>
            </dl>
          </article>

          <article className="card">
            <h2>Сессия</h2>
            <dl className="data-list">
              <div>
                <dt>Игра</dt>
                <dd>{sessionInfo?.name ?? session?.name ?? '—'}</dd>
              </div>
              <div>
                <dt>Статус</dt>
                <dd>
                  <span className={`badge badge--${(sessionInfo?.status ?? 'STARTING').toLowerCase()}`}>
                    {sessionInfo?.status
                      ? formatEnum(sessionInfo.status, sessionStatus)
                      : '—'}
                  </span>
                </dd>
              </div>
            </dl>
          </article>

          <article className="card">
            <h2>Кошелёк</h2>
            <p className="dashboard-stat">{wallet?.balance ?? 0} М-коинов</p>
            <Link to="/wallet" className="card-link">
              Открыть кошелёк →
            </Link>
          </article>

          <article className="card">
            <h2>Очки</h2>
            <p className="dashboard-stat">{totalScore} очков</p>
            <Link to="/score" className="card-link">
              Рейтинг →
            </Link>
          </article>

          <article className="card card--wide">
            <h2>Активные квесты</h2>
            {activeQuests.length === 0 ? (
              <p className="empty-state">Нет активных квестов. Начните квест на странице квестов.</p>
            ) : (
              <ul className="simple-list">
                {activeQuests.map((quest) => (
                  <li key={quest.id}>
                    <strong>{quest.quest.title}</strong>
                    <span>{formatEnum(quest.quest.type, questType)}</span>
                  </li>
                ))}
              </ul>
            )}
            <Link to="/quests" className="card-link">
              Все квесты →
            </Link>
          </article>

          <article className="card card--wide">
            <h2>Победа</h2>
            {victory.length === 0 ? (
              <p className="empty-state">Условия победы не настроены.</p>
            ) : (
              <ul className="simple-list">
                {victory.slice(0, 3).map((condition) => (
                  <li key={condition.id}>
                    <strong>{condition.description}</strong>
                    <span>{condition.achieved ? 'Выполнено' : 'В процессе'}</span>
                  </li>
                ))}
              </ul>
            )}
            {achievedVictory.length > 0 && (
              <p className="status-ok">
                Выполнено условий: {achievedVictory.length}
              </p>
            )}
            <Link to="/victory" className="card-link">
              Условия победы →
            </Link>
          </article>
        </div>
      </PageState>
    </section>
  );
}
