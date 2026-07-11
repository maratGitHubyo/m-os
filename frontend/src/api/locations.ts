import { apiFetch } from './client';
import type { GameSessionInfo, LocationPoint } from '../types';

export async function fetchLocations(): Promise<LocationPoint[]> {
  return apiFetch<LocationPoint[]>('/api/locations');
}

export async function fetchCurrentSession(): Promise<GameSessionInfo> {
  return apiFetch<GameSessionInfo>('/api/session/current');
}
