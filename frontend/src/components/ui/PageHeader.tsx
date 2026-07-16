interface PageHeaderProps {
  title: string;
  hint?: string;
  children?: React.ReactNode;
}

export function PageHeader({ title, hint, children }: PageHeaderProps) {
  return (
    <header className="page-header">
      <div className="page-header__text">
        <h1 className="page-header__title">{title}</h1>
        {hint && <p className="page-hint">{hint}</p>}
      </div>
      {children && <div className="page-header__actions">{children}</div>}
    </header>
  );
}
