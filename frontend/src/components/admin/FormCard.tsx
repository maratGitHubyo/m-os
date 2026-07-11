interface FormCardProps {
  title: string;
  children: React.ReactNode;
}

export function FormCard({ title, children }: FormCardProps) {
  return (
    <article className="card form-card">
      <h2>{title}</h2>
      {children}
    </article>
  );
}
