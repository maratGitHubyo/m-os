import { dismissToast, useToasts } from '../../stores/toastStore';

export function ToastContainer() {
  const toasts = useToasts();

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="toast-container" aria-live="polite">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`toast toast--${toast.type}`}
          role="status"
        >
          <span>{toast.message}</span>
          <button
            type="button"
            className="toast__close"
            aria-label="Закрыть"
            onClick={() => dismissToast(toast.id)}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
