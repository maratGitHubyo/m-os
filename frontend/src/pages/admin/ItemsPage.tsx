import { useState } from 'react';
import { createItemTemplate, grantItem } from '../../api/admin/items';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { showToast } from '../../stores/toastStore';
import type { ItemTemplate } from '../../types/admin';

const rarities: ItemTemplate['rarity'][] = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY'];

export function ItemsPage() {
  const { players } = useAdminPlayers();
  const [templates, setTemplates] = useState<ItemTemplate[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [rarity, setRarity] = useState<ItemTemplate['rarity']>('COMMON');
  const [isUnique, setIsUnique] = useState(false);
  const [grantUserId, setGrantUserId] = useState('');
  const [grantTemplateId, setGrantTemplateId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleCreateTemplate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const created = await createItemTemplate({
        name,
        description,
        imageUrl: imageUrl || undefined,
        rarity,
        isUnique,
      });
      setTemplates((current) => [...current, created]);
      showToast(`Template "${created.name}" created`);
      setName('');
      setDescription('');
      setImageUrl('');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to create template', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleGrant = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!grantUserId || !grantTemplateId) {
      showToast('Select player and template', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await grantItem({ userId: grantUserId, itemTemplateId: grantTemplateId });
      showToast('Item granted');
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Failed to grant item', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Items"
        description="Create item templates and grant items. Template list shows items created in this browser session (no list API)."
      />

      <FormCard title="Create template">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreateTemplate(event)}>
          <label className="form-field">
            <span>Name</span>
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Rarity</span>
            <select
              className="form-select"
              value={rarity}
              onChange={(event) => setRarity(event.target.value as ItemTemplate['rarity'])}
            >
              {rarities.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field form-field--wide">
            <span>Description</span>
            <input
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              required
            />
          </label>
          <label className="form-field">
            <span>Image URL</span>
            <input value={imageUrl} onChange={(event) => setImageUrl(event.target.value)} />
          </label>
          <label className="form-field form-field--checkbox">
            <input
              type="checkbox"
              checked={isUnique}
              onChange={(event) => setIsUnique(event.target.checked)}
            />
            <span>Unique item</span>
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Create template
          </button>
        </form>
      </FormCard>

      <FormCard title="Grant item">
        <form className="admin-form-grid" onSubmit={(event) => void handleGrant(event)}>
          <label className="form-field">
            <span>Player</span>
            <PlayerSelect players={players} value={grantUserId} onChange={setGrantUserId} required />
          </label>
          <label className="form-field">
            <span>Template</span>
            <select
              className="form-select"
              value={grantTemplateId}
              onChange={(event) => setGrantTemplateId(event.target.value)}
              required
            >
              <option value="">Select template…</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Grant item
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Templates (session)</h2>
      {templates.length === 0 ? (
        <p className="empty-state">No templates in this session yet.</p>
      ) : (
        <DataTable
          rows={templates}
          rowKey={(row) => row.id}
          columns={[
            { key: 'name', header: 'Name', render: (row) => row.name },
            { key: 'rarity', header: 'Rarity', render: (row) => row.rarity },
            { key: 'unique', header: 'Unique', render: (row) => (row.isUnique ? 'Yes' : 'No') },
            { key: 'id', header: 'ID', render: (row) => <span className="mono">{row.id}</span> },
          ]}
        />
      )}
    </section>
  );
}
