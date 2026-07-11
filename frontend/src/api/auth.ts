import { apiFetch } from './client';
import type { LoginResponse } from '../types';

export interface LoginRequest {
  username: string;
  password: string;
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
