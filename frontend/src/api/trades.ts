import { apiFetch } from './client';
import type { CreateTradeRequest, Trade } from '../types';

export async function fetchTrades(): Promise<Trade[]> {
  return apiFetch<Trade[]>('/api/trades');
}

export async function createTrade(request: CreateTradeRequest): Promise<Trade> {
  return apiFetch<Trade>('/api/trades', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function acceptTrade(tradeId: string): Promise<Trade> {
  return apiFetch<Trade>(`/api/trades/${tradeId}/accept`, { method: 'POST' });
}

export async function declineTrade(tradeId: string): Promise<Trade> {
  return apiFetch<Trade>(`/api/trades/${tradeId}/decline`, { method: 'POST' });
}

export async function cancelTrade(tradeId: string): Promise<Trade> {
  return apiFetch<Trade>(`/api/trades/${tradeId}/cancel`, { method: 'POST' });
}
