import { apiFetch } from './client';
import type { LeaderboardEntry, Score } from '../types';

export async function fetchMyScores(): Promise<Score[]> {
  return apiFetch<Score[]>('/api/score/me');
}

export async function fetchLeaderboard(): Promise<LeaderboardEntry[]> {
  return apiFetch<LeaderboardEntry[]>('/api/leaderboard');
}
