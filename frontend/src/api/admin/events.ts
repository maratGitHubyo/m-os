import { apiFetch } from '../client';
import type { GameEvent } from '../../types';
import type { CreateGameEventRequest, UpdateGameEventStatusRequest } from '../../types/admin';

export async function fetchAdminEvents(): Promise<GameEvent[]> {
  return apiFetch<GameEvent[]>('/api/events');
}

export async function createEvent(request: CreateGameEventRequest): Promise<GameEvent> {
  return apiFetch<GameEvent>('/api/admin/events', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function updateEventStatus(
  id: string,
  request: UpdateGameEventStatusRequest,
): Promise<GameEvent> {
  return apiFetch<GameEvent>(`/api/admin/events/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}
