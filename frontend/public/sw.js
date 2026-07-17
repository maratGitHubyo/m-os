const CACHE_VERSION = 'mos-static-v1';
const OFFLINE_URL = '/offline.html';

const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/offline.html',
  '/manifest.webmanifest',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
  '/icons/maskable-512.png',
  '/icons/apple-touch-icon.png',
  '/icons/favicon-32.png',
  '/icons/favicon.ico',
  '/icons/logo-192.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_VERSION).then(async (cache) => {
      await Promise.all(
        PRECACHE_URLS.map(async (url) => {
          try {
            await cache.add(url);
          } catch {
            // skip missing assets during install
          }
        }),
      );
    }),
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(
        keys
          .filter((key) => key !== CACHE_VERSION)
          .map((key) => caches.delete(key)),
      );
      await self.clients.claim();
    })(),
  );
});

function isStaticAsset(url) {
  const { pathname } = url;
  return (
    pathname.startsWith('/assets/') ||
    pathname.startsWith('/icons/') ||
    pathname.endsWith('.js') ||
    pathname.endsWith('.css') ||
    pathname.endsWith('.woff') ||
    pathname.endsWith('.woff2') ||
    pathname.endsWith('.ttf') ||
    pathname.endsWith('.png') ||
    pathname.endsWith('.jpg') ||
    pathname.endsWith('.jpeg') ||
    pathname.endsWith('.svg') ||
    pathname.endsWith('.ico') ||
    pathname.endsWith('.webmanifest')
  );
}

async function cachePut(request, response) {
  if (!response || !response.ok) {
    return;
  }
  const cache = await caches.open(CACHE_VERSION);
  await cache.put(request, response.clone());
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') {
    return;
  }

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) {
    return;
  }

  if (
    url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/ws') ||
    url.pathname.startsWith('/sockjs-node')
  ) {
    return;
  }

  if (request.mode === 'navigate') {
    event.respondWith(
      (async () => {
        try {
          const networkResponse = await fetch(request);
          await cachePut('/index.html', networkResponse);
          return networkResponse;
        } catch {
          const cache = await caches.open(CACHE_VERSION);
          return (
            (await cache.match('/index.html')) ||
            (await cache.match(OFFLINE_URL)) ||
            Response.error()
          );
        }
      })(),
    );
    return;
  }

  if (!isStaticAsset(url)) {
    return;
  }

  event.respondWith(
    (async () => {
      const cache = await caches.open(CACHE_VERSION);
      const cached = await cache.match(request);

      const networkPromise = fetch(request)
        .then(async (response) => {
          await cachePut(request, response);
          return response;
        })
        .catch(() => null);

      if (cached) {
        void networkPromise;
        return cached;
      }

      const networkResponse = await networkPromise;
      if (networkResponse) {
        return networkResponse;
      }

      return (await cache.match(OFFLINE_URL)) || Response.error();
    })(),
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    (async () => {
      const clientsList = await self.clients.matchAll({
        type: 'window',
        includeUncontrolled: true,
      });
      for (const client of clientsList) {
        if ('focus' in client) {
          await client.focus();
          return;
        }
      }
      if (self.clients.openWindow) {
        await self.clients.openWindow('/');
      }
    })(),
  );
});
