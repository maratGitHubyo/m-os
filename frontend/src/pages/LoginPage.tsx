import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login as loginApi } from '../api/auth';
import { ApiError } from '../api/client';
import { translateError } from '../i18n/ru';
import { login as saveAuth } from '../stores/authStore';

export function LoginPage() {
  const navigate = useNavigate();
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
      navigate('/', { replace: true });
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
        <h1>M-OS</h1>
        <p className="login-card__subtitle">Войдите, чтобы присоединиться к игре</p>

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

          {error && <p className="status-error">{error}</p>}

          <button type="submit" className="btn btn--primary" disabled={loading}>
            {loading ? 'Вход…' : 'Войти'}
          </button>
        </form>

        <p className="login-card__hint">Демо: alice/demo123 или admin/admin123</p>
      </div>
    </div>
  );
}
