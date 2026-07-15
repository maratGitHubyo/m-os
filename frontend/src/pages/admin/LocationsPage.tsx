import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  createLocation,
  deleteLocation,
  fetchAdminLocations,
  updateLocation,
} from '../../api/admin/locations';
import { fetchCurrentSession } from '../../api/locations';
import { GameMap, type MapDraftPoint } from '../../components/GameMap';
import { ConfirmDialog } from '../../components/admin/ConfirmDialog';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { translateError, ui } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { LocationPoint } from '../../types';

const DEFAULT_MAP = '/maps/dacha.png';

export function LocationsPage() {
  const [locations, setLocations] = useState<LocationPoint[]>([]);
  const [mapImageUrl, setMapImageUrl] = useState(DEFAULT_MAP);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draft, setDraft] = useState<MapDraftPoint | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingDelete, setPendingDelete] = useState<LocationPoint | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [zone, setZone] = useState('участок');
  const [hidden, setHidden] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [data, session] = await Promise.all([fetchAdminLocations(), fetchCurrentSession()]);
      setLocations(data);
      setMapImageUrl(session.mapImageUrl ?? DEFAULT_MAP);
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

  const displayLocations = useMemo(() => {
    if (!editingId || !draft) {
      return locations;
    }
    return locations.map((location) =>
      location.id === editingId ? { ...location, x: draft.x, y: draft.y } : location,
    );
  }, [locations, editingId, draft]);

  const resetForm = () => {
    setEditingId(null);
    setDraft(null);
    setName('');
    setDescription('');
    setZone('участок');
    setHidden(false);
  };

  const startEdit = (location: LocationPoint) => {
    setEditingId(location.id);
    setDraft({ x: location.x, y: location.y });
    setName(location.name ?? '');
    setDescription(location.description ?? '');
    setZone(location.zone);
    setHidden(location.hidden);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!draft) {
      showToast('Сначала нажмите точку на карте', 'error');
      return;
    }
    if (!name.trim()) {
      showToast('Введите название точки', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        name: name.trim(),
        description: description.trim() || name.trim(),
        zone: zone.trim() || 'участок',
        x: draft.x,
        y: draft.y,
        hidden,
      };

      if (editingId) {
        await updateLocation(editingId, payload);
        showToast('Локация обновлена');
      } else {
        await createLocation(payload);
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

  const confirmDelete = async () => {
    if (!pendingDelete) {
      return;
    }
    setDeleting(true);
    try {
      await deleteLocation(pendingDelete.id);
      showToast('Точка удалена');
      if (editingId === pendingDelete.id) {
        resetForm();
      }
      setPendingDelete(null);
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось удалить точку',
        'error',
      );
    } finally {
      setDeleting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Локации"
        description="Нажмите на карту, подпишите точку и сохраните — без ручного ввода координат."
      />

      <PageState loading={loading} error={error} loadingLabel="Загрузка карты…">
        <div className="admin-map-editor">
          <div className="admin-map-editor__map">
            <p className="page-hint">
              {editingId
                ? 'Редактирование: клик по карте перемещает точку.'
                : 'Клик по карте — новая точка. Клик по маркеру — редактировать.'}
            </p>
            <GameMap
              locations={displayLocations}
              mapImageUrl={mapImageUrl}
              selectedId={editingId}
              onSelect={startEdit}
              draftPoint={!editingId ? draft : null}
              onMapClick={setDraft}
              clickToPlace
            />
          </div>

          <FormCard title={editingId ? 'Редактировать точку' : 'Новая точка'}>
            <form className="admin-form-grid" onSubmit={(event) => void handleSubmit(event)}>
              <div className="form-field form-field--wide">
                <span>Координаты</span>
                <p className="admin-map-editor__coords">
                  {draft
                    ? `${draft.x.toFixed(1)}% × ${draft.y.toFixed(1)}%`
                    : 'Нажмите на карту слева'}
                </p>
              </div>
              <label className="form-field form-field--wide">
                <span>Название</span>
                <input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Например: Баня, беседка…"
                  required
                />
              </label>
              <label className="form-field form-field--wide">
                <span>Описание (необязательно)</span>
                <input
                  value={description}
                  onChange={(event) => setDescription(event.target.value)}
                  placeholder="Короткая подсказка для гостей"
                />
              </label>
              <label className="form-field">
                <span>Зона</span>
                <input
                  value={zone}
                  onChange={(event) => setZone(event.target.value)}
                  placeholder="участок"
                />
              </label>
              <label className="form-field form-field--checkbox">
                <input
                  type="checkbox"
                  checked={hidden}
                  onChange={(event) => setHidden(event.target.checked)}
                />
                <span>Скрытая (туман войны)</span>
              </label>
              <div className="admin-actions">
                <button
                  type="submit"
                  className="btn btn--primary"
                  disabled={submitting || !draft}
                >
                  {editingId ? ui.save : 'Добавить точку'}
                </button>
                {(editingId || draft || name) && (
                  <button type="button" className="btn btn--secondary" onClick={resetForm}>
                    {ui.cancel}
                  </button>
                )}
                {editingId && (
                  <button
                    type="button"
                    className="btn btn--danger"
                    disabled={submitting || deleting}
                    onClick={() => {
                      const location = locations.find((item) => item.id === editingId);
                      if (location) {
                        setPendingDelete(location);
                      }
                    }}
                  >
                    Удалить
                  </button>
                )}
              </div>
            </form>
          </FormCard>
        </div>

        <h2 className="section-title">Все точки ({locations.length})</h2>
        {locations.length === 0 ? (
          <p className="empty-state">Пока пусто — поставьте первую точку кликом по карте.</p>
        ) : (
          <ul className="admin-map-editor__list">
            {locations.map((location) => (
              <li key={location.id} className="admin-map-editor__list-row">
                <button
                  type="button"
                  className={`admin-map-editor__list-item ${editingId === location.id ? 'admin-map-editor__list-item--active' : ''}`}
                  onClick={() => startEdit(location)}
                >
                  <strong>{location.name ?? '(без названия)'}</strong>
                  <span>
                    {location.zone} · {location.x.toFixed(0)}%, {location.y.toFixed(0)}%
                    {location.hidden ? ' · скрытая' : ''}
                  </span>
                </button>
                <button
                  type="button"
                  className="btn btn--danger btn--small"
                  disabled={deleting}
                  onClick={() => setPendingDelete(location)}
                >
                  Удалить
                </button>
              </li>
            ))}
          </ul>
        )}
      </PageState>

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Удалить точку?"
        message={
          pendingDelete
            ? `Точка «${pendingDelete.name ?? 'без названия'}» будет удалена с карты.`
            : ''
        }
        confirmLabel={deleting ? 'Удаление…' : 'Удалить'}
        confirmVariant="danger"
        onCancel={() => {
          if (!deleting) {
            setPendingDelete(null);
          }
        }}
        onConfirm={() => void confirmDelete()}
      />
    </section>
  );
}
