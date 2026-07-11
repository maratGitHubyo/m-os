import { apiFetch } from '../client';
import type { VictoryCondition } from '../../types';
import type {
  CreateVictoryConditionRequest,
  VictoryAdminStatusResponse,
} from '../../types/admin';

export async function fetchVictoryAdminStatus(): Promise<VictoryAdminStatusResponse> {
  return apiFetch<VictoryAdminStatusResponse>('/api/admin/victory-conditions/status');
}

export async function fetchVictoryConditions(): Promise<VictoryCondition[]> {
  return apiFetch<VictoryCondition[]>('/api/victory-conditions');
}

export async function createVictoryCondition(
  request: CreateVictoryConditionRequest,
): Promise<VictoryCondition> {
  return apiFetch<VictoryCondition>('/api/admin/victory-conditions', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
