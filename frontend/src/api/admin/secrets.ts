import { apiFetch } from '../client';
import type { CreatePlayerSecretRequest, PlayerSecretInfo } from '../../types/admin';

export async function createSecret(
  request: CreatePlayerSecretRequest,
): Promise<PlayerSecretInfo> {
  return apiFetch<PlayerSecretInfo>('/api/admin/secrets', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
