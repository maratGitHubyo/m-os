import { apiFetch } from '../client';
import type { CreateCollectibleNumberRequest, CollectibleNumberInfo } from '../../types/admin';

export interface PlayerNumberGrant {
  id: string;
  userId: string;
  gameSessionId: string;
  numberValue: number;
  acquiredAt: string;
}

export async function createNumber(
  request: CreateCollectibleNumberRequest,
): Promise<CollectibleNumberInfo> {
  return apiFetch<CollectibleNumberInfo>('/api/admin/numbers', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function grantNumber(
  numberId: string,
  userId: string,
): Promise<PlayerNumberGrant> {
  return apiFetch<PlayerNumberGrant>(`/api/admin/numbers/${numberId}/grant/${userId}`, {
    method: 'POST',
  });
}
