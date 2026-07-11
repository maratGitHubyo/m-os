import { NavLink, Outlet } from 'react-router-dom';

const adminNavItems = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/session', label: 'Session' },
  { to: '/admin/players', label: 'Players' },
  { to: '/admin/wallet', label: 'Wallet' },
  { to: '/admin/score', label: 'Score' },
  { to: '/admin/items', label: 'Items' },
  { to: '/admin/locations', label: 'Locations' },
  { to: '/admin/qr', label: 'QR Codes' },
  { to: '/admin/secrets', label: 'Secrets' },
  { to: '/admin/numbers', label: 'Numbers' },
  { to: '/admin/quests', label: 'Quests' },
  { to: '/admin/victory', label: 'Victory' },
  { to: '/admin/events', label: 'Events' },
  { to: '/admin/audit', label: 'Audit' },
];

export function AdminLayout() {
  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <h2 className="admin-sidebar__title">Admin Panel</h2>
        <NavLink to="/" className="admin-sidebar__back">
          ← Back to game
        </NavLink>
        <nav className="admin-sidebar__nav">
          {adminNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                isActive ? 'admin-sidebar__link admin-sidebar__link--active' : 'admin-sidebar__link'
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="admin-content">
        <Outlet />
      </div>
    </div>
  );
}
