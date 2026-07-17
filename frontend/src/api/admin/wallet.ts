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

export interface AdminBulkCreditResponse {
  playerCount: number;
  amountPerPlayer: number;
  totalCredited: number;
}

export async function creditAllWallets(
  request: AdminWalletOperationRequest,
): Promise<AdminBulkCreditResponse> {
  return apiFetch<AdminBulkCreditResponse>('/api/admin/wallet/credit-all', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
