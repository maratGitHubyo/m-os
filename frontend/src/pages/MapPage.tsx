import { useEffect, useState } from 'react';
import { fetchCurrentSession, fetchLocations } from '../api/locations';
import { GameMap } from '../components/GameMap';
import { PageState } from '../components/ui/PageState';
import { translateError } from '../i18n/ru';
import type { LocationPoint } from '../types';

export function MapPage() {
  const [locations, setLocations] = useState<LocationPoint[]>([]);
  const [mapImageUrl, setMapImageUrl] = useState<string | null>(null);
  const [selected, setSelected] = useState<LocationPoint | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      setLoading(true);
      setError(null);

      try {
        const [session, points] = await Promise.all([
          fetchCurrentSession(),
          fetchLocations(),
        ]);

        if (!cancelled) {
          setMapImageUrl(session.mapImageUrl ?? null);
          setLocations(points);
          setSelected(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? translateError(err.message) : 'Не удалось загрузить карту',
          );
          setLocations([]);
          setSelected(null);
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
    <section className="map-page">
      <h1>Карта</h1>
      <p className="page-hint">
        Туман войны: скрытые локации показывают только зону и маркер, пока не будут открыты.
      </p>

      <PageState
        loading={loading}
        error={error}
        loadingLabel="Загрузка карты…"
        empty={locations.length === 0}
        emptyMessage="На карте пока нет локаций."
      >
        <>
          <GameMap
            locations={locations}
            mapImageUrl={mapImageUrl}
            selectedId={selected?.id ?? null}
            onSelect={setSelected}
          />

          {selected && (
            <article className="map-detail">
              <h2>
                {selected.discovered || !selected.hidden ? selected.name : 'Неизвестная локация'}
              </h2>
              <p className="map-detail__zone">Зона: {selected.zone}</p>
              <p className="map-detail__coords">
                Координаты: {selected.x.toFixed(1)}%, {selected.y.toFixed(1)}%
              </p>
              {selected.discovered || !selected.hidden ? (
                <p>{selected.description}</p>
              ) : (
                <p className="map-detail__fog">Эта локация ещё не открыта.</p>
              )}
            </article>
          )}

          <ul className="map-list">
            {locations.map((location) => {
              const known = location.discovered || !location.hidden;
              return (
                <li key={location.id}>
                  <button type="button" className="map-list__item" onClick={() => setSelected(location)}>
                    <span className={known ? 'map-list__name' : 'map-list__name map-list__name--unknown'}>
                      {known ? location.name : `? — ${location.zone}`}
                    </span>
                    <span className="map-list__coords">
                      {location.x.toFixed(0)}%, {location.y.toFixed(0)}%
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        </>
      </PageState>
    </section>
  );
}
