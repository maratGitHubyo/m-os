interface PageStateProps {
  loading?: boolean;
  error?: string | null;
  empty?: boolean;
  emptyMessage?: string;
  children: React.ReactNode;
}

export function PageState({
  loading,
  error,
  empty,
  emptyMessage = 'No data available.',
  children,
}: PageStateProps) {
  if (loading) {
    return <p className="status-loading">Loading…</p>;
  }

  if (error) {
    return <p className="status-error">{error}</p>;
  }

  if (empty) {
    return <p className="empty-state">{emptyMessage}</p>;
  }

  return <>{children}</>;
}
