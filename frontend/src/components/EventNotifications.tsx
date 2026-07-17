import { useEffect, useState } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { API_URL } from '../api/client';
import { eventStatus, formatEnum } from '../i18n/ru';
import { NotificationService } from '../services/NotificationService';
import { getToken, subscribe, useAuth } from '../stores/authStore';
import type { GameEventBroadcast } from '../types';

interface EventNotification {
  id: string;
  message: string;
}

function formatNotification(payload: GameEventBroadcast): string {
  const status = formatEnum(payload.status, eventStatus);
  return `${payload.title} — ${status}`;
}

function isAppVisible(): boolean {
  return document.visibilityState === 'visible';
}

export function EventNotifications() {
  const { session, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<EventNotification[]>([]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    void NotificationService.requestPermission();
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated || !session?.id) {
      return;
    }

    let client: Client | null = null;
    let cancelled = false;

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
              const text = formatNotification(payload);
              const note: EventNotification = {
                id: `${payload.eventId}-${payload.changedAt}`,
                message: text,
              };

              if (isAppVisible()) {
                setNotifications((current) => [note, ...current].slice(0, 5));
              } else {
                void NotificationService.showLocalNotification('Новое событие', text, {
                  tag: `mos-event-${payload.eventId}`,
                });
              }
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
  }, [isAuthenticated, session?.id]);

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
