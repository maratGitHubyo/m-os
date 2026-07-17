import { useEffect, useState } from 'react';
import { Button } from './ui/Button';

export function OfflineScreen() {
  const [offline, setOffline] = useState(
    typeof navigator !== 'undefined' ? !navigator.onLine : false,
  );

  useEffect(() => {
    const goOffline = () => setOffline(true);
    const goOnline = () => setOffline(false);

    window.addEventListener('offline', goOffline);
    window.addEventListener('online', goOnline);
    setOffline(!navigator.onLine);

    return () => {
      window.removeEventListener('offline', goOffline);
      window.removeEventListener('online', goOnline);
    };
  }, []);

  if (!offline) {
    return null;
  }

  return (
    <div className="offline-screen" role="alert">
      <div className="offline-screen__card">
        <div className="offline-screen__mark" aria-hidden="true">
          M
        </div>
        <h1>Нет подключения</h1>
        <p>Проверьте интернет и попробуйте снова.</p>
        <Button type="button" onClick={() => window.location.reload()}>
          Повторить
        </Button>
      </div>
    </div>
  );
}
