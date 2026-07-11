import { apiFetch } from './client';
import type { VictoryCondition } from '../types/victory';

const TOKEN_KEY = 'mos_token';

export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export async function fetchVictoryConditions(token: string): Promise<VictoryCondition[]> {
  return apiFetch<VictoryCondition[]>('/api/victory-conditions', {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}
