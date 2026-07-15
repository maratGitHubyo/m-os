# M-OS Architecture v4

**Marat Operating System** — игровая система для проведения дня рождения.

## Принципы

| Принцип | Решение |
|---------|---------|
| Источник истины | Backend |
| Мультисессионность | `GameSession` + `GameConfig` + `VictoryCondition` |
| Исследование | `LocationPoint` + карта + туман войны |
| Секреты | `PlayerSecret` — персональные коды и награды |
| Аудит | `AuditLog` — все значимые действия |
| Конкурентность | `Wallet.version` — optimistic locking |
| QR-политики | `scanPolicy` — FIRST_PLAYER / EVERY_PLAYER / LIMITED |
| Real-time | WebSocket/STOMP (этап 9) |
| Будущее | `auction/` — заготовка без реализации |

## Структура репозитория

```
M-OS/
├── backend/                     # Spring Boot 3.x, Java 21
├── frontend/                    # React + Vite + TypeScript
├── database/
│   ├── seeds/
│   └── scripts/
├── docs/
├── docker-compose.dev.yml       # PostgreSQL (dev)
└── README.md
```

## Backend слои

```
Controller → Service → Repository → Database
```

Пакеты: `com.mos.{session,user,wallet,item,qrcode,location,secret,event,trade,quest,audit,...}`

## Frontend

React SPA — только отображение и вызов API. Игровая логика на backend.

## Docker (dev)

Этап 0: только PostgreSQL в Docker. Backend и frontend из IDE.

Production Docker + nginx — этап 15 (перед дачей).

## Сущности

### GameSession

| Поле | Описание |
|------|----------|
| id, name, date | Основные данные |
| status | DRAFT, STARTING, ACTIVE, PAUSED, FINISHED |
| mapImageUrl | Карта дачи |

Автоподключение: если одна ACTIVE/STARTING сессия — JWT содержит gameSessionId.

### GameConfig (1:1 с GameSession)

startingCoins, maxTradeOffers, numbersTotal, fogOfWarEnabled, secretsEnabled, leaderboardEnabled, customSettings (JSON).

### Wallet / Leaderboard

Рейтинг строится по балансу M-Coins участников сессии (`GET /api/leaderboard`, только ADMIN).

### User

username, passwordHash, **nickname**, displayName, avatarUrl.

### SessionParticipant

userId, gameSessionId, role, **nicknameSnapshot** (имя на момент входа в сессию).

### Wallet

userId, gameSessionId, balance, **version** (@Version optimistic locking).

### CoinTransaction

История всех операций с М-коинами. Баланс меняется только через транзакции.

### ItemTemplate

name, description, imageUrl, rarity, **isUnique**, gameSessionId.

### PlayerItem / ItemOwnershipHistory

Экземпляры предметов и история владения.

### LocationPoint

name, description, x (0–100%), y (0–100%), zone, hidden, gameSessionId.

### PlayerLocationDiscovery

Туман войны — фиксация открытия точки игроком.

### QrCode

locationPointId, code, rewardType, rewardPayload, **scanPolicy** (FIRST_PLAYER / EVERY_PLAYER / LIMITED), scanLimit.

### PlayerSecret

userId, gameSessionId, type, **code**, **title**, **description**, **rewardType**, **rewardPayload**, used.

### PlayerScore / ScoreTransaction

Категории: TOTAL, EXPLORER, COLLECTOR, TRADER, QUEST.

### Quest / PlayerQuest

Автоматические и административные задания.

### TradeOffer / TradeOfferItem / TradeOfferCoins

Обмены между игроками.

### GameEvent

Базовая сущность событий: ANNOUNCEMENT, BONUS_PERIOD, LOCATION_REVEAL, LEADERBOARD_FREEZE, AUCTION, CUSTOM.

Статусы: SCHEDULED, RUNNING, PAUSED, COMPLETED, CANCELLED.

### AuditLog

userId, gameSessionId, action, entityType, entityId, description, metadata.

### Auction (placeholder)

Пакет `auction/` зарезервирован. AuctionLot, AuctionBid — этап 16+.

## WebSocket

Endpoint: `/ws` (STOMP)

| Destination | Назначение |
|-------------|------------|
| `/topic/session/{id}/broadcast` | Объявления |
| `/user/queue/notifications` | Личные уведомления |
| `/user/queue/rewards` | Моментальные награды |
| `/topic/session/{id}/leaderboard` | Рейтинг |

## ER-диаграмма (упрощённая)

```
GameSession ── GameConfig
GameSession ── VictoryCondition
GameSession ── SessionParticipant ── User
GameSession ── LocationPoint ── QrCode
GameSession ── GameEvent
GameSession ── PlayerSecret
User ── Wallet (version)
User ── PlayerScore
LocationPoint ── PlayerLocationDiscovery
```

## Порядок разработки

| Этап | Содержание |
|------|------------|
| 0 | Scaffold, PostgreSQL, health check |
| 1 | Auth, User, GameSession, GameConfig, SessionParticipant |
| 2 | AuditLog |
| 3 | Wallet + CoinTransaction |
| 4 | Items |
| 5 | LocationPoint + карта |
| 6 | QR-коды |
| 7 | PlayerSecret |
| 8 | PlayerScore |
| 9 | GameEvent + WebSocket |
| 10 | VictoryCondition |
| 11 | Numbers |
| 12 | Quests |
| 13 | Trades |
| 14 | Admin polish + seeds |
| 15 | Production Docker + nginx |
| 16+ | Auction |

## Технологии

**Backend:** Java 21, Spring Boot 3.x, Spring Security, JWT, JPA, PostgreSQL, Flyway, Lombok, Maven.

**Frontend:** React, TypeScript, Vite, Tailwind CSS, shadcn/ui (позже).
