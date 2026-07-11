import { Link } from 'react-router-dom';
import { ui } from '../../i18n/ru';

interface ErrorPageProps {
  title: string;
  message: string;
  actionLabel?: string;
  actionTo?: string;
}

export function ErrorPage({
  title,
  message,
  actionLabel = ui.goHome,
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
