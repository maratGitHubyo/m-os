import { API_URL, apiFetch } from './client';
import { getToken } from '../stores/authStore';
import type { QrScanResponse, QrScanResultResponse } from '../types';

const UUID_PATTERN =
  /([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})/i;

export function extractPublicIdFromQrText(text: string): string | null {
  const trimmed = text.trim();
  if (!trimmed) {
    return null;
  }

  const urlMatch = trimmed.match(/\/qr\/([0-9a-f-]{36})/i);
  if (urlMatch) {
    return urlMatch[1];
  }

  const deepLinkMatch = trimmed.match(/^mos:\/\/qr\/([0-9a-f-]{36})/i);
  if (deepLinkMatch) {
    return deepLinkMatch[1];
  }

  const uuidMatch = trimmed.match(UUID_PATTERN);
  if (uuidMatch) {
    return uuidMatch[1];
  }

  return null;
}

export async function scanQrCode(code: string): Promise<QrScanResponse> {
  return apiFetch<QrScanResponse>('/api/qr/scan', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export async function scanQrByPublicId(publicId: string): Promise<QrScanResultResponse> {
  return apiFetch<QrScanResultResponse>(`/api/qr/scan/${publicId}`, {
    method: 'POST',
  });
}

export async function fetchQrImageBlob(qrId: string): Promise<Blob> {
  const token = getToken();
  const response = await fetch(`${API_URL}/api/admin/qr/${qrId}/image`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });

  if (!response.ok) {
    throw new Error('Failed to load QR image');
  }

  return response.blob();
}

export function openQrImage(qrId: string): void {
  const token = getToken();
  const url = `${API_URL}/api/admin/qr/${qrId}/image`;
  if (!token) {
    window.open(url, '_blank', 'noopener,noreferrer');
    return;
  }

  void fetchQrImageBlob(qrId).then((blob) => {
    const objectUrl = URL.createObjectURL(blob);
    window.open(objectUrl, '_blank', 'noopener,noreferrer');
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  });
}

export async function downloadQrImage(qrId: string, filename: string): Promise<void> {
  const blob = await fetchQrImageBlob(qrId);
  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(objectUrl);
}
