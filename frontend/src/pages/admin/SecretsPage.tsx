import { useState } from 'react';
import { createSecret } from '../../api/admin/secrets';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { PlayerSecretInfo } from '../../types/admin';

export function SecretsPage() {
  const { players } = useAdminPlayers();
  const [secrets, setSecrets] = useState<PlayerSecretInfo[]>([]);
  const [userId, setUserId] = useState('');
  const [code, setCode] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [rewardType, setRewardType] = useState<PlayerSecretInfo['rewardType']>('COIN');
  const [payloadJson, setPayloadJson] = useState('{"amount": 25}');
  const [submitting, setSubmitting] = useState(false);

  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!userId) {
      showToast('Выберите игрока', 'error');
      return;
    }
    setSubmitting(true);
    try {
      let rewardPayload: Record<string, unknown> = {};
      if (payloadJson.trim()) {
        rewardPayload = JSON.parse(payloadJson) as Record<string, unknown>;
      }
      const created = await createSecret({
        userId,
        code,
        title,
        description,
        rewardType,
        rewardPayload,
      });
      setSecrets((current) => [...current, created]);
      showToast(`Промокод «${created.code}» создан`);
      setCode('');
      setTitle('');
      setDescription('');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать промокод',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Промокод"
        description="Создание промокодов для игроков. Список показывает коды, созданные в этой сессии браузера (нет API списка)."
      />

      <FormCard title="Создать промокод">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Игрок</span>
            <PlayerSelect players={players} value={userId} onChange={setUserId} required />
          </label>
          <label className="form-field">
            <span>Код</span>
            <input value={code} onChange={(event) => setCode(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Название</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Описание</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Тип награды</span>
            <select
              className="form-select"
              value={rewardType}
              onChange={(event) =>
                setRewardType(event.target.value as PlayerSecretInfo['rewardType'])
              }
            >
              <option value="COIN">COIN</option>
              <option value="ITEM">ITEM</option>
              <option value="NUMBER">NUMBER</option>
              <option value="QUEST">QUEST</option>
              <option value="NONE">NONE</option>
            </select>
          </label>
          <label className="form-field form-field--wide">
            <span>Награда (JSON)</span>
            <input value={payloadJson} onChange={(event) => setPayloadJson(event.target.value)} />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать промокод
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Промокоды (сессия)</h2>
      {secrets.length === 0 ? (
        <p className="empty-state">В этой сессии пока нет промокодов.</p>
      ) : (
        <DataTable
          rows={secrets}
          rowKey={(row) => row.id}
          columns={[
            { key: 'code', header: 'Код', render: (row) => row.code },
            { key: 'title', header: 'Название', render: (row) => row.title },
            { key: 'reward', header: 'Награда', render: (row) => row.rewardType },
            { key: 'used', header: 'Использован', render: (row) => (row.used ? 'Да' : 'Нет') },
          ]}
        />
      )}
    </section>
  );
}
