import { apiFetch } from './client';
import type { CoinTransaction, CoinTransfer, CreateCoinTransferRequest, Page, Wallet } from '../types';

export async function fetchMyWallet(): Promise<Wallet> {
  return apiFetch<Wallet>('/api/wallet/me');
}

export async function fetchMyTransactions(): Promise<Page<CoinTransaction>> {
  return apiFetch<Page<CoinTransaction>>('/api/wallet/me/transactions');
}

export async function transferCoins(request: CreateCoinTransferRequest): Promise<CoinTransfer> {
  return apiFetch<CoinTransfer>('/api/wallet/transfer', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function fetchTransfers(): Promise<Page<CoinTransfer>> {
  return apiFetch<Page<CoinTransfer>>('/api/wallet/transfers');
}
