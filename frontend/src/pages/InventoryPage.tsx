import { useEffect, useMemo, useState } from 'react';
import { fetchInventory } from '../api/inventory';
import { PageHeader } from '../components/ui/PageHeader';
import { PageState } from '../components/ui/PageState';
import { formatEnum, itemRarity, translateError } from '../i18n/ru';
import type { Item, ItemRarity } from '../types';

const rarityFilters: Array<{ value: 'ALL' | 'LORE' | ItemRarity; label: string }> = [
  { value: 'ALL', label: 'Все' },
  { value: 'LORE', label: 'Лор' },
  { value: 'COMMON', label: 'Обычные' },
  { value: 'RARE', label: 'Редкие' },
  { value: 'EPIC', label: 'Эпические' },
  { value: 'LEGENDARY', label: 'Легендарные' },
];

export function InventoryPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rarityFilter, setRarityFilter] = useState<'ALL' | 'LORE' | ItemRarity>('ALL');
  const [previewItem, setPreviewItem] = useState<Item | null>(null);

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

  useEffect(() => {
    if (!previewItem) {
      return;
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setPreviewItem(null);
      }
    };

    document.addEventListener('keydown', onKeyDown);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = previousOverflow;
    };
  }, [previewItem]);

  const filteredItems = useMemo(() => {
    if (rarityFilter === 'ALL') {
      return items;
    }
    if (rarityFilter === 'LORE') {
      return items.filter((item) => Boolean(item.template.isLore));
    }
    return items.filter(
      (item) => !item.template.isLore && item.template.rarity === rarityFilter,
    );
  }, [items, rarityFilter]);

  return (
    <section className="inventory-page">
      <PageHeader
        title="Инвентарь"
        hint="Предметы, собранные в этой экспедиции. Нажмите на фото, чтобы увеличить."
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
            {filteredItems.map((item) => {
              const hasImage = Boolean(item.template.imageUrl);
              const isLore = Boolean(item.template.isLore);
              const description = isLore
                ? item.template.description ?? 'Текст откроется перед аукционом'
                : item.template.description ?? 'Нет описания';
              return (
                <li key={item.id} className={isLore ? 'item-card item-card--lore' : 'item-card'}>
                  {hasImage ? (
                    <button
                      type="button"
                      className="item-card__image-btn"
                      onClick={() => setPreviewItem(item)}
                      aria-label={`Открыть фото: ${item.template.name}`}
                    >
                      <img
                        className="item-card__image"
                        src={item.template.imageUrl!}
                        alt={item.template.name}
                      />
                    </button>
                  ) : (
                    <div className="item-card__placeholder item-card__placeholder--lore" aria-hidden="true">
                      {isLore ? 'Лор' : ''}
                    </div>
                  )}
                  <div className="item-card__body">
                    <div className="item-card__header">
                      <h2>{item.template.name}</h2>
                      <div className="item-card__badges">
                        {isLore && <span className="badge badge--lore">Лор</span>}
                        {!isLore && (
                          <span className={`rarity rarity--${item.template.rarity.toLowerCase()}`}>
                            {formatEnum(item.template.rarity, itemRarity)}
                          </span>
                        )}
                      </div>
                    </div>
                    <p>{description}</p>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </PageState>

      {previewItem?.template.imageUrl && (
        <div
          className="item-lightbox"
          role="dialog"
          aria-modal="true"
          aria-label={previewItem.template.name}
          onClick={() => setPreviewItem(null)}
        >
          <button
            type="button"
            className="item-lightbox__close"
            aria-label="Закрыть"
            onClick={() => setPreviewItem(null)}
          >
            ×
          </button>
          <figure
            className="item-lightbox__content"
            onClick={(event) => event.stopPropagation()}
          >
            <img
              className="item-lightbox__image"
              src={previewItem.template.imageUrl}
              alt={previewItem.template.name}
            />
            <figcaption className="item-lightbox__caption">
              <span>{previewItem.template.name}</span>
              {previewItem.template.isLore ? (
                <span className="badge badge--lore">Лор</span>
              ) : (
                <span className={`rarity rarity--${previewItem.template.rarity.toLowerCase()}`}>
                  {formatEnum(previewItem.template.rarity, itemRarity)}
                </span>
              )}
              {previewItem.template.isLore && (
                <p className="item-lightbox__lore-text">
                  {previewItem.template.description ?? 'Текст откроется перед аукционом'}
                </p>
              )}
            </figcaption>
          </figure>
        </div>
      )}
    </section>
  );
}
