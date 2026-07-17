import { useEffect, useId, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { fetchCurrentSession } from '../api/locations';
import { EventNotifications } from './EventNotifications';
import { InstallAppButton } from './InstallAppButton';
import { logout, useAuth } from '../stores/authStore';

type NavItem = {
  to: string;
  label: string;
  end?: boolean;
  icon: 'home' | 'map' | 'qr' | 'bag' | 'wallet' | 'trade' | 'quest' | 'event' | 'promo' | 'auction' | 'admin' | 'more';
};

const primaryNav: NavItem[] = [
  { to: '/', label: 'Главная', end: true, icon: 'home' },
  { to: '/map', label: 'Карта', icon: 'map' },
  { to: '/scan', label: 'QR', icon: 'qr' },
  { to: '/inventory', label: 'Инвентарь', icon: 'bag' },
];

const secondaryNav: NavItem[] = [
  { to: '/secrets', label: 'Промокод', icon: 'promo' },
  { to: '/wallet', label: 'Кошелёк', icon: 'wallet' },
  { to: '/trades', label: 'Обмены', icon: 'trade' },
  { to: '/quests', label: 'Квесты', icon: 'quest' },
  { to: '/events', label: 'События', icon: 'event' },
];

function NavIcon({ name }: { name: NavItem['icon'] }) {
  const common = {
    width: 22,
    height: 22,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 1.75,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true as const,
  };

  switch (name) {
    case 'home':
      return (
        <svg {...common}>
          <path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6H10v6H5a1 1 0 0 1-1-1v-9.5z" />
        </svg>
      );
    case 'map':
      return (
        <svg {...common}>
          <path d="M9 4 3 6.5V19l6-2.5 6 2.5 6-2.5V4.5L15 7 9 4z" />
          <path d="M9 4v12.5M15 7v12.5" />
        </svg>
      );
    case 'qr':
      return (
        <svg {...common}>
          <path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4z" />
          <path d="M14 14h3v3M20 14v6h-6M17 20h3" />
        </svg>
      );
    case 'bag':
      return (
        <svg {...common}>
          <path d="M6 8h12l-1 12H7L6 8z" />
          <path d="M9 8V6a3 3 0 0 1 6 0v2" />
        </svg>
      );
    case 'wallet':
      return (
        <svg {...common}>
          <path d="M3 7.5A1.5 1.5 0 0 1 4.5 6H19a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4.5A1.5 1.5 0 0 1 3 17.5v-10z" />
          <path d="M16 13h4" />
        </svg>
      );
    case 'trade':
      return (
        <svg {...common}>
          <path d="M7 7h11l-3-3M17 17H6l3 3" />
          <path d="M18 7v4M6 17v-4" />
        </svg>
      );
    case 'quest':
      return (
        <svg {...common}>
          <path d="M12 3 5 6v6c0 4.2 2.9 7.6 7 9 4.1-1.4 7-4.8 7-9V6l-7-3z" />
          <path d="M9.5 12.5 11 14l3.5-3.5" />
        </svg>
      );
    case 'event':
      return (
        <svg {...common}>
          <path d="M8 3v3M16 3v3M4 8h16M5 6h14a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1z" />
        </svg>
      );
    case 'promo':
      return (
        <svg {...common}>
          <path d="M4 9h11l5 3-5 3H4V9z" />
          <path d="M8 12h.01" />
        </svg>
      );
    case 'auction':
      return (
        <svg {...common}>
          <path d="M12 3 7 12h10L12 3zM8 20h8M12 12v8" />
        </svg>
      );
    case 'admin':
      return (
        <svg {...common}>
          <circle cx="12" cy="8" r="3.5" />
          <path d="M5 19.5c1.5-3.2 4-4.5 7-4.5s5.5 1.3 7 4.5" />
        </svg>
      );
    case 'more':
      return (
        <svg {...common}>
          <circle cx="6" cy="12" r="1.5" fill="currentColor" stroke="none" />
          <circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none" />
          <circle cx="18" cy="12" r="1.5" fill="currentColor" stroke="none" />
        </svg>
      );
    default:
      return null;
  }
}

export function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const [auctionEnabled, setAuctionEnabled] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const moreTitleId = useId();

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

  useEffect(() => {
    setMoreOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!moreOpen) {
      return;
    }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMoreOpen(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [moreOpen]);

  const auctionItem: NavItem | null = auctionEnabled
    ? { to: '/auction', label: 'Аукцион', icon: 'auction' }
    : null;

  const desktopNav: NavItem[] = [
    ...primaryNav,
    ...secondaryNav,
    ...(auctionItem ? [auctionItem] : []),
  ];

  const moreNav: NavItem[] = [
    ...secondaryNav,
    ...(auctionItem ? [auctionItem] : []),
  ];

  const morePaths = moreNav.map((item) => item.to);
  const moreActive =
    morePaths.some((path) => location.pathname === path || location.pathname.startsWith(`${path}/`)) ||
    location.pathname.startsWith('/admin');

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__brand">
          <NavLink to="/" className="app-header__logo" end>
            M-OS
          </NavLink>
          {user && (
            <span className="app-header__user" title={user.username}>
              <span className="app-header__user-dot" aria-hidden="true" />
              {user.nickname}
            </span>
          )}
          <InstallAppButton />
        </div>
        <nav className="app-nav app-nav--desktop" aria-label="Основная навигация">
          {desktopNav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link'
              }
            >
              {item.label}
            </NavLink>
          ))}
          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                isActive ? 'app-nav__link app-nav__link--active' : 'app-nav__link'
              }
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

      <nav className="bottom-nav" aria-label="Мобильная навигация">
        {primaryNav.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              isActive ? 'bottom-nav__item bottom-nav__item--active' : 'bottom-nav__item'
            }
          >
            <NavIcon name={item.icon} />
            <span>{item.label}</span>
          </NavLink>
        ))}
        <button
          type="button"
          className={
            moreOpen || moreActive
              ? 'bottom-nav__item bottom-nav__item--active'
              : 'bottom-nav__item'
          }
          aria-expanded={moreOpen}
          aria-controls="app-more-drawer"
          onClick={() => setMoreOpen((open) => !open)}
        >
          <NavIcon name="more" />
          <span>Ещё</span>
        </button>
      </nav>

      {moreOpen && (
        <div className="more-drawer-backdrop" onClick={() => setMoreOpen(false)}>
          <div
            id="app-more-drawer"
            className="more-drawer"
            role="dialog"
            aria-modal="true"
            aria-labelledby={moreTitleId}
            onClick={(event) => event.stopPropagation()}
          >
            <div className="more-drawer__handle" aria-hidden="true" />
            <h2 id={moreTitleId} className="more-drawer__title">
              Меню экспедиции
            </h2>
            <ul className="more-drawer__list">
              {moreNav.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    className={({ isActive }) =>
                      isActive
                        ? 'more-drawer__link more-drawer__link--active'
                        : 'more-drawer__link'
                    }
                    onClick={() => setMoreOpen(false)}
                  >
                    <NavIcon name={item.icon} />
                    <span>{item.label}</span>
                  </NavLink>
                </li>
              ))}
              {user?.role === 'ADMIN' && (
                <li>
                  <NavLink
                    to="/admin"
                    className={({ isActive }) =>
                      isActive
                        ? 'more-drawer__link more-drawer__link--active'
                        : 'more-drawer__link'
                    }
                    onClick={() => setMoreOpen(false)}
                  >
                    <NavIcon name="admin" />
                    <span>Админ</span>
                  </NavLink>
                </li>
              )}
            </ul>
            <button type="button" className="more-drawer__logout" onClick={handleLogout}>
              Выйти из системы
            </button>
          </div>
        </div>
      )}

      <EventNotifications />
    </div>
  );
}
