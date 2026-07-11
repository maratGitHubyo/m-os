interface EmptyStateProps {
  message?: string;
  children?: React.ReactNode;
}

export function EmptyState({ message = 'No data available.', children }: EmptyStateProps) {
  return (
    <div className="empty-state-panel">
      <p className="empty-state">{message}</p>
      {children}
    </div>
  );
}
