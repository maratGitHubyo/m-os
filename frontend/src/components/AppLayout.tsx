import { Link } from 'react-router-dom';

export function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="app">
      <header className="app-header">
        <Link to="/">M-OS</Link>
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}
