import { apiFetch } from '../client';
import type { Quest } from '../../types';
import type { CreateQuestRequest, UpdateQuestRequest } from '../../types/admin';

export async function fetchAdminQuests(): Promise<Quest[]> {
  return apiFetch<Quest[]>('/api/quests');
}

export async function createQuest(request: CreateQuestRequest): Promise<Quest> {
  return apiFetch<Quest>('/api/admin/quests', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function updateQuest(id: string, request: UpdateQuestRequest): Promise<Quest> {
  return apiFetch<Quest>(`/api/admin/quests/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}
