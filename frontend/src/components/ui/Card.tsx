interface CardProps {
  children: React.ReactNode;
  className?: string;
  wide?: boolean;
  as?: 'article' | 'div' | 'section';
}

export function Card({ children, className = '', wide, as: Tag = 'article' }: CardProps) {
  const classes = ['card', wide ? 'card--wide' : '', className].filter(Boolean).join(' ');
  return <Tag className={classes}>{children}</Tag>;
}

interface StatCardProps {
  label: string;
  value: React.ReactNode;
  children?: React.ReactNode;
  className?: string;
}

export function StatCard({ label, value, children, className = '' }: StatCardProps) {
  return (
    <Card className={`stat-card ${className}`.trim()}>
      <p className="stat-card__label">{label}</p>
      <p className="stat-card__value">{value}</p>
      {children}
    </Card>
  );
}
