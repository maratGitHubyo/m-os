import { EmptyState } from './EmptyState';
import { Skeleton } from './Skeleton';

interface PageStateProps {
  loading?: boolean;
  loadingLabel?: string;
  error?: string | null;
  empty?: boolean;
  emptyMessage?: string;
  emptyTitle?: string;
  skeleton?: 'list' | 'card' | 'stat';
  skeletonCount?: number;
  children: React.ReactNode;
}

export function PageState({
  loading,
  loadingLabel,
  error,
  empty,
  emptyMessage,
  emptyTitle,
  skeleton = 'list',
  skeletonCount = 3,
  children,
}: PageStateProps) {
  if (loading) {
    return (
      <div className="page-state-loading" role="status" aria-live="polite" aria-label={loadingLabel ?? 'Загрузка'}>
        <span className="visually-hidden">{loadingLabel ?? 'Загрузка'}</span>
        <Skeleton variant={skeleton} count={skeletonCount} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="status-error-panel" role="alert">
        <p className="status-error">{error}</p>
      </div>
    );
  }

  if (empty) {
    return <EmptyState title={emptyTitle} message={emptyMessage} />;
  }

  return <>{children}</>;
}
