import { ui } from '../../i18n/ru';

interface EmptyStateProps {
  message?: string;
  title?: string;
  children?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  message = ui.noData,
  title,
  children,
  className = '',
}: EmptyStateProps) {
  return (
    <div className={`empty-state-panel ${className}`.trim()}>
      <div className="empty-state-panel__glow" aria-hidden="true" />
      {title && <h2 className="empty-state-panel__title">{title}</h2>}
      <p className="empty-state">{message}</p>
      {children}
    </div>
  );
}
