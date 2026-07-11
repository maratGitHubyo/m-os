import { useCallback, useEffect, useState } from 'react';
import { fetchMyQuests, fetchQuests, startQuest } from '../api/quests';
import { PageState } from '../components/ui/PageState';
import { formatEnum, questStatus, questType, translateError } from '../i18n/ru';
import type { Quest, QuestProgress } from '../types';

function progressLabel(questProgress: QuestProgress): string | null {
  const current = questProgress.progress.current;
  const target = questProgress.progress.target;
  if (typeof current === 'number' && typeof target === 'number') {
    return `${current} / ${target}`;
  }
  return null;
}

export function QuestPage() {
  const [available, setAvailable] = useState<Quest[]>([]);
  const [myQuests, setMyQuests] = useState<QuestProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [startingId, setStartingId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const [quests, playerQuests] = await Promise.all([fetchQuests(), fetchMyQuests()]);
      setAvailable(quests);
      setMyQuests(playerQuests);
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить квесты',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const playerQuestByQuestId = new Map(myQuests.map((quest) => [quest.questId, quest]));

  const handleStart = async (questId: string) => {
    setStartingId(questId);
    setError(null);

    try {
      await startQuest(questId);
      await load();
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось начать квест',
      );
    } finally {
      setStartingId(null);
    }
  };

  const activeQuests = myQuests.filter((quest) => quest.status === 'ACTIVE');
  const completedQuests = myQuests.filter((quest) => quest.status === 'COMPLETED');

  return (
    <section className="quest-page">
      <h1>Квесты</h1>
      <p className="page-hint">Начинайте квесты и отслеживайте прогресс.</p>

      <PageState loading={loading} error={error} loadingLabel="Загрузка квестов…">
        <>
          <h2 className="section-title">Доступные квесты</h2>
          {available.length === 0 ? (
            <p className="empty-state">Нет доступных квестов.</p>
          ) : (
            <ul className="quest-list">
              {available.map((quest) => {
                const playerQuest = playerQuestByQuestId.get(quest.id);
                const canStart = !playerQuest || playerQuest.status === 'FAILED';

                return (
                  <li key={quest.id} className="quest-card">
                    <div className="quest-card__header">
                      <h3>{quest.title}</h3>
                      <span className="quest-card__type">{formatEnum(quest.type, questType)}</span>
                    </div>
                    <p>{quest.description}</p>
                    {playerQuest ? (
                      <p className="quest-card__status">
                        Статус: {formatEnum(playerQuest.status, questStatus)}
                      </p>
                    ) : (
                      <button
                        type="button"
                        className="btn btn--primary"
                        disabled={!canStart || startingId === quest.id}
                        onClick={() => void handleStart(quest.id)}
                      >
                        {startingId === quest.id ? 'Запуск…' : 'Начать квест'}
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}

          <h2 className="section-title">Активные квесты</h2>
          {activeQuests.length === 0 ? (
            <p className="empty-state">Нет активных квестов.</p>
          ) : (
            <ul className="quest-list">
              {activeQuests.map((quest) => (
                <li key={quest.id} className="quest-card quest-card--active">
                  <div className="quest-card__header">
                    <h3>{quest.quest.title}</h3>
                    <span className="badge badge--active">Активно</span>
                  </div>
                  <p>{quest.quest.description}</p>
                  {progressLabel(quest) && (
                    <p className="quest-card__progress">Прогресс: {progressLabel(quest)}</p>
                  )}
                </li>
              ))}
            </ul>
          )}

          {completedQuests.length > 0 && (
            <>
              <h2 className="section-title">Выполненные</h2>
              <ul className="quest-list">
                {completedQuests.map((quest) => (
                  <li key={quest.id} className="quest-card quest-card--completed">
                    <div className="quest-card__header">
                      <h3>{quest.quest.title}</h3>
                      <span className="badge badge--completed">Выполнено</span>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </>
      </PageState>
    </section>
  );
}
