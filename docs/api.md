# M-OS API

> Документация REST API. Будет дополняться по мере реализации этапов.

## Base URL

```
http://localhost:8080
```

## Health Check

```
GET /actuator/health
```

## Модули (план)

| Модуль | Prefix | Этап |
|--------|--------|------|
| Auth | `/api/auth` | 1 |
| Users | `/api/users` | 1 |
| Sessions | `/api/session` | 1 |
| Wallet | `/api/wallet`, `/api/leaderboard` | 3 |
| Items | `/api/items`, `/api/inventory` | 4 |
| Locations | `/api/locations` | 5 |
| QR | `/api/qr` | 6 |
| Secrets | `/api/secrets` | 7 |
| Events | `/api/events` | 9 |
| Trades | `/api/trades` | 13 |
| Admin | `/api/admin/*` | various |

## WebSocket

```
WS /ws
```

STOMP endpoints — этап 9.

## Формат ошибок

```json
{
  "timestamp": "2026-07-11T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Описание ошибки"
}
```
