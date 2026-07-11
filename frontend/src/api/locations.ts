import { apiFetch } from './client';
import type { GameSessionInfo, LocationPoint } from '../types';

const TOKEN_KEY = 'mos_token';

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

function authHeaders(): HeadersInit {
  const token = getStoredToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function fetchLocations(): Promise<LocationPoint[]> {
  return apiFetch<LocationPoint[]>('/api/locations', {
    headers: authHeaders(),
  });
}

export async function fetchCurrentSession(): Promise<GameSessionInfo> {
  return apiFetch<GameSessionInfo>('/api/session/current', {
    headers: authHeaders(),
  });
}
