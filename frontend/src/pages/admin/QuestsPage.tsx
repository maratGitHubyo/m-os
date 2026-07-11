import { useCallback, useEffect, useState } from 'react';
import { createQuest, fetchAdminQuests, updateQuest } from '../../api/admin/quests';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { showToast } from '../../stores/toastStore';
import type { Quest } from '../../types';

const questTypes: Quest['type'][] = [
  'COLLECT_ITEMS',
  'FIND_LOCATIONS',
  'COLLECT_NUMBERS',
  'REACH_SCORE',
  'CUSTOM',
];

export function QuestsPage() {
  const [quests, setQuests] = useState<Quest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<Quest['type']>('FIND_LOCATIONS');
  const [targetJson, setTargetJson] = useState('{"count": 3}');
  const [status, setStatus] = useState<'ACTIVE' | 'DISABLED'>('ACTIVE');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminQuests();
      setQuests(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load quests');
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
    setType('FIND_LOCATIONS');
    setTargetJson('{"count": 3}');
    setStatus('ACTIVE');
  };

  const startEdit = (quest: Quest) => {
    setEditingId(quest.id);
    setTitle(quest.title);
    setDescription(quest.description);
    setType(quest.type);
    setTargetJson(JSON.stringify(quest.targetConfig));
    setStatus(quest.status);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const targetConfig = JSON.parse(targetJson) as Record<string, unknown>;
      if (editingId) {
        await updateQuest(editingId, {
          title,
          description,
          targetConfig,
          status,
        });
        showToast('Quest updated');
      } else {
        await createQuest({
          title,
          description,
          type,
          targetConfig,
          status,
        });
        showToast('Quest created');
      }
      resetForm();
      await load();
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Quest save failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader title="Quests" description="Create and edit quest definitions." />

      <FormCard title={editingId ? 'Edit quest' : 'Create quest'}>
        <form className="admin-form-grid" onSubmit={(event) => void handleSubmit(event)}>
          <label className="form-field">
            <span>Title</span>
            <input value={title} onChange={(event) => setTitle(event.target.value)} required />
          </label>
          {!editingId && (
            <label className="form-field">
              <span>Type</span>
              <select
                className="form-select"
                value={type}
                onChange={(event) => setType(event.target.value as Quest['type'])}
              >
                {questTypes.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </label>
          )}
          <label className="form-field form-field--wide">
            <span>Description</span>
            <input value={description} onChange={(event) => setDescription(event.target.value)} />
          </label>
          <label className="form-field form-field--wide">
            <span>Target config (JSON)</span>
            <input value={targetJson} onChange={(event) => setTargetJson(event.target.value)} />
          </label>
          <label className="form-field">
            <span>Status</span>
            <select
              className="form-select"
              value={status}
              onChange={(event) => setStatus(event.target.value as 'ACTIVE' | 'DISABLED')}
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
          </label>
          <div className="admin-actions">
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {editingId ? 'Save changes' : 'Create quest'}
            </button>
            {editingId && (
              <button type="button" className="btn btn--secondary" onClick={resetForm}>
                Cancel edit
              </button>
            )}
          </div>
        </form>
      </FormCard>

      <h2 className="section-title">Quest list</h2>
      <PageState loading={loading} error={error} empty={quests.length === 0}>
        <DataTable
          rows={quests}
          rowKey={(row) => row.id}
          columns={[
            { key: 'title', header: 'Title', render: (row) => row.title },
            { key: 'type', header: 'Type', render: (row) => row.type },
            { key: 'status', header: 'Status', render: (row) => row.status },
            {
              key: 'actions',
              header: '',
              render: (row) => (
                <button type="button" className="btn btn--secondary btn--small" onClick={() => startEdit(row)}>
                  Edit
                </button>
              ),
            },
          ]}
        />
      </PageState>
    </section>
  );
}
