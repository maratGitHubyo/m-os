import { ErrorPage } from '../components/ui/ErrorPage';

export function ForbiddenPage() {
  return (
    <ErrorPage
      title="403 — Access denied"
      message="You do not have permission to view this page."
    />
  );
}
