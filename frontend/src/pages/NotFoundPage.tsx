import { ErrorPage } from '../components/ui/ErrorPage';

export function NotFoundPage() {
  return (
    <ErrorPage
      title="404 — Page not found"
      message="The page you are looking for does not exist or has been moved."
    />
  );
}
