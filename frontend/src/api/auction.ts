import { apiFetch } from './client';
import type { AuctionState, AuctionLot } from '../types';

export async function fetchAuctionState(): Promise<AuctionState> {
  return apiFetch<AuctionState>('/api/auction');
}

export async function placeBid(lotId: string, amount: number): Promise<AuctionLot> {
  return apiFetch<AuctionLot>(`/api/auction/lots/${lotId}/bids`, {
    method: 'POST',
    body: JSON.stringify({ amount }),
  });
}
