import type { LocationPoint } from '../types';

interface MapMarkerProps {
  location: LocationPoint;
  selected: boolean;
  onSelect: (location: LocationPoint) => void;
}

export function MapMarker({ location, selected, onSelect }: MapMarkerProps) {
  const known = location.discovered || !location.hidden;
  const label = known ? location.name ?? 'Unknown' : '?';

  return (
    <button
      type="button"
      className={`map-marker ${known ? 'map-marker--known' : 'map-marker--unknown'} ${selected ? 'map-marker--selected' : ''}`}
      style={{ left: `${location.x}%`, top: `${location.y}%` }}
      title={known ? location.name ?? undefined : `${location.zone} — undiscovered`}
      aria-label={known ? location.name ?? 'Location' : `Undiscovered location in ${location.zone}`}
      onClick={() => onSelect(location)}
    >
      <span className="map-marker__dot" />
      <span className="map-marker__label">{label}</span>
    </button>
  );
}

interface GameMapProps {
  locations: LocationPoint[];
  mapImageUrl?: string | null;
  selectedId: string | null;
  onSelect: (location: LocationPoint) => void;
}

export function GameMap({ locations, mapImageUrl, selectedId, onSelect }: GameMapProps) {
  return (
    <div className="game-map">
      {mapImageUrl ? (
        <img className="game-map__image" src={mapImageUrl} alt="Game map" />
      ) : (
        <div className="game-map__placeholder" aria-hidden="true" />
      )}
      <div className="game-map__overlay">
        {locations.map((location) => (
          <MapMarker
            key={location.id}
            location={location}
            selected={selectedId === location.id}
            onSelect={onSelect}
          />
        ))}
      </div>
    </div>
  );
}
