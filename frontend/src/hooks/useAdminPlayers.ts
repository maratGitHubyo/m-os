import { useCallback, useEffect, useState } from 'react';
import { fetchAdminPlayers } from '../api/admin/players';
import { translateError } from '../i18n/ru';
import type { AdminPlayerRow } from '../types/admin';

export function useAdminPlayers() {
  const [players, setPlayers] = useState<AdminPlayerRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminPlayers();
      setPlayers(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? translateError(err.message)
          : 'Не удалось загрузить игроков',
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  return { players, loading, error, reload };
}
