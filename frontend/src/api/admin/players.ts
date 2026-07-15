import { fetchLeaderboard } from './leaderboard';
import type { AdminPlayerRow } from '../../types/admin';

export async function fetchAdminPlayers(): Promise<AdminPlayerRow[]> {
  const leaderboard = await fetchLeaderboard();
  return leaderboard.map((entry) => ({
    userId: entry.userId,
    nickname: entry.nickname,
    rank: entry.rank,
    balance: entry.balance,
    username: null,
    role: null,
    active: null,
  }));
}
