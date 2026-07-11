import { useState } from 'react';
import { createSecret } from '../../api/admin/secrets';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
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
      showToast('Select a player', 'error');
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
      showToast(`Secret "${created.code}" created`);
      setCode('');
      setTitle('');
      setDescription('');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create secret', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Secrets"
        description="Create player secrets. List shows secrets created in this browser session (no list API)."
      />

      <FormCard title="Create secret">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreate(event)}>
          <label className="form-field">
            <span>Player</span>
            <PlayerSelect players={players} value={userId} onChange={setUserId} required />
          </label>
          <label className="form-field">
            <span>Code</span>
            <input value={code} onChange={(event) => setCode(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Title</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>
          <label className="form-field form-field--wide">
            <span>Description</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Reward type</span>
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
            <span>Reward payload (JSON)</span>
            <input value={payloadJson} onChange={(event) => setPayloadJson(event.target.value)} />
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Create secret
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Secrets (session)</h2>
      {secrets.length === 0 ? (
        <p className="empty-state">No secrets in this session yet.</p>
      ) : (
        <DataTable
          rows={secrets}
          rowKey={(row) => row.id}
          columns={[
            { key: 'code', header: 'Code', render: (row) => row.code },
            { key: 'title', header: 'Title', render: (row) => row.title },
            { key: 'reward', header: 'Reward', render: (row) => row.rewardType },
            { key: 'used', header: 'Used', render: (row) => (row.used ? 'Yes' : 'No') },
          ]}
        />
      )}
    </section>
  );
}
