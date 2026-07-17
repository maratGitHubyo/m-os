import { useEffect, useState } from 'react';
import { Button } from './ui/Button';
import { NotificationService } from '../services/NotificationService';

/**
 * Asks for Notification permission via a user gesture (required on iOS).
 * Hidden when unsupported, already granted, or denied.
 */
export function EnableNotificationsButton() {
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!('Notification' in window)) {
      setVisible(false);
      return;
    }
    setVisible(Notification.permission === 'default');
  }, []);

  if (!visible) {
    return null;
  }

  const handleEnable = async () => {
    setBusy(true);
    try {
      const permission = await NotificationService.requestPermission();
      setVisible(permission === 'default');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="notify-enable">
      <Button
        type="button"
        variant="secondary"
        size="small"
        className="notify-enable__button"
        disabled={busy}
        onClick={() => void handleEnable()}
      >
        {busy ? 'Запрос…' : 'Включить уведомления'}
      </Button>
    </div>
  );
}
