import { apiFetch } from '../client';
import type { Item } from '../../types';
import type {
  AdminGrantItemRequest,
  CreateItemTemplateRequest,
  ItemCollectionStatsResponse,
  ItemTemplate,
} from '../../types/admin';

export async function createItemTemplate(
  request: CreateItemTemplateRequest,
): Promise<ItemTemplate> {
  return apiFetch<ItemTemplate>('/api/admin/items/templates', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function listItemTemplates(): Promise<ItemTemplate[]> {
  return apiFetch<ItemTemplate[]>('/api/admin/items/templates');
}

export async function fetchCollectionStats(): Promise<ItemCollectionStatsResponse> {
  return apiFetch<ItemCollectionStatsResponse>('/api/admin/items/collection-stats');
}

export async function grantItem(request: AdminGrantItemRequest): Promise<Item> {
  return apiFetch<Item>('/api/admin/items/grant', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
