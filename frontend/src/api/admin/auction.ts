import { apiFetch } from '../client';
import type { AuctionLot, AuctionState, CreateAuctionLotRequest } from '../../types';

export async function fetchAdminAuctionState(): Promise<AuctionState> {
  return apiFetch<AuctionState>('/api/admin/auction');
}

export async function setAuctionMode(enabled: boolean): Promise<AuctionState> {
  return apiFetch<AuctionState>('/api/admin/auction/mode', {
    method: 'POST',
    body: JSON.stringify({ enabled }),
  });
}

export async function createAuctionLot(request: CreateAuctionLotRequest): Promise<AuctionLot> {
  return apiFetch<AuctionLot>('/api/admin/auction/lots', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function openAuctionLot(lotId: string): Promise<AuctionLot> {
  return apiFetch<AuctionLot>(`/api/admin/auction/lots/${lotId}/open`, { method: 'POST' });
}

export async function sellAuctionLot(lotId: string): Promise<AuctionLot> {
  return apiFetch<AuctionLot>(`/api/admin/auction/lots/${lotId}/sell`, { method: 'POST' });
}

export async function cancelAuctionLot(lotId: string): Promise<AuctionLot> {
  return apiFetch<AuctionLot>(`/api/admin/auction/lots/${lotId}/cancel`, { method: 'POST' });
}
