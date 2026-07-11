import { apiFetch } from '../client';
import type { Page } from '../../types';
import type { AuditAction, AuditLogEntry } from '../../types/admin';

export interface AuditQuery {
  userId?: string;
  action?: AuditAction;
  page?: number;
  size?: number;
}

export async function fetchAuditLogs(query: AuditQuery = {}): Promise<Page<AuditLogEntry>> {
  const params = new URLSearchParams();
  if (query.userId) {
    params.set('userId', query.userId);
  }
  if (query.action) {
    params.set('action', query.action);
  }
  params.set('page', String(query.page ?? 0));
  params.set('size', String(query.size ?? 20));
  params.set('sort', 'createdAt,desc');

  return apiFetch<Page<AuditLogEntry>>(`/api/admin/audit?${params.toString()}`);
}
