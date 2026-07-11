import { Link } from 'react-router-dom';

export function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="app">
      <header className="app-header">
        <Link to="/">M-OS</Link>
        <nav className="app-nav">
          <Link to="/map">Map</Link>
        </nav>
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}
