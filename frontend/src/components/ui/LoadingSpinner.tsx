import { ui } from '../../i18n/ru';

interface LoadingSpinnerProps {
  label?: string;
}

export function LoadingSpinner({ label = ui.loading }: LoadingSpinnerProps) {
  return (
    <div className="loading-spinner" role="status" aria-live="polite">
      <div className="loading-spinner__icon" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}
