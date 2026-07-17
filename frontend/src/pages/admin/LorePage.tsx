import { useCallback, useEffect, useState } from 'react';
import { fetchLoreStats, seedLore, setLoreRevealed } from '../../api/admin/lore';
import { DataTable } from '../../components/admin/DataTable';
import { FormCard } from '../../components/admin/FormCard';
import { PageHeader } from '../../components/admin/PageHeader';
import { PageState } from '../../components/admin/PageState';
import { translateError } from '../../i18n/ru';
import { showToast } from '../../stores/toastStore';
import type { LoreStatsResponse } from '../../types/admin';

export function LorePage() {
  const [stats, setStats] = useState<LoreStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setStats(await fetchLoreStats());
    } catch (err) {
      setError(err instanceof Error ? translateError(err.message) : 'Не удалось загрузить лор');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const toggleReveal = async () => {
    if (!stats) {
      return;
    }
    setSubmitting(true);
    try {
      const updated = await setLoreRevealed(!stats.loreRevealed);
      setStats({ ...stats, loreRevealed: updated.loreRevealed });
      showToast(updated.loreRevealed ? 'Лор раскрыт для владельцев' : 'Лор снова скрыт');
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось переключить лор',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleSeed = async () => {
    setSubmitting(true);
    try {
      const result = await seedLore();
      showToast(
        `Лор засеян: шаблоны +${result.templatesCreated}, коды +${result.secretsCreated}` +
          (result.locationsUpdated ? `, с карты убрано промокодов: ${result.locationsUpdated}` : ''),
      );
      await load();
    } catch (err) {
      showToast(
        err instanceof Error ? translateError(err.message) : 'Не удалось засеять лор',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <PageHeader
        title="Лор"
        description="Восемь фрагментов истории. По умолчанию текст скрыт — раскройте перед аукционом. Промокоды печатаются и кладутся на точки карты вручную."
      />

      <FormCard title="Управление">
        <div className="admin-actions">
          <button
            type="button"
            className="btn btn--primary"
            disabled={!stats || submitting}
            onClick={() => void toggleReveal()}
          >
            {stats?.loreRevealed ? 'Скрыть лор' : 'Раскрыть лор'}
          </button>
          <button
            type="button"
            className="btn btn--secondary"
            disabled={submitting}
            onClick={() => void handleSeed()}
          >
            Создать / обновить lore promos
          </button>
        </div>
        {stats && (
          <p className="muted">
            Статус: {stats.loreRevealed ? 'текст открыт владельцам' : 'текст запечатан'}. Найдено{' '}
            {stats.foundCount} из {stats.totalFragments}. С фрагментом: {stats.playersWithLore}, без:{' '}
            {stats.playersWithoutLore}.
          </p>
        )}
      </FormCard>

      <FormCard title="Промокоды для печати">
        <p className="muted">
          Распечатайте коды и положите на точки карты (Lor1…Lor8). На карте в приложении коды не
          показываются.
        </p>
        {stats ? (
          <ul className="lore-print-codes">
            {stats.fragments.map((fragment, index) => (
              <li key={fragment.code}>
                <strong>Lor{index + 1}</strong> · <strong>{fragment.code}</strong> → {fragment.name}
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">Загрузка…</p>
        )}
      </FormCard>

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка статистики лора…"
        empty={!stats || stats.fragments.length === 0}
        emptyTitle="Нет фрагментов"
        emptyMessage="Нажмите «Создать / обновить lore promos», чтобы засеять 8 фрагментов и промокоды."
      >
        {stats && (
          <DataTable
            rows={stats.fragments}
            rowKey={(row) => row.code}
            columns={[
              { key: 'name', header: 'Фрагмент', render: (row) => row.name },
              { key: 'code', header: 'Код', render: (row) => row.code },
              {
                key: 'status',
                header: 'Статус',
                render: (row) => (row.found ? 'Найден' : 'Не найден'),
              },
              {
                key: 'owner',
                header: 'Владелец',
                render: (row) => row.ownerNickname ?? '—',
              },
            ]}
          />
        )}
      </PageState>
    </section>
  );
}
