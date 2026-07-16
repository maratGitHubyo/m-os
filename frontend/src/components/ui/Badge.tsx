type BadgeTone =
  | 'default'
  | 'active'
  | 'running'
  | 'completed'
  | 'scheduled'
  | 'paused'
  | 'finished'
  | 'cancelled'
  | 'starting'
  | 'achieved'
  | 'pending'
  | 'accepted'
  | 'declined'
  | 'success'
  | 'warn'
  | 'danger';

interface BadgeProps {
  children: React.ReactNode;
  tone?: BadgeTone | string;
  className?: string;
}

export function Badge({ children, tone = 'default', className = '' }: BadgeProps) {
  const toneClass = tone && tone !== 'default' ? `badge--${tone.toLowerCase()}` : '';
  return <span className={`badge ${toneClass} ${className}`.trim()}>{children}</span>;
}
