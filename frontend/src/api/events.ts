import { apiFetch } from './client';
import type { GameEvent } from '../types';

export async function fetchEvents(): Promise<GameEvent[]> {
  return apiFetch<GameEvent[]>('/api/events');
}
