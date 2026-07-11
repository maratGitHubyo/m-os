import { apiFetch } from './client';
import type { Item } from '../types';

export async function fetchInventory(): Promise<Item[]> {
  return apiFetch<Item[]>('/api/inventory');
}
