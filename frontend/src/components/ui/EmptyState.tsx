import { ui } from '../../i18n/ru';

interface EmptyStateProps {
  message?: string;
  children?: React.ReactNode;
}

export function EmptyState({ message = ui.noData, children }: EmptyStateProps) {
  return (
    <div className="empty-state-panel">
      <p className="empty-state">{message}</p>
      {children}
    </div>
  );
}
