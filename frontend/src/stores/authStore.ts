import { useSyncExternalStore } from 'react';
import type { Session, User } from '../types';

const STORAGE_KEY = 'mos_auth';

export interface AuthState {
  token: string | null;
  user: User | null;
  session: Session | null;
}

function loadState(): AuthState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return { token: null, user: null, session: null };
    }
    const parsed = JSON.parse(raw) as AuthState;
    return {
      token: parsed.token ?? null,
      user: parsed.user ?? null,
      session: parsed.session ?? null,
    };
  } catch {
    return { token: null, user: null, session: null };
  }
}

let state: AuthState = loadState();
const listeners = new Set<() => void>();

function persistState(): void {
  if (state.token && state.user && state.session) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}

function setState(next: AuthState): void {
  state = next;
  persistState();
  listeners.forEach((listener) => listener());
}

export function getAuthState(): AuthState {
  return state;
}

export function getToken(): string | null {
  return state.token;
}

export function isAuthenticated(): boolean {
  return Boolean(state.token);
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function login(
  token: string,
  user: User,
  session: Session,
  username: string,
): void {
  setState({
    token,
    user: { ...user, username },
    session,
  });
}

export function logout(): void {
  setState({ token: null, user: null, session: null });
}

export function useAuth(): AuthState & { isAuthenticated: boolean } {
  const snapshot = useSyncExternalStore(subscribe, getAuthState, getAuthState);
  return {
    ...snapshot,
    isAuthenticated: Boolean(snapshot.token),
  };
}
