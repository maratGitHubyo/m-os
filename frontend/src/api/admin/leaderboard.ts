import { apiFetch } from '../client';
import type { LeaderboardEntry } from '../../types';

export async function fetchLeaderboard(): Promise<LeaderboardEntry[]> {
  return apiFetch<LeaderboardEntry[]>('/api/leaderboard');
}
