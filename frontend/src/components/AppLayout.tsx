import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { EventNotifications } from './EventNotifications';
import { logout, useAuth } from '../stores/authStore';

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/map', label: 'Map' },
  { to: '/inventory', label: 'Inventory' },
  { to: '/wallet', label: 'Wallet' },
  { to: '/quests', label: 'Quests' },
  { to: '/score', label: 'Leaderboard' },
  { to: '/victory', label: 'Victory' },
  { to: '/events', label: 'Events' },
];

export function AppLayout() {
  const navigate = useNavigate();
  const { user } = useAuth();

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
          <button type="button" className="app-nav__logout" onClick={handleLogout}>
            Logout
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
