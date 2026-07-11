import { useEffect, useState } from 'react';
import { fetchCurrentSession, fetchLocations, getStoredToken, setStoredToken } from '../api/locations';
import { GameMap } from '../components/GameMap';
import type { LocationPoint } from '../types';

export function MapPage() {
  const [locations, setLocations] = useState<LocationPoint[]>([]);
  const [mapImageUrl, setMapImageUrl] = useState<string | null>(null);
  const [selected, setSelected] = useState<LocationPoint | null>(null);
  const [tokenInput, setTokenInput] = useState(getStoredToken() ?? '');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const loadMap = async (token: string) => {
    setLoading(true);
    setError(null);

    try {
      setStoredToken(token);
      const [session, points] = await Promise.all([
        fetchCurrentSession(),
        fetchLocations(),
      ]);
      setMapImageUrl(session.mapImageUrl);
      setLocations(points);
      setSelected(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load map');
      setLocations([]);
      setSelected(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const token = getStoredToken();
    if (token) {
      void loadMap(token);
    }
  }, []);

  const handleLoad = () => {
    if (!tokenInput.trim()) {
      setError('JWT token is required');
      return;
    }
    void loadMap(tokenInput.trim());
  };

  return (
    <section className="map-page">
      <h1>Map</h1>
      <p className="map-page__hint">
        Fog of war: hidden locations show zone and marker only until discovered.
      </p>

      <div className="map-page__auth">
        <input
          type="password"
          className="map-page__token-input"
          placeholder="Paste JWT token (login via API)"
          value={tokenInput}
          onChange={(event) => setTokenInput(event.target.value)}
        />
        <button type="button" className="map-page__load-btn" onClick={handleLoad} disabled={loading}>
          {loading ? 'Loading…' : 'Load map'}
        </button>
      </div>

      {error && <p className="status-error">{error}</p>}

      {locations.length > 0 && (
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
                      {known ? location.name : '? — ' + location.zone}
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
