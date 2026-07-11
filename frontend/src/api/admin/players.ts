import { fetchLeaderboard } from '../score';
import type { AdminPlayerRow } from '../../types/admin';

export async function fetchAdminPlayers(): Promise<AdminPlayerRow[]> {
  const leaderboard = await fetchLeaderboard();
  return leaderboard.map((entry) => ({
    userId: entry.userId,
    nickname: entry.nickname,
    rank: entry.rank,
    totalScore: entry.points,
    username: null,
    role: null,
    active: null,
    coins: null,
  }));
}
