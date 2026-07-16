import { apiFetch, API_URL } from '../client';
import { getToken } from '../../stores/authStore';
import type { CreateQrCodeRequest, CreateQrCodeSimpleRequest, QrCodeInfo } from '../../types/admin';

export async function listQrCodes(): Promise<QrCodeInfo[]> {
  return apiFetch<QrCodeInfo[]>('/api/admin/qr');
}

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

export async function downloadQrPrintDocx(): Promise<void> {
  const token = getToken();
  const response = await fetch(`${API_URL}/api/admin/qr/print-docx`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!response.ok) {
    throw new Error('Failed to download QR Word document');
  }
  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = 'mos-qr-print.docx';
  link.click();
  URL.revokeObjectURL(objectUrl);
}
