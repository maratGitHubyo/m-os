import { apiFetch } from '../client';
import type { AdminDashboardResponse } from '../../types/admin';

export async function fetchAdminDashboard(): Promise<AdminDashboardResponse> {
  return apiFetch<AdminDashboardResponse>('/api/admin/dashboard');
}
