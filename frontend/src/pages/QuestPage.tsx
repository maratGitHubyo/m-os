import { useCallback, useEffect, useState } from 'react';
import { completeQuest, fetchMyQuests, fetchQuests, startQuest } from '../api/quests';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { PageHeader } from '../components/ui/PageHeader';
import { PageState } from '../components/ui/PageState';
import { Progress } from '../components/ui/Progress';
import { formatEnum, questStatus, translateError } from '../i18n/ru';
import { showToast } from '../stores/toastStore';
import type { Quest, QuestProgress } from '../types';

function rewardLabel(quest: Quest): string | null {
  const reward = quest.rewardConfig;
  if (!reward || !reward.type) {
    return null;
  }
  const type = String(reward.type).toUpperCase();
  if (type === 'COIN' && reward.amount != null) {
    return `+${reward.amount} M-Coins`;
  }
  if (type === 'ITEM') {
    return 'Предмет в награду';
  }
  return null;
}

function policyLabel(quest: Quest): string {
  if (quest.assigneeUserId) {
    return 'Личное задание';
  }
  if (quest.completionPolicy === 'LIMITED') {
    const limit = quest.completionLimit ?? 1;
    const done = quest.completedCount ?? 0;
    return `Кто быстрее · ${done}/${limit}`;
  }
  return 'Для всех · один раз';
}

export function QuestPage() {
  const [available, setAvailable] = useState<Quest[]>([]);
  const [myQuests, setMyQuests] = useState<QuestProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [notes, setNotes] = useState<Record<string, string>>({});

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

  const handleStart = async (questId: string) => {
    setBusyId(questId);
    setError(null);
    try {
      await startQuest(questId);
      showToast('Квест взят');
      await load();
    } catch (err) {
      setError(err instanceof Error ? translateError(err.message) : 'Не удалось взять квест');
    } finally {
      setBusyId(null);
    }
  };

  const handleComplete = async (questId: string) => {
    setBusyId(questId);
    setError(null);
    try {
      await completeQuest(questId, notes[questId]);
      showToast('Квест выполнен — награда получена');
      await load();
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось завершить квест',
      );
    } finally {
      setBusyId(null);
    }
  };

  const activeQuests = myQuests.filter((quest) => quest.status === 'ACTIVE');
  const completedQuests = myQuests.filter((quest) => quest.status === 'COMPLETED');
  const failedQuests = myQuests.filter((quest) => quest.status === 'FAILED');

  return (
    <section className="quest-page">
      <PageHeader
        title="Миссии"
        hint="Социальные задания на честном слове: возьмите квест, сделайте в жизни и отметьте «Готово»."
      />

      <PageState loading={loading} error={error} loadingLabel="Загрузка квестов…" skeletonCount={3}>
        <>
          <h2 className="section-title">Доступные</h2>
          {available.length === 0 ? (
            <p className="empty-state">Сейчас нет доступных миссий.</p>
          ) : (
            <ul className="quest-list">
              {available.map((quest) => (
                <li key={quest.id} className="quest-card">
                  <div className="quest-card__header">
                    <h3>{quest.title}</h3>
                    <span className="quest-card__type">{policyLabel(quest)}</span>
                  </div>
                  <p>{quest.description}</p>
                  {quest.completionPolicy === 'LIMITED' && (quest.completionLimit ?? 0) > 0 && (
                    <Progress
                      className="quest-card__progress"
                      label="Места"
                      value={quest.completedCount ?? 0}
                      max={quest.completionLimit ?? 1}
                    />
                  )}
                  {rewardLabel(quest) && (
                    <p className="quest-card__reward">{rewardLabel(quest)}</p>
                  )}
                  <Button
                    disabled={busyId === quest.id}
                    onClick={() => void handleStart(quest.id)}
                  >
                    {busyId === quest.id ? 'Берём…' : 'Взять миссию'}
                  </Button>
                </li>
              ))}
            </ul>
          )}

          <h2 className="section-title">В работе</h2>
          {activeQuests.length === 0 ? (
            <p className="empty-state">Нет активных миссий. Возьмите задание выше.</p>
          ) : (
            <ul className="quest-list">
              {activeQuests.map((playerQuest) => (
                <li key={playerQuest.id} className="quest-card quest-card--active">
                  <div className="quest-card__header">
                    <h3>{playerQuest.quest.title}</h3>
                    <Badge tone="active">В работе</Badge>
                  </div>
                  <p>{playerQuest.quest.description}</p>
                  {rewardLabel(playerQuest.quest) && (
                    <p className="quest-card__reward">{rewardLabel(playerQuest.quest)}</p>
                  )}
                  <label className="form-field">
                    <span>Что сделали? (необязательно)</span>
                    <textarea
                      rows={3}
                      value={notes[playerQuest.questId] ?? ''}
                      onChange={(event) =>
                        setNotes((current) => ({
                          ...current,
                          [playerQuest.questId]: event.target.value,
                        }))
                      }
                      placeholder="Коротко: тост, подарок, сюрприз…"
                    />
                  </label>
                  <Button
                    disabled={busyId === playerQuest.questId}
                    onClick={() => void handleComplete(playerQuest.questId)}
                  >
                    {busyId === playerQuest.questId ? 'Отправляем…' : 'Готово'}
                  </Button>
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
                      <Badge tone="completed">{formatEnum(quest.status, questStatus)}</Badge>
                    </div>
                    {quest.completionNote && (
                      <p className="quest-card__note">«{quest.completionNote}»</p>
                    )}
                  </li>
                ))}
              </ul>
            </>
          )}

          {failedQuests.length > 0 && (
            <>
              <h2 className="section-title">Закрыты</h2>
              <ul className="quest-list">
                {failedQuests.map((quest) => (
                  <li key={quest.id} className="quest-card">
                    <div className="quest-card__header">
                      <h3>{quest.quest.title}</h3>
                      <Badge>{formatEnum(quest.status, questStatus)}</Badge>
                    </div>
                    <p className="page-hint">
                      Мест больше нет или организатор закрыл незавершённые квесты.
                    </p>
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
