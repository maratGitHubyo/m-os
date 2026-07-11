import { apiFetch } from '../client';
import type { GameSessionInfo } from '../../types';

export async function startSession(): Promise<GameSessionInfo> {
  return apiFetch<GameSessionInfo>('/api/admin/session/start', { method: 'POST' });
}

export async function pauseSession(): Promise<GameSessionInfo> {
  return apiFetch<GameSessionInfo>('/api/admin/session/pause', { method: 'POST' });
}

export async function finishSession(): Promise<GameSessionInfo> {
  return apiFetch<GameSessionInfo>('/api/admin/session/finish', { method: 'POST' });
}
