import { useEffect, useState } from 'react';
import { fetchInventory } from '../api/inventory';
import { PageState } from '../components/ui/PageState';
import type { Item } from '../types';

export function InventoryPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

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
          setError(err instanceof Error ? err.message : 'Failed to load inventory');
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

  return (
    <section className="inventory-page">
      <h1>Inventory</h1>
      <p className="page-hint">Items you have collected in this session.</p>

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Loading inventory…"
        empty={items.length === 0}
        emptyMessage="Your inventory is empty."
      >
        <ul className="item-grid">
          {items.map((item) => (
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
                    {item.template.rarity}
                  </span>
                </div>
                <p>{item.template.description ?? 'No description'}</p>
              </div>
            </li>
          ))}
        </ul>
      </PageState>
    </section>
  );
}
