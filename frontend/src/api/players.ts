import { apiFetch } from './client';
import type { SessionPlayer } from '../types';

export async function fetchSessionPlayers(): Promise<SessionPlayer[]> {
  return apiFetch<SessionPlayer[]>('/api/players');
}
