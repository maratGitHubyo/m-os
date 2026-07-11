export interface HealthResponse {
  status: string;
}

export interface LocationPoint {
  id: string;
  zone: string;
  x: number;
  y: number;
  hidden: boolean;
  discovered: boolean;
  name: string | null;
  description: string | null;
}

export interface GameSessionInfo {
  id: string;
  name: string;
  mapImageUrl: string | null;
}
