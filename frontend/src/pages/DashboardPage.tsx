import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchCurrentSession } from '../api/locations';
import { fetchMyQuests } from '../api/quests';
import { fetchMyWallet } from '../api/wallet';
import { PageState } from '../components/ui/PageState';
import { formatEnum, questType, roleLabel, sessionStatus, translateError } from '../i18n/ru';
import { useAuth } from '../stores/authStore';
import type { GameSessionInfo, QuestProgress, Wallet } from '../types';

export function DashboardPage() {
  const { user, session } = useAuth();
  const [sessionInfo, setSessionInfo] = useState<GameSessionInfo | null>(null);
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [quests, setQuests] = useState<QuestProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const [currentSession, myWallet, myQuests] = await Promise.all([
          fetchCurrentSession(),
          fetchMyWallet(),
          fetchMyQuests(),
        ]);

        if (!cancelled) {
          setSessionInfo(currentSession);
          setWallet(myWallet);
          setQuests(myQuests);
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

  const activeQuests = quests.filter((quest) => quest.status === 'ACTIVE');

  return (
    <section className="dashboard-page">
      <h1>Главная</h1>
      <p className="page-hint">Обзор вашей игры.</p>

      <PageState loading={loading} error={error} loadingLabel="Загрузка главной…">
        <div className="dashboard-grid">
          <article className="card card--wide dashboard-flow">
            <h2>Игровой цикл</h2>
            <p className="page-hint dashboard-flow__steps">
              Исследовать → Получить M-Coins → Обменяться → Собрать больше к аукциону
            </p>
            <div className="quick-actions">
              <Link to="/scan" className="quick-action-card">
                <strong>QR-коды</strong>
                <span>Камерой телефона → по ссылке</span>
              </Link>
              <Link to="/secrets" className="quick-action-card">
                <strong>Промокод</strong>
                <span>Активировать код</span>
              </Link>
              <Link to="/trades" className="quick-action-card">
                <strong>Обмены</strong>
                <span>Предметы и монеты</span>
              </Link>
            </div>
          </article>

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
        </div>
      </PageState>
    </section>
  );
}
