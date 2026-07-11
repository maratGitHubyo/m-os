import { apiFetch } from './client';
import type { CoinTransaction, Page, Wallet } from '../types';

export async function fetchMyWallet(): Promise<Wallet> {
  return apiFetch<Wallet>('/api/wallet/me');
}

export async function fetchMyTransactions(): Promise<Page<CoinTransaction>> {
  return apiFetch<Page<CoinTransaction>>('/api/wallet/me/transactions');
}
