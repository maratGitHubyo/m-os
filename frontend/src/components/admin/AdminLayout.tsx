import { NavLink, Outlet } from 'react-router-dom';

const adminNavItems = [
  { to: '/admin', label: 'Панель', end: true },
  { to: '/admin/session', label: 'Сессия' },
  { to: '/admin/players', label: 'Игроки' },
  { to: '/admin/wallet', label: 'Кошелёк' },
  { to: '/admin/items', label: 'Предметы' },
  { to: '/admin/locations', label: 'Локации' },
  { to: '/admin/qr', label: 'QR-коды' },
  { to: '/admin/secrets', label: 'Промокод' },
  { to: '/admin/numbers', label: 'Числа' },
  { to: '/admin/quests', label: 'Квесты' },
  { to: '/admin/events', label: 'События' },
  { to: '/admin/audit', label: 'Аудит' },
];

export function AdminLayout() {
  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <h2 className="admin-sidebar__title">Панель администратора</h2>
        <NavLink to="/" className="admin-sidebar__back">
          ← Вернуться в игру
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
