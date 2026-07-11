import { Link } from 'react-router-dom';

interface ErrorPageProps {
  title: string;
  message: string;
  actionLabel?: string;
  actionTo?: string;
}

export function ErrorPage({
  title,
  message,
  actionLabel = 'Go to dashboard',
  actionTo = '/',
}: ErrorPageProps) {
  return (
    <div className="error-page">
      <h1>{title}</h1>
      <p>{message}</p>
      <Link to={actionTo} className="btn">
        {actionLabel}
      </Link>
    </div>
  );
}
