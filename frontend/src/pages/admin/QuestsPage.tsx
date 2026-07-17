import { useCallback, useEffect, useState } from 'react';
import {
  broadcastQuests,
  closeIncompleteQuests,
  createQuest,
  fetchAdminQuests,
  fetchQuestAutoDistribute,
  startQuestAutoDistribute,
  stopQuestAutoDistribute,
  updateQuest,
  type QuestAutoDistributeStatus,
} from '../../api/admin/quests';
import { listItemTemplates } from '../../api/admin/items';
import { fetchSessionPlayers } from '../../api/players';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { formatEnum, questStatus, translateError, ui } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { ItemTemplate, Quest, SessionPlayer } from '../../types';

type RewardKind = 'NONE' | 'COIN' | 'ITEM';
type CompletionPolicy = 'EVERY_PLAYER' | 'LIMITED';
type Audience = 'ALL' | 'PLAYER';

function isBroadcastQuest(quest: Quest): boolean {
  return String(quest.targetConfig?.source ?? '') === 'POOL_BROADCAST';
}

function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleString('ru-RU');
  } catch {
    return value;
  }
}

export function QuestsPage() {
  const [quests, setQuests] = useState<Quest[]>([]);
  const [templates, setTemplates] = useState<ItemTemplate[]>([]);
  const [players, setPlayers] = useState<SessionPlayer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [closing, setClosing] = useState(false);

  const [broadcastCount, setBroadcastCount] = useState('2');
  const [broadcasting, setBroadcasting] = useState(false);
  const [autoStatus, setAutoStatus] = useState<QuestAutoDistributeStatus | null>(null);
  const [autoBusy, setAutoBusy] = useState(false);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [audience, setAudience] = useState<Audience>('ALL');
  const [assigneeUserId, setAssigneeUserId] = useState('');
  const [completionPolicy, setCompletionPolicy] = useState<CompletionPolicy>('EVERY_PLAYER');
  const [completionLimit, setCompletionLimit] = useState('1');
  const [rewardKind, setRewardKind] = useState<RewardKind>('COIN');
  const [coinAmount, setCoinAmount] = useState('40');
  const [itemTemplateId, setItemTemplateId] = useState('');
  const [status, setStatus] = useState<'ACTIVE' | 'DISABLED'>('ACTIVE');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [data, itemTemplates, sessionPlayers, auto] = await Promise.all([
        fetchAdminQuests(),
        listItemTemplates(),
        fetchSessionPlayers(),
        fetchQuestAutoDistribute(),
      ]);
      setQuests(data);
      setTemplates(itemTemplates);
      setPlayers(sessionPlayers);
      setAutoStatus(auto);
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

  const resetForm = () => {
    setEditingId(null);
    setTitle('');
    setDescription('');
    setAudience('ALL');
    setAssigneeUserId('');
    setCompletionPolicy('EVERY_PLAYER');
    setCompletionLimit('1');
    setRewardKind('COIN');
    setCoinAmount('40');
    setItemTemplateId('');
    setStatus('ACTIVE');
  };

  const buildRewardConfig = (): Record<string, unknown> | null => {
    if (rewardKind === 'COIN') {
      return { type: 'COIN', amount: Number(coinAmount) };
    }
    if (rewardKind === 'ITEM') {
      return { type: 'ITEM', itemTemplateId };
    }
    return null;
  };

  const playerName = (userId: string | null | undefined) => {
    if (!userId) {
      return 'Все';
    }
    return players.find((player) => player.id === userId)?.nickname ?? 'Игрок';
  };

  const startEdit = (quest: Quest) => {
    setEditingId(quest.id);
    setTitle(quest.title);
    setDescription(quest.description ?? '');
    setCompletionPolicy(quest.completionPolicy ?? 'EVERY_PLAYER');
    setCompletionLimit(String(quest.completionLimit ?? 1));
    setStatus(quest.status);
    if (quest.assigneeUserId) {
      setAudience('PLAYER');
      setAssigneeUserId(quest.assigneeUserId);
    } else {
      setAudience('ALL');
      setAssigneeUserId('');
    }

    const reward = quest.rewardConfig;
    const type = reward?.type ? String(reward.type).toUpperCase() : 'NONE';
    if (type === 'COIN') {
      setRewardKind('COIN');
      setCoinAmount(String(reward?.amount ?? 40));
      setItemTemplateId('');
    } else if (type === 'ITEM') {
      setRewardKind('ITEM');
      setItemTemplateId(String(reward?.itemTemplateId ?? ''));
    } else {
      setRewardKind('NONE');
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (audience === 'PLAYER' && !assigneeUserId) {
      showToast('Выберите игрока', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const rewardConfig = buildRewardConfig();
      const effectivePolicy = audience === 'PLAYER' ? 'EVERY_PLAYER' : completionPolicy;
      const payload = {
        title,
        description,
        rewardConfig,
        status,
        completionPolicy: effectivePolicy,
        completionLimit:
          audience === 'ALL' && effectivePolicy === 'LIMITED' ? Number(completionLimit) : null,
        assigneeUserId: audience === 'PLAYER' ? assigneeUserId : null,
        assignToAll: audience === 'ALL',
      };

      if (editingId) {
        await updateQuest(editingId, payload);
        showToast('Квест обновлён');
      } else {
        await createQuest({
          ...payload,
          type: 'SOCIAL',
          targetConfig: {},
        });
        showToast('Квест создан');
      }
      resetForm();
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось сохранить квест',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleCloseIncomplete = async () => {
    if (!window.confirm('Закрыть все незавершённые квесты? Игроки больше не смогут их сдать.')) {
      return;
    }
    setClosing(true);
    try {
      const result = await closeIncompleteQuests();
      showToast(`Закрыто активных сдач: ${result.closedPlayerQuests}`);
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось закрыть квесты',
        'error',
      );
    } finally {
      setClosing(false);
    }
  };

  const handleBroadcast = async () => {
    const count = Number(broadcastCount);
    if (!Number.isInteger(count) || count < 1 || count > 20) {
      showToast('Укажите число заданий от 1 до 20', 'error');
      return;
    }
    setBroadcasting(true);
    try {
      const result = await broadcastQuests(count);
      showToast(
        `Выдано ${result.questsCreated} заданий (${result.countPerPlayer} × ${result.playerCount} игроков)`,
      );
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выдать задания',
        'error',
      );
    } finally {
      setBroadcasting(false);
    }
  };

  const handleAutoToggle = async () => {
    setAutoBusy(true);
    try {
      const next = autoStatus?.enabled
        ? await stopQuestAutoDistribute()
        : await startQuestAutoDistribute();
      setAutoStatus(next);
      showToast(next.enabled ? 'Авторассылка включена' : 'Авторассылка остановлена');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось изменить авторассылку',
        'error',
      );
    } finally {
      setAutoBusy(false);
    }
  };

  const policyLabel = (quest: Quest) => {
    if (quest.assigneeUserId) {
      return `Личное · ${playerName(quest.assigneeUserId)}`;
    }
    if (quest.completionPolicy === 'LIMITED') {
      return `Лимит ${quest.completedCount}/${quest.completionLimit ?? 0}`;
    }
    return `Всем · ${quest.completedCount} сдач`;
  };

  const rewardLabel = (quest: Quest) => {
    const reward = quest.rewardConfig;
    if (!reward?.type) {
      return '—';
    }
    if (String(reward.type).toUpperCase() === 'COIN') {
      return `${reward.amount ?? 0} M`;
    }
    return String(reward.type);
  };

  return (
    <section>
      <PageHeader
        title="Квесты"
        description="Создавайте задания для всех гостей или для одного игрока."
      />

      <div className="admin-actions" style={{ marginBottom: '1rem' }}>
        <button
          type="button"
          className="btn btn--secondary"
          disabled={closing}
          onClick={() => void handleCloseIncomplete()}
        >
          {closing ? 'Закрываем…' : 'Закрыть незавершённые (перед аукционом)'}
        </button>
      </div>

      <FormCard title="Рассылка из пула">
        <div className="admin-form-grid">
          <label className="form-field">
            <span>Сколько заданий каждому</span>
            <input
              type="number"
              min={1}
              max={20}
              value={broadcastCount}
              onChange={(event) => setBroadcastCount(event.target.value)}
            />
          </label>
          <div className="admin-actions form-field">
            <button
              type="button"
              className="btn btn--primary"
              disabled={broadcasting}
              onClick={() => void handleBroadcast()}
            >
              {broadcasting ? 'Выдаём…' : 'Выдать задания'}
            </button>
          </div>

          <div className="form-field form-field--wide" style={{ display: 'grid', gap: '0.5rem' }}>
            <p style={{ margin: 0 }}>
              Авто: каждые {autoStatus?.intervalMinutes ?? 30} мин × {autoStatus?.autoCount ?? 2}{' '}
              задания каждому.{' '}
              {autoStatus?.enabled ? (
                <>
                  Включена. Следующая: {formatDateTime(autoStatus.nextDistributionAt)}. Последняя:{' '}
                  {formatDateTime(autoStatus.lastDistributedAt)}.
                </>
              ) : (
                'Выключена.'
              )}
            </p>
            <div className="admin-actions">
              <button
                type="button"
                className={autoStatus?.enabled ? 'btn btn--secondary' : 'btn btn--primary'}
                disabled={autoBusy}
                onClick={() => void handleAutoToggle()}
              >
                {autoBusy
                  ? '…'
                  : autoStatus?.enabled
                    ? 'Остановить авторассылку'
                    : 'Включить авторассылку'}
              </button>
            </div>
          </div>
        </div>
      </FormCard>

      <FormCard title={editingId ? 'Редактировать квест' : 'Создать квест'}>
        <form className="admin-form-grid" onSubmit={(event) => void handleSubmit(event)}>
          <label className="form-field form-field--wide">
            <span>Название</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Описание</span>
            <textarea
              rows={3}
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              placeholder="Удивить именинника, придумать тост…"
            />
          </label>

          <fieldset className="form-field form-field--wide radio-group">
            <legend>Кому задание</legend>
            <label className="radio-option">
              <input
                type="radio"
                name="audience"
                checked={audience === 'ALL'}
                onChange={() => setAudience('ALL')}
              />
              Всем игрокам
            </label>
            <label className="radio-option">
              <input
                type="radio"
                name="audience"
                checked={audience === 'PLAYER'}
                onChange={() => setAudience('PLAYER')}
              />
              Конкретному игроку
            </label>
          </fieldset>

          {audience === 'PLAYER' && (
            <label className="form-field">
              <span>Игрок</span>
              <select
                className="form-select"
                value={assigneeUserId}
                onChange={(event) => setAssigneeUserId(event.target.value)}
                required
              >
                <option value="">Выберите игрока</option>
                {players.map((player) => (
                  <option key={player.id} value={player.id}>
                    {player.nickname}
                  </option>
                ))}
              </select>
            </label>
          )}

          {audience === 'ALL' && (
            <>
              <fieldset className="form-field form-field--wide radio-group">
                <legend>Кто может выполнить</legend>
                <label className="radio-option">
                  <input
                    type="radio"
                    name="policy"
                    checked={completionPolicy === 'EVERY_PLAYER'}
                    onChange={() => setCompletionPolicy('EVERY_PLAYER')}
                  />
                  Каждый гость один раз
                </label>
                <label className="radio-option">
                  <input
                    type="radio"
                    name="policy"
                    checked={completionPolicy === 'LIMITED'}
                    onChange={() => setCompletionPolicy('LIMITED')}
                  />
                  Ограниченное число (кто быстрее)
                </label>
              </fieldset>

              {completionPolicy === 'LIMITED' && (
                <label className="form-field">
                  <span>Сколько человек успеют</span>
                  <input
                    type="number"
                    min={1}
                    value={completionLimit}
                    onChange={(event) => setCompletionLimit(event.target.value)}
                    required
                  />
                </label>
              )}
            </>
          )}

          <fieldset className="form-field form-field--wide radio-group">
            <legend>Награда</legend>
            <label className="radio-option">
              <input
                type="radio"
                name="reward"
                checked={rewardKind === 'NONE'}
                onChange={() => setRewardKind('NONE')}
              />
              Нет
            </label>
            <label className="radio-option">
              <input
                type="radio"
                name="reward"
                checked={rewardKind === 'COIN'}
                onChange={() => setRewardKind('COIN')}
              />
              Монеты
            </label>
            <label className="radio-option">
              <input
                type="radio"
                name="reward"
                checked={rewardKind === 'ITEM'}
                onChange={() => setRewardKind('ITEM')}
              />
              Предмет
            </label>
          </fieldset>

          {rewardKind === 'COIN' && (
            <label className="form-field">
              <span>Количество M-Coins</span>
              <input
                type="number"
                min={1}
                value={coinAmount}
                onChange={(event) => setCoinAmount(event.target.value)}
                required
              />
            </label>
          )}

          {rewardKind === 'ITEM' && (
            <label className="form-field">
              <span>Предмет</span>
              <select
                className="form-select"
                value={itemTemplateId}
                onChange={(event) => setItemTemplateId(event.target.value)}
                required
              >
                <option value="">Выберите предмет</option>
                {templates.map((template) => (
                  <option key={template.id} value={template.id}>
                    {template.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          <label className="form-field">
            <span>Статус</span>
            <select
              className="form-select"
              value={status}
              onChange={(event) => setStatus(event.target.value as 'ACTIVE' | 'DISABLED')}
            >
              <option value="ACTIVE">{formatEnum('ACTIVE', questStatus)}</option>
              <option value="DISABLED">Отключён</option>
            </select>
          </label>

          <div className="admin-actions">
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {editingId ? ui.save : 'Создать квест'}
            </button>
            {editingId && (
              <button type="button" className="btn btn--secondary" onClick={resetForm}>
                {ui.cancel}
              </button>
            )}
          </div>
        </form>
      </FormCard>

      <h2 className="section-title">Список квестов</h2>
      <PageState loading={loading} error={error} empty={quests.length === 0}>
        <DataTable
          rows={quests}
          rowKey={(row) => row.id}
          columns={[
            {
              key: 'title',
              header: 'Название',
              render: (row) => (
                <span>
                  {row.title}
                  {isBroadcastQuest(row) ? (
                    <span
                      style={{
                        marginLeft: '0.5rem',
                        fontSize: '0.75rem',
                        opacity: 0.75,
                        whiteSpace: 'nowrap',
                      }}
                    >
                      · рассылка
                    </span>
                  ) : null}
                </span>
              ),
            },
            { key: 'policy', header: 'Кому', render: (row) => policyLabel(row) },
            { key: 'reward', header: 'Награда', render: (row) => rewardLabel(row) },
            {
              key: 'status',
              header: 'Статус',
              render: (row) =>
                row.status === 'DISABLED' ? 'Отключён' : formatEnum(row.status, questStatus),
            },
            {
              key: 'actions',
              header: '',
              render: (row) => (
                <button
                  type="button"
                  className="btn btn--secondary btn--small"
                  onClick={() => startEdit(row)}
                >
                  {ui.edit}
                </button>
              ),
            },
          ]}
        />
      </PageState>
    </section>
  );
}
