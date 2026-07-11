import { useState } from 'react';
import { addPoints, subtractPoints } from '../../api/admin/score';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { PlayerSelect } from '../../components/admin/PlayerSelect';
import { useAdminPlayers } from '../../hooks/useAdminPlayers';
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
  const [reason, setReason] = useState('Admin score adjustment');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = async (type: 'add' | 'subtract') => {
    if (!userId) {
      showToast('Select a player', 'error');
      return;
    }

    const parsedPoints = Number(points);
    if (!parsedPoints || parsedPoints < 1) {
      showToast('Points must be at least 1', 'error');
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
        showToast('Points added');
      } else {
        await subtractPoints(userId, request);
        showToast('Points subtracted');
      }
    } catch (err) {
      showToast(err instanceof Error ? err.message : 'Score operation failed', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader title="Score" description="Add or subtract points for players." />

      <PageState loading={loading} error={error} empty={players.length === 0}>
        <FormCard title="Score adjustment">
          <div className="admin-form-grid">
            <label className="form-field">
              <span>Player</span>
              <PlayerSelect players={players} value={userId} onChange={setUserId} required />
            </label>
            <label className="form-field">
              <span>Category</span>
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
              <span>Points</span>
              <input
                type="number"
                min={1}
                value={points}
                onChange={(event) => setPoints(event.target.value)}
              />
            </label>
            <label className="form-field form-field--wide">
              <span>Reason</span>
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
              + Points
            </button>
            <button
              type="button"
              className="btn btn--danger"
              disabled={submitting}
              onClick={() => void handleChange('subtract')}
            >
              − Points
            </button>
          </div>
        </FormCard>
      </PageState>
    </section>
  );
}
