import { apiFetch } from '../client';
import type {
  LoreRevealResponse,
  LoreSeedResponse,
  LoreStatsResponse,
} from '../../types/admin';

export async function fetchLoreStats(): Promise<LoreStatsResponse> {
  return apiFetch<LoreStatsResponse>('/api/admin/lore');
}

export async function setLoreRevealed(revealed: boolean): Promise<LoreRevealResponse> {
  return apiFetch<LoreRevealResponse>('/api/admin/lore/reveal', {
    method: 'POST',
    body: JSON.stringify({ revealed }),
  });
}

export async function seedLore(): Promise<LoreSeedResponse> {
  return apiFetch<LoreSeedResponse>('/api/admin/lore/seed', {
    method: 'POST',
  });
}
