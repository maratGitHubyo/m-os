import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import type { HealthResponse } from '../types';

export function HomePage() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<HealthResponse>('/actuator/health')
      .then(setHealth)
      .catch((err: Error) => setError(err.message));
  }, []);

  return (
    <section className="home">
      <h1>M-OS</h1>
      <p>Marat Operating System — игровая система для дня рождения</p>
      <p className="status-label">Backend status:</p>
      {health && <p className="status-ok">{health.status}</p>}
      {error && <p className="status-error">{error}</p>}
      {!health && !error && <p className="status-loading">Checking...</p>}
    </section>
  );
}
