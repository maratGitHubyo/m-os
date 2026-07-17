import { useEffect, useState, type Dispatch, type SetStateAction } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_URL } from '../api/client';
import { eventStatus, formatEnum } from '../i18n/ru';
import { NotificationService } from '../services/NotificationService';
import { getToken, subscribe, useAuth } from '../stores/authStore';
import type { AppNotificationMessage, GameEventBroadcast } from '../types';

const BANNER_TTL_MS = 4500;

interface BannerNotification {
  id: string;
  message: string;
}

function formatEventNotification(payload: GameEventBroadcast): string {
  const status = formatEnum(payload.status, eventStatus);
  return `${payload.title} — ${status}`;
}

function isAppVisible(): boolean {
  return document.visibilityState === 'visible';
}

function isForCurrentUser(payload: AppNotificationMessage, userId: string | undefined): boolean {
  if (!payload.targetUserId) {
    return true;
  }
  return Boolean(userId) && payload.targetUserId === userId;
}

function pushBanner(
  setNotifications: Dispatch<SetStateAction<BannerNotification[]>>,
  note: BannerNotification,
): void {
  setNotifications((current) => [note, ...current].slice(0, 5));
  window.setTimeout(() => {
    setNotifications((current) => current.filter((item) => item.id !== note.id));
  }, BANNER_TTL_MS);
}

function deliverNotification(
  title: string,
  body: string,
  id: string,
  tag: string,
  setNotifications: Dispatch<SetStateAction<BannerNotification[]>>,
): void {
  if (isAppVisible()) {
    pushBanner(setNotifications, { id, message: body });
  } else {
    void NotificationService.showLocalNotification(title, body, { tag });
  }
}

export function EventNotifications() {
  const { session, user, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<BannerNotification[]>([]);

  useEffect(() => {
    if (!isAuthenticated || !session?.id) {
      return;
    }

    let client: Client | null = null;
    let cancelled = false;
    const userId = user?.id;

    const connect = (token: string | null) => {
      if (!token || cancelled) {
        return;
      }

      client?.deactivate();

      client = new Client({
        webSocketFactory: () => new SockJS(`${API_URL}/ws`),
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
        reconnectDelay: 5000,
        onConnect: () => {
          client?.subscribe(`/topic/session/${session.id}/events`, (message: IMessage) => {
            try {
              const payload = JSON.parse(message.body) as GameEventBroadcast;
              const text = formatEventNotification(payload);
              deliverNotification(
                'Новое событие',
                text,
                `${payload.eventId}-${payload.changedAt}`,
                `mos-event-${payload.eventId}`,
                setNotifications,
              );
            } catch {
              // ignore malformed messages
            }
          });

          client?.subscribe(`/topic/session/${session.id}/notifications`, (message: IMessage) => {
            try {
              const payload = JSON.parse(message.body) as AppNotificationMessage;
              if (!isForCurrentUser(payload, userId)) {
                return;
              }
              deliverNotification(
                payload.title,
                payload.body,
                `${payload.type}-${payload.createdAt}-${payload.targetUserId ?? 'all'}`,
                `mos-app-${payload.type}-${payload.createdAt}`,
                setNotifications,
              );
            } catch {
              // ignore malformed messages
            }
          });
        },
      });

      client.activate();
    };

    connect(getToken());

    const unsubscribe = subscribe(() => {
      connect(getToken());
    });

    return () => {
      cancelled = true;
      unsubscribe();
      client?.deactivate();
    };
  }, [isAuthenticated, session?.id, user?.id]);

  if (notifications.length === 0) {
    return null;
  }

  return (
    <div className="event-notifications" aria-live="polite">
      {notifications.map((note) => (
        <div key={note.id} className="event-notification">
          {note.message}
        </div>
      ))}
    </div>
  );
}
