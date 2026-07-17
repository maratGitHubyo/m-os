import { useEffect, useState } from 'react';

const SPLASH_MS = 2200;
const SPLASH_SEEN_KEY = 'mos.splashSeen';

function isStandaloneDisplay(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const media = window.matchMedia('(display-mode: standalone)').matches;
  const iosStandalone =
    'standalone' in navigator &&
    Boolean((navigator as Navigator & { standalone?: boolean }).standalone);
  return media || iosStandalone;
}

export function SplashScreen() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (!isStandaloneDisplay()) {
      return;
    }

    try {
      if (sessionStorage.getItem(SPLASH_SEEN_KEY) === '1') {
        return;
      }
      sessionStorage.setItem(SPLASH_SEEN_KEY, '1');
    } catch {
      // show splash anyway
    }

    setVisible(true);
    const timer = window.setTimeout(() => setVisible(false), SPLASH_MS);
    return () => window.clearTimeout(timer);
  }, []);

  if (!visible) {
    return null;
  }

  return (
    <div className="splash-screen" role="status" aria-live="polite">
      <div className="splash-screen__logo" aria-hidden="true">
        <img src="/icons/logo-192.png" alt="" width={96} height={96} />
      </div>
      <p className="splash-screen__brand">M-OS</p>
      <p className="splash-screen__loading">Loading...</p>
    </div>
  );
}
