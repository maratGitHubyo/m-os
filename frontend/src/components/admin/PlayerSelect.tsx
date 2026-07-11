import type { AdminPlayerRow } from '../../types/admin';

interface PlayerSelectProps {
  players: AdminPlayerRow[];
  value: string;
  onChange: (userId: string) => void;
  required?: boolean;
}

export function PlayerSelect({ players, value, onChange, required }: PlayerSelectProps) {
  return (
    <select
      className="form-select"
      value={value}
      required={required}
      onChange={(event) => onChange(event.target.value)}
    >
      <option value="">Выберите игрока…</option>
      {players.map((player) => (
        <option key={player.userId} value={player.userId}>
          {player.nickname} ({player.userId.slice(0, 8)}…)
        </option>
      ))}
    </select>
  );
}
