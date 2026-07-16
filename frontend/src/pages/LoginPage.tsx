import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { login as loginApi } from '../api/auth';
import { ApiError } from '../api/client';
import { Button } from '../components/ui/Button';
import { translateError } from '../i18n/ru';
import { login as saveAuth } from '../stores/authStore';

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await loginApi({ username, password });
      saveAuth(response.token, response.user, response.session, username);
      const from =
        typeof location.state === 'object' &&
        location.state !== null &&
        'from' in location.state &&
        typeof (location.state as { from?: unknown }).from === 'string'
          ? (location.state as { from: string }).from
          : '/';
      navigate(from || '/', { replace: true });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError(translateError('Invalid username or password'));
      } else {
        setError(
          err instanceof Error ? translateError(err.message) : 'Не удалось войти',
        );
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    document.title = 'Вход — M-OS';
  }, []);

  return (
    <div className="login-page">
      <div className="login-card">
        <p className="login-card__status">
          <span className="login-card__status-dot" aria-hidden="true" />
          signal online · expedition gate
        </p>
        <h1>M-OS</h1>
        <p className="login-card__subtitle">Войдите в терминал экспедиции</p>

        <form className="login-form" onSubmit={(event) => void handleSubmit(event)}>
          <label className="form-field">
            <span>Логин</span>
            <input
              type="text"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              required
            />
          </label>

          <label className="form-field">
            <span>Пароль</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </label>

          {error && (
            <p className="status-error" role="alert">
              {error}
            </p>
          )}

          <Button type="submit" disabled={loading}>
            {loading ? 'Подключение…' : 'Войти в игру'}
          </Button>
        </form>

        <p className="login-card__hint">Логин и пароль выдаёт организатор</p>
      </div>
    </div>
  );
}
