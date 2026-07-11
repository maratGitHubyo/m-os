import { apiFetch } from '../client';
import type { AdminScoreChangeRequest } from '../../types/admin';

export interface ScoreTransaction {
  id: string;
  userId: string;
  gameSessionId: string;
  category: AdminScoreChangeRequest['category'];
  delta: number;
  reason: string;
  createdAt: string;
}

export async function addPoints(
  userId: string,
  request: AdminScoreChangeRequest,
): Promise<ScoreTransaction> {
  return apiFetch<ScoreTransaction>(`/api/admin/score/${userId}/add`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function subtractPoints(
  userId: string,
  request: AdminScoreChangeRequest,
): Promise<ScoreTransaction> {
  return apiFetch<ScoreTransaction>(`/api/admin/score/${userId}/subtract`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
