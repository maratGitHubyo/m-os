import { useEffect, useState } from 'react';
import { fetchCurrentSession, fetchLocations } from '../api/locations';
import { GameMap } from '../components/GameMap';
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
          setError(err instanceof Error ? err.message : 'Failed to load map');
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
      <h1>Map</h1>
      <p className="page-hint">
        Fog of war: hidden locations show zone and marker only until discovered.
      </p>

      {loading && <p className="status-loading">Loading map…</p>}
      {error && <p className="status-error">{error}</p>}

      {!loading && !error && locations.length === 0 && (
        <p className="empty-state">No locations on the map yet.</p>
      )}

      {!loading && !error && locations.length > 0 && (
        <>
          <GameMap
            locations={locations}
            mapImageUrl={mapImageUrl}
            selectedId={selected?.id ?? null}
            onSelect={setSelected}
          />

          {selected && (
            <article className="map-detail">
              <h2>{selected.discovered || !selected.hidden ? selected.name : 'Unknown location'}</h2>
              <p className="map-detail__zone">Zone: {selected.zone}</p>
              <p className="map-detail__coords">
                Coordinates: {selected.x.toFixed(1)}%, {selected.y.toFixed(1)}%
              </p>
              {selected.discovered || !selected.hidden ? (
                <p>{selected.description}</p>
              ) : (
                <p className="map-detail__fog">This location has not been discovered yet.</p>
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
      )}
    </section>
  );
}
