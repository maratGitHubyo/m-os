import type { LocationPoint } from '../types';

interface MapMarkerProps {
  location: LocationPoint;
  selected: boolean;
  onSelect: (location: LocationPoint) => void;
}

export function MapMarker({ location, selected, onSelect }: MapMarkerProps) {
  const known = location.discovered || !location.hidden;
  const label = known ? location.name ?? 'Неизвестно' : '?';

  return (
    <button
      type="button"
      className={`map-marker ${known ? 'map-marker--known' : 'map-marker--unknown'} ${selected ? 'map-marker--selected' : ''}`}
      style={{ left: `${location.x}%`, top: `${location.y}%` }}
      title={known ? location.name ?? undefined : `${location.zone} — не открыта`}
      aria-label={
        known ? location.name ?? 'Локация' : `Неоткрытая локация в зоне ${location.zone}`
      }
      onClick={(event) => {
        event.stopPropagation();
        onSelect(location);
      }}
    >
      <span className="map-marker__dot" />
      <span className="map-marker__label">{label}</span>
    </button>
  );
}

export interface MapDraftPoint {
  x: number;
  y: number;
}

interface GameMapProps {
  locations: LocationPoint[];
  mapImageUrl?: string | null;
  selectedId?: string | null;
  onSelect?: (location: LocationPoint) => void;
  draftPoint?: MapDraftPoint | null;
  onMapClick?: (point: MapDraftPoint) => void;
  clickToPlace?: boolean;
}

function clampPercent(value: number): number {
  return Math.min(100, Math.max(0, value));
}

export function GameMap({
  locations,
  mapImageUrl,
  selectedId = null,
  onSelect,
  draftPoint = null,
  onMapClick,
  clickToPlace = false,
}: GameMapProps) {
  const handleOverlayClick = (event: React.MouseEvent<HTMLDivElement>) => {
    if (!onMapClick) {
      return;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    if (rect.width <= 0 || rect.height <= 0) {
      return;
    }
    const x = clampPercent(((event.clientX - rect.left) / rect.width) * 100);
    const y = clampPercent(((event.clientY - rect.top) / rect.height) * 100);
    onMapClick({
      x: Math.round(x * 10) / 10,
      y: Math.round(y * 10) / 10,
    });
  };

  return (
    <div className={`game-map ${clickToPlace ? 'game-map--editable' : ''}`}>
      {mapImageUrl ? (
        <img className="game-map__image" src={mapImageUrl} alt="Карта дачи" draggable={false} />
      ) : (
        <div className="game-map__placeholder" aria-hidden="true" />
      )}
      <div
        className="game-map__overlay"
        onClick={onMapClick ? handleOverlayClick : undefined}
        role={onMapClick ? 'presentation' : undefined}
      >
        {locations.map((location) => (
          <MapMarker
            key={location.id}
            location={location}
            selected={selectedId === location.id}
            onSelect={onSelect ?? (() => undefined)}
          />
        ))}
        {draftPoint && (
          <div
            className="map-marker map-marker--draft"
            style={{ left: `${draftPoint.x}%`, top: `${draftPoint.y}%` }}
            aria-label="Новая точка"
          >
            <span className="map-marker__dot" />
            <span className="map-marker__label">Новая</span>
          </div>
        )}
      </div>
    </div>
  );
}
