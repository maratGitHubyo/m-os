interface PageHeaderProps {
  title: string;
  description?: string;
}

export function PageHeader({ title, description }: PageHeaderProps) {
  return (
    <header className="admin-page-header">
      <h1>{title}</h1>
      {description && <p className="page-hint">{description}</p>}
    </header>
  );
}
