import { apiFetch } from './client';
import type { VictoryCondition } from '../types';

export async function fetchVictoryConditions(): Promise<VictoryCondition[]> {
  return apiFetch<VictoryCondition[]>('/api/victory-conditions');
}
