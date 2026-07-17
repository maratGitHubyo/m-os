const PERMISSION_KEY = 'mos.notificationPermission';
const SW_PATH = '/sw.js';

let registrationPromise: Promise<ServiceWorkerRegistration | null> | null = null;

function persistPermission(permission: NotificationPermission): void {
  try {
    localStorage.setItem(PERMISSION_KEY, permission);
  } catch {
    // ignore quota / private mode
  }
}

export function getStoredPermission(): NotificationPermission | null {
  try {
    const value = localStorage.getItem(PERMISSION_KEY);
    if (value === 'granted' || value === 'denied' || value === 'default') {
      return value;
    }
  } catch {
    // ignore
  }
  return null;
}

export async function registerServiceWorker(): Promise<ServiceWorkerRegistration | null> {
  if (!('serviceWorker' in navigator)) {
    return null;
  }

  if (!registrationPromise) {
    registrationPromise = navigator.serviceWorker
      .register(SW_PATH, { scope: '/' })
      .then((registration) => registration)
      .catch((error) => {
        console.warn('[PWA] Service worker registration failed', error);
        registrationPromise = null;
        return null;
      });
  }

  return registrationPromise;
}

export const NotificationService = {
  async requestPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      persistPermission('denied');
      return 'denied';
    }

    await registerServiceWorker();

    let permission = Notification.permission;
    if (permission === 'default') {
      permission = await Notification.requestPermission();
    }

    persistPermission(permission);
    return permission;
  },

  isGranted(): boolean {
    if (!('Notification' in window)) {
      return false;
    }
    if (Notification.permission === 'granted') {
      persistPermission('granted');
      return true;
    }
    return false;
  },

  async showLocalNotification(
    title: string,
    body: string,
    options: NotificationOptions = {},
  ): Promise<void> {
    if (!this.isGranted()) {
      return;
    }

    const payload: NotificationOptions = {
      body,
      icon: '/icons/icon-192.png',
      badge: '/icons/favicon-32.png',
      tag: options.tag ?? 'mos-event',
      ...options,
    };

    try {
      const registration = await registerServiceWorker();
      if (registration?.showNotification) {
        await registration.showNotification(title, payload);
        return;
      }
    } catch {
      // fall through to page Notification
    }

    try {
      const notification = new Notification(title, payload);
      notification.onclick = () => {
        window.focus();
        notification.close();
      };
    } catch (error) {
      console.warn('[PWA] Unable to show notification', error);
    }
  },
};
