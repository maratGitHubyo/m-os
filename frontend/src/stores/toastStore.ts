import { useSyncExternalStore } from 'react';

export interface Toast {
  id: string;
  message: string;
  type: 'success' | 'error';
}

let toasts: Toast[] = [];
const listeners = new Set<() => void>();
let counter = 0;

function notify(): void {
  listeners.forEach((listener) => listener());
}

export function showToast(message: string, type: Toast['type'] = 'success'): void {
  const id = `toast-${++counter}`;
  toasts = [{ id, message, type }, ...toasts].slice(0, 5);
  notify();
  window.setTimeout(() => {
    toasts = toasts.filter((toast) => toast.id !== id);
    notify();
  }, 4000);
}

export function dismissToast(id: string): void {
  toasts = toasts.filter((toast) => toast.id !== id);
  notify();
}

export function getToasts(): Toast[] {
  return toasts;
}

export function subscribeToasts(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useToasts(): Toast[] {
  return useSyncExternalStore(subscribeToasts, getToasts, getToasts);
}
