import { apiFetch } from '../client';
import type { Quest } from '../../types';
import type { CreateQuestRequest, UpdateQuestRequest } from '../../types/admin';

export async function fetchAdminQuests(): Promise<Quest[]> {
  return apiFetch<Quest[]>('/api/admin/quests');
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

export async function closeIncompleteQuests(): Promise<{
  success: boolean;
  closedPlayerQuests: number;
  message: string;
}> {
  return apiFetch('/api/admin/quests/close-incomplete', {
    method: 'POST',
  });
}

export interface BroadcastQuestsResponse {
  playerCount: number;
  questsCreated: number;
  countPerPlayer: number;
}

export interface QuestAutoDistributeStatus {
  enabled: boolean;
  lastDistributedAt: string | null;
  nextDistributionAt: string | null;
  autoCount: number;
  intervalMinutes: number;
}

export async function broadcastQuests(count: number): Promise<BroadcastQuestsResponse> {
  return apiFetch<BroadcastQuestsResponse>('/api/admin/quests/broadcast', {
    method: 'POST',
    body: JSON.stringify({ count }),
  });
}

export async function fetchQuestAutoDistribute(): Promise<QuestAutoDistributeStatus> {
  return apiFetch<QuestAutoDistributeStatus>('/api/admin/quests/auto-distribute');
}

export async function startQuestAutoDistribute(): Promise<QuestAutoDistributeStatus> {
  return apiFetch<QuestAutoDistributeStatus>('/api/admin/quests/auto-distribute/start', {
    method: 'POST',
  });
}

export async function stopQuestAutoDistribute(): Promise<QuestAutoDistributeStatus> {
  return apiFetch<QuestAutoDistributeStatus>('/api/admin/quests/auto-distribute/stop', {
    method: 'POST',
  });
}
