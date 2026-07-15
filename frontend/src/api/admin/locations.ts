import { apiFetch } from '../client';
import type { LocationPoint } from '../../types';
import type {
  AdminLocationPoint,
  CreateLocationRequest,
  UpdateLocationRequest,
} from '../../types/admin';

export async function fetchAdminLocations(): Promise<LocationPoint[]> {
  return apiFetch<LocationPoint[]>('/api/locations');
}

export async function createLocation(
  request: CreateLocationRequest,
): Promise<AdminLocationPoint> {
  return apiFetch<AdminLocationPoint>('/api/admin/locations', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function updateLocation(
  id: string,
  request: UpdateLocationRequest,
): Promise<AdminLocationPoint> {
  return apiFetch<AdminLocationPoint>(`/api/admin/locations/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}

export async function deleteLocation(id: string): Promise<void> {
  await apiFetch<void>(`/api/admin/locations/${id}`, {
    method: 'DELETE',
  });
}
