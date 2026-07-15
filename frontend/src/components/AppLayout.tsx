import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { fetchCurrentSession } from '../api/locations';
import { EventNotifications } from './EventNotifications';
import { logout, useAuth } from '../stores/authStore';

const baseNavItems = [
  { to: '/', label: 'Главная', end: true },
  { to: '/map', label: 'Карта' },
  { to: '/scan', label: 'QR' },
  { to: '/secrets', label: 'Промокод' },
  { to: '/inventory', label: 'Инвентарь' },
  { to: '/wallet', label: 'Кошелёк' },
  { to: '/trades', label: 'Обмены' },
  { to: '/quests', label: 'Квесты' },
  { to: '/events', label: 'События' },
];

export function AppLayout() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [auctionEnabled, setAuctionEnabled] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      try {
        const session = await fetchCurrentSession();
        if (!cancelled) {
          setAuctionEnabled(Boolean(session.config?.auctionModeEnabled));
        }
      } catch {
        if (!cancelled) {
          setAuctionEnabled(false);
        }
      }
    };

    void load();
    const interval = window.setInterval(() => {
      void load();
    }, 15000);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, []);

  const navItems = auctionEnabled
    ? [...baseNavItems, { to: '/auction', label: 'Аукцион' }]
    : baseNavItems;

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__brand">
          <NavLink to="/">M-OS</NavLink>
          {user && <span className="app-header__user">{user.nickname}</span>}
        </div>
        <nav className="app-nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}
            >
              {item.label}
            </NavLink>
          ))}
          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              className={({ isActive }) => (isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link')}
            >
              Админ
            </NavLink>
          )}
          <button type="button" className="app-nav__logout" onClick={handleLogout}>
            Выйти
          </button>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <EventNotifications />
    </div>
  );
}
