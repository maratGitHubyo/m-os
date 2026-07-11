import { LoadingSpinner } from './LoadingSpinner';
import { EmptyState } from './EmptyState';

interface PageStateProps {
  loading?: boolean;
  loadingLabel?: string;
  error?: string | null;
  empty?: boolean;
  emptyMessage?: string;
  children: React.ReactNode;
}

export function PageState({
  loading,
  loadingLabel,
  error,
  empty,
  emptyMessage,
  children,
}: PageStateProps) {
  if (loading) {
    return <LoadingSpinner label={loadingLabel} />;
  }

  if (error) {
    return (
      <div className="status-error-panel">
        <p className="status-error">{error}</p>
      </div>
    );
  }

  if (empty) {
    return <EmptyState message={emptyMessage} />;
  }

  return <>{children}</>;
}
