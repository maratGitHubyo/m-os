import { useState } from 'react';
import { createItemTemplate, grantItem } from '../../api/admin/items';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { formatEnum, itemRarity, translateError } from '../../i18n/ru';
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
      showToast(`Шаблон «${created.name}» создан`);
      setName('');
      setDescription('');
      setImageUrl('');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось создать шаблон',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleGrant = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!grantUserId || !grantTemplateId) {
      showToast('Выберите игрока и шаблон', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await grantItem({ userId: grantUserId, itemTemplateId: grantTemplateId });
      showToast('Предмет выдан');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось выдать предмет',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Предметы"
        description="Создание шаблонов предметов и выдача игрокам. Список показывает шаблоны, созданные в этой сессии браузера (нет API списка)."
      />

      <FormCard title="Создать шаблон">
        <form className="admin-form-grid" onSubmit={(event) => void handleCreateTemplate(event)}>
          <label className="form-field">
            <span>Название</span>
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Редкость</span>
            <select
              className="form-select"
              value={rarity}
              onChange={(event) => setRarity(event.target.value as ItemTemplate['rarity'])}
            >
              {rarities.map((value) => (
                <option key={value} value={value}>
                  {formatEnum(value, itemRarity)}
                </option>
              ))}
            </select>
          </label>
          <label className="form-field form-field--wide">
            <span>Описание</span>
            <input
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              required
            />
          </label>
          <label className="form-field">
            <span>URL изображения</span>
            <input value={imageUrl} onChange={(event) => setImageUrl(event.target.value)} />
          </label>
          <label className="form-field form-field--checkbox">
            <input
              type="checkbox"
              checked={isUnique}
              onChange={(event) => setIsUnique(event.target.checked)}
            />
            <span>Уникальный предмет</span>
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Создать шаблон
          </button>
        </form>
      </FormCard>

      <FormCard title="Выдать предмет">
        <form className="admin-form-grid" onSubmit={(event) => void handleGrant(event)}>
          <label className="form-field">
            <span>Игрок</span>
            <PlayerSelect players={players} value={grantUserId} onChange={setGrantUserId} required />
          </label>
          <label className="form-field">
            <span>Шаблон</span>
            <select
              className="form-select"
              value={grantTemplateId}
              onChange={(event) => setGrantTemplateId(event.target.value)}
              required
            >
              <option value="">Выберите шаблон…</option>
              {templates.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            Выдать предмет
          </button>
        </form>
      </FormCard>

      <h2 className="section-title">Шаблоны (сессия)</h2>
      {templates.length === 0 ? (
        <p className="empty-state">В этой сессии пока нет шаблонов.</p>
      ) : (
        <DataTable
          rows={templates}
          rowKey={(row) => row.id}
          columns={[
            { key: 'name', header: 'Название', render: (row) => row.name },
            {
              key: 'rarity',
              header: 'Редкость',
              render: (row) => formatEnum(row.rarity, itemRarity),
            },
            { key: 'unique', header: 'Уникальный', render: (row) => (row.isUnique ? 'Да' : 'Нет') },
            { key: 'id', header: 'ID', render: (row) => <span className="mono">{row.id}</span> },
          ]}
        />
      )}
    </section>
  );
}
