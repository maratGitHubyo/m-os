import { useEffect, useMemo, useState } from 'react';
import { fetchInventory } from '../api/inventory';
import { PageHeader } from '../components/ui/PageHeader';
import { PageState } from '../components/ui/PageState';
import { formatEnum, itemRarity, translateError } from '../i18n/ru';
import type { Item, ItemRarity } from '../types';

const rarityFilters: Array<{ value: 'ALL' | ItemRarity; label: string }> = [
  { value: 'ALL', label: 'Все' },
  { value: 'COMMON', label: 'Обычные' },
  { value: 'RARE', label: 'Редкие' },
  { value: 'EPIC', label: 'Эпические' },
  { value: 'LEGENDARY', label: 'Легендарные' },
];

export function InventoryPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rarityFilter, setRarityFilter] = useState<'ALL' | ItemRarity>('ALL');

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchInventory();
        if (!cancelled) {
          setItems(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? translateError(err.message) : 'Не удалось загрузить инвентарь',
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  const filteredItems = useMemo(() => {
    if (rarityFilter === 'ALL') {
      return items;
    }
    return items.filter((item) => item.template.rarity === rarityFilter);
  }, [items, rarityFilter]);

  return (
    <section className="inventory-page">
      <PageHeader
        title="Инвентарь"
        hint="Предметы, собранные в этой экспедиции."
      />

      {!loading && !error && items.length > 0 && (
        <div className="inventory-filters" role="group" aria-label="Фильтр по редкости">
          {rarityFilters.map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={
                rarityFilter === filter.value
                  ? 'filter-chip filter-chip--active'
                  : 'filter-chip'
              }
              aria-pressed={rarityFilter === filter.value}
              onClick={() => setRarityFilter(filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </div>
      )}

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка инвентаря…"
        empty={items.length === 0}
        emptyTitle="Пустой рюкзак"
        emptyMessage="Инвентарь пуст. Ищите QR и промокоды на локациях."
        skeleton="card"
        skeletonCount={4}
      >
        {filteredItems.length === 0 ? (
          <p className="empty-state">Нет предметов этой редкости.</p>
        ) : (
          <ul className="item-grid">
            {filteredItems.map((item) => (
              <li key={item.id} className="item-card">
                {item.template.imageUrl ? (
                  <img
                    className="item-card__image"
                    src={item.template.imageUrl}
                    alt={item.template.name}
                  />
                ) : (
                  <div className="item-card__placeholder" aria-hidden="true" />
                )}
                <div className="item-card__body">
                  <div className="item-card__header">
                    <h2>{item.template.name}</h2>
                    <span className={`rarity rarity--${item.template.rarity.toLowerCase()}`}>
                      {formatEnum(item.template.rarity, itemRarity)}
                    </span>
                  </div>
                  <p>{item.template.description ?? 'Нет описания'}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </PageState>
    </section>
  );
}
