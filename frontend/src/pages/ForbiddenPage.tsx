import { ErrorPage } from '../components/ui/ErrorPage';

export function ForbiddenPage() {
  return (
    <ErrorPage
      title="403 — Доступ запрещён"
      message="У вас нет прав для просмотра этой страницы."
    />
  );
}
