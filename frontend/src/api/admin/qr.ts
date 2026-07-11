import { apiFetch } from '../client';
import type { CreateQrCodeRequest, QrCodeInfo } from '../../types/admin';

export async function createQrCode(request: CreateQrCodeRequest): Promise<QrCodeInfo> {
  return apiFetch<QrCodeInfo>('/api/admin/qr', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
