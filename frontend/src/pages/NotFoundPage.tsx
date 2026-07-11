import { ErrorPage } from '../components/ui/ErrorPage';

export function NotFoundPage() {
  return (
    <ErrorPage
      title="404 — Страница не найдена"
      message="Запрашиваемая страница не существует или была перемещена."
    />
  );
}
