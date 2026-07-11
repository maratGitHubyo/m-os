import { apiFetch } from './client';
import type { QrScanResponse } from '../types';

export async function scanQrCode(code: string): Promise<QrScanResponse> {
  return apiFetch<QrScanResponse>('/api/qr/scan', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}
