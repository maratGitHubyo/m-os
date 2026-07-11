import { apiFetch } from './client';
import type { SecretRedeemResponse } from '../types';

export async function redeemSecret(code: string): Promise<SecretRedeemResponse> {
  return apiFetch<SecretRedeemResponse>('/api/secrets/redeem', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}
