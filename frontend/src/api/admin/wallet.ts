import { apiFetch } from '../client';
import type { CoinTransaction } from '../../types';
import type { AdminWalletOperationRequest } from '../../types/admin';

export async function creditWallet(
  userId: string,
  request: AdminWalletOperationRequest,
): Promise<CoinTransaction> {
  return apiFetch<CoinTransaction>(`/api/admin/wallet/${userId}/credit`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function debitWallet(
  userId: string,
  request: AdminWalletOperationRequest,
): Promise<CoinTransaction> {
  return apiFetch<CoinTransaction>(`/api/admin/wallet/${userId}/debit`, {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
