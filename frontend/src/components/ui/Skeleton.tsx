interface SkeletonProps {
  variant?: 'line' | 'card' | 'stat' | 'list';
  count?: number;
  className?: string;
}

export function Skeleton({ variant = 'list', count = 3, className = '' }: SkeletonProps) {
  if (variant === 'line') {
    return <div className={`skeleton skeleton--line ${className}`.trim()} aria-hidden="true" />;
  }

  if (variant === 'stat') {
    return (
      <div className={`skeleton-block skeleton-block--stat ${className}`.trim()} aria-hidden="true">
        <div className="skeleton skeleton--line skeleton--short" />
        <div className="skeleton skeleton--line skeleton--lg" />
      </div>
    );
  }

  if (variant === 'card') {
    return (
      <div className={`skeleton-block skeleton-block--card ${className}`.trim()} aria-hidden="true">
        <div className="skeleton skeleton--media" />
        <div className="skeleton skeleton--line" />
        <div className="skeleton skeleton--line skeleton--short" />
      </div>
    );
  }

  return (
    <div
      className={`skeleton-list ${className}`.trim()}
      role="status"
      aria-live="polite"
      aria-label="Загрузка"
    >
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="skeleton-block skeleton-block--card">
          <div className="skeleton skeleton--line" />
          <div className="skeleton skeleton--line skeleton--short" />
          <div className="skeleton skeleton--line skeleton--mid" />
        </div>
      ))}
    </div>
  );
}
