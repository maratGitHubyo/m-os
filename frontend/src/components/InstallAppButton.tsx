import { useEffect, useState } from 'react';
import { Button } from './ui/Button';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
}

function isIosDevice(): boolean {
  if (typeof navigator === 'undefined') {
    return false;
  }
  const ua = navigator.userAgent;
  const iOS = /iPad|iPhone|iPod/.test(ua);
  const iPadOs = navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1;
  return iOS || iPadOs;
}

function isStandaloneDisplay(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const media = window.matchMedia('(display-mode: standalone)').matches;
  const iosStandalone = 'standalone' in navigator && Boolean((navigator as Navigator & { standalone?: boolean }).standalone);
  return media || iosStandalone;
}

export function InstallAppButton() {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [installed, setInstalled] = useState(false);
  const [showIosHint, setShowIosHint] = useState(false);

  useEffect(() => {
    if (isStandaloneDisplay()) {
      setInstalled(true);
      return;
    }

    const onBeforeInstall = (event: Event) => {
      event.preventDefault();
      setDeferredPrompt(event as BeforeInstallPromptEvent);
    };

    const onInstalled = () => {
      setInstalled(true);
      setDeferredPrompt(null);
      setShowIosHint(false);
    };

    window.addEventListener('beforeinstallprompt', onBeforeInstall);
    window.addEventListener('appinstalled', onInstalled);

    if (isIosDevice()) {
      setShowIosHint(true);
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
      window.removeEventListener('appinstalled', onInstalled);
    };
  }, []);

  if (installed) {
    return null;
  }

  const handleInstall = async () => {
    if (!deferredPrompt) {
      return;
    }
    await deferredPrompt.prompt();
    const choice = await deferredPrompt.userChoice;
    if (choice.outcome === 'accepted') {
      setInstalled(true);
    }
    setDeferredPrompt(null);
  };

  if (deferredPrompt) {
    return (
      <div className="install-app">
        <Button
          type="button"
          variant="secondary"
          size="small"
          className="install-app__button"
          onClick={() => void handleInstall()}
        >
          Установить приложение
        </Button>
      </div>
    );
  }

  if (showIosHint) {
    return (
      <div className="install-app install-app--ios" role="note">
        <p className="install-app__hint">
          Установить на iPhone: <strong>Поделиться → На экран Домой</strong>
        </p>
      </div>
    );
  }

  return null;
}
