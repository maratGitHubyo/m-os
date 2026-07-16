import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <header className="admin-page-header">
      <div className="admin-page-header__row">
        <div>
          <h1>{title}</h1>
          {description && <p className="page-hint">{description}</p>}
        </div>
        {actions}
      </div>
    </header>
  );
}
