import { useState } from 'react';
import { addPoints, subtractPoints } from '../../api/admin/score';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { AdminScoreChangeRequest } from '../../types/admin';

const categories: AdminScoreChangeRequest['category'][] = [
  'TOTAL',
  'EXPLORER',
  'COLLECTOR',
  'TRADER',
  'QUEST',
];

export function AdminScorePage() {
  const { players, loading, error } = useAdminPlayers();
  const [userId, setUserId] = useState('');
  const [category, setCategory] = useState<AdminScoreChangeRequest['category']>('TOTAL');
  const [points, setPoints] = useState('10');
  const [reason, setReason] = useState('Корректировка очков администратором');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = async (type: 'add' | 'subtract') => {
    if (!userId) {
      showToast('Выберите игрока', 'error');
      return;
    }

    const parsedPoints = Number(points);
    if (!parsedPoints || parsedPoints < 1) {
      showToast('Очки должны быть не менее 1', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const request: AdminScoreChangeRequest = {
        category,
        points: parsedPoints,
        reason,
      };
      if (type === 'add') {
        await addPoints(userId, request);
        showToast('Очки добавлены');
      } else {
        await subtractPoints(userId, request);
        showToast('Очки списаны');
      }
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Ошибка операции с очками',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader title="Очки" description="Добавление и списание очков для игроков." />

      <PageState loading={loading} error={error} empty={players.length === 0}>
        <FormCard title="Корректировка очков">
          <div className="admin-form-grid">
            <label className="form-field">
              <span>Игрок</span>
              <PlayerSelect players={players} value={userId} onChange={setUserId} required />
            </label>
            <label className="form-field">
              <span>Категория</span>
              <select
                className="form-select"
                value={category}
                onChange={(event) =>
                  setCategory(event.target.value as AdminScoreChangeRequest['category'])
                }
              >
                {categories.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </label>
            <label className="form-field">
              <span>Очки</span>
              <input
                type="number"
                min={1}
                value={points}
                onChange={(event) => setPoints(event.target.value)}
              />
            </label>
            <label className="form-field form-field--wide">
              <span>Причина</span>
              <input
                type="text"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
              />
            </label>
          </div>
          <div className="admin-actions">
            <button
              type="button"
              className="btn btn--primary"
              disabled={submitting}
              onClick={() => void handleChange('add')}
            >
              + Очки
            </button>
            <button
              type="button"
              className="btn btn--danger"
              disabled={submitting}
              onClick={() => void handleChange('subtract')}
            >
              − Очки
            </button>
          </div>
        </FormCard>
      </PageState>
    </section>
  );
}
