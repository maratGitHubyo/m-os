import { apiFetch } from './client';
import type { Quest, QuestProgress } from '../types';

export async function fetchQuests(): Promise<Quest[]> {
  return apiFetch<Quest[]>('/api/quests');
}

export async function fetchMyQuests(): Promise<QuestProgress[]> {
  return apiFetch<QuestProgress[]>('/api/quests/me');
}

export async function startQuest(questId: string): Promise<QuestProgress> {
  const response = await apiFetch<{ playerQuest: QuestProgress }>(`/api/quests/${questId}/start`, {
    method: 'POST',
  });
  return response.playerQuest;
}

export async function completeQuest(
  questId: string,
  note?: string,
): Promise<QuestProgress> {
  return apiFetch<QuestProgress>(`/api/quests/${questId}/complete`, {
    method: 'POST',
    body: JSON.stringify({ note: note?.trim() || null }),
  });
}
