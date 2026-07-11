import { apiFetch } from '../client';
import type { CreateQrCodeRequest, CreateQrCodeSimpleRequest, QrCodeInfo } from '../../types/admin';

export async function createQrCode(request: CreateQrCodeRequest): Promise<QrCodeInfo> {
  return apiFetch<QrCodeInfo>('/api/admin/qr', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export async function createQrCodeSimple(
  request: CreateQrCodeSimpleRequest,
): Promise<QrCodeInfo> {
  return apiFetch<QrCodeInfo>('/api/admin/qr/simple', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
