import { useCallback, useEffect, useState } from 'react';
import {
  createLocation,
  fetchAdminLocations,
  updateLocation,
} from '../../api/admin/locations';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { translateError, ui } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { LocationPoint } from '../../types';

export function LocationsPage() {
  const [locations, setLocations] = useState<LocationPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [zone, setZone] = useState('');
  const [x, setX] = useState('50');
  const [y, setY] = useState('50');
  const [hidden, setHidden] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminLocations();
      setLocations(data);
    } catch (err) {
      setError(
        err instanceof Error ? translateError(err.message) : 'Не удалось загрузить локации',
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
    setName('');
    setDescription('');
    setZone('');
    setX('50');
    setY('50');
    setHidden(false);
  };

  const startEdit = (location: LocationPoint) => {
    setEditingId(location.id);
    setName(location.name ?? '');
    setDescription(location.description ?? '');
    setZone(location.zone);
    setX(String(location.x));
    setY(String(location.y));
    setHidden(location.hidden);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      if (editingId) {
        await updateLocation(editingId, {
          name,
          description,
          zone,
          x: Number(x),
          y: Number(y),
          hidden,
        });
        showToast('Локация обновлена');
      } else {
        await createLocation({
          name,
          description,
          zone,
          x: Number(x),
          y: Number(y),
          hidden,
        });
        showToast('Локация создана');
      }
      resetForm();
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось сохранить локацию',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader title="Локации" description="Создание и редактирование локаций на карте." />

      <FormCard title={editingId ? 'Редактировать локацию' : 'Создать локацию'}>
        <form className="admin-form-grid" onSubmit={(event) => void handleSubmit(event)}>
          <label className="form-field">
            <span>Название</span>
            <input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label className="form-field">
            <span>Зона</span>
            <input value={zone} onChange={(event) => setZone(event.target.value)} required />
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
            <span>X (%)</span>
            <input type="number" min={0} max={100} value={x} onChange={(event) => setX(event.target.value)} />
          </label>
          <label className="form-field">
            <span>Y (%)</span>
            <input type="number" min={0} max={100} value={y} onChange={(event) => setY(event.target.value)} />
          </label>
          <label className="form-field form-field--checkbox">
            <input type="checkbox" checked={hidden} onChange={(event) => setHidden(event.target.checked)} />
            <span>Скрытая</span>
          </label>
          <div className="admin-actions">
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {editingId ? ui.save : 'Создать локацию'}
            </button>
            {editingId && (
              <button type="button" className="btn btn--secondary" onClick={resetForm}>
                {ui.cancel}
              </button>
            )}
          </div>
        </form>
      </FormCard>

      <h2 className="section-title">Локации</h2>
      <PageState
        loading={loading}
        error={error}
        empty={locations.length === 0}
        emptyMessage="Локаций пока нет."
      >
        <DataTable
          rows={locations}
          rowKey={(row) => row.id}
          columns={[
            {
              key: 'name',
              header: 'Название',
              render: (row) => row.name ?? (row.hidden ? '(скрытая)' : '—'),
            },
            { key: 'zone', header: 'Зона', render: (row) => row.zone },
            {
              key: 'coords',
              header: 'Координаты',
              render: (row) => `${row.x.toFixed(1)}%, ${row.y.toFixed(1)}%`,
            },
            { key: 'hidden', header: 'Скрытая', render: (row) => (row.hidden ? 'Да' : 'Нет') },
            {
              key: 'actions',
              header: '',
              render: (row) => (
                <button type="button" className="btn btn--secondary btn--small" onClick={() => startEdit(row)}>
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
