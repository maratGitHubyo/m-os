# M-OS

**Marat Operating System** — игровая веб-система для проведения дня рождения.

Создаёт дополнительный игровой слой поверх праздника: исследование территории, QR-коды, предметы, М-коины, задания и обмены.

## Requirements

### Production (Docker)

- Docker
- Docker Compose

### Development

- Java 21
- Maven 3.9+
- Node.js 20+
- PostgreSQL 16 (via Docker or local)

---

## Architecture

Backend — единственный источник истины. Frontend — SPA (UI + API only).

```
Browser → nginx (production) → React static / Spring Boot API
                ↓
           PostgreSQL + Flyway
                ↓
         WebSocket /ws (STOMP events)
```

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.4, Java 21 |
| Frontend | React 19, Vite 6, TypeScript |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | JWT (`userId`, `gameSessionId`, `role`) |
| Real-time | WebSocket/STOMP |
| Production | Docker Compose + nginx |
| API docs | OpenAPI / Swagger UI |

---

## Backend modules

| Package | Responsibility |
|---------|----------------|
| `auth` | Login, JWT |
| `user` | User profile |
| `session` | Game session, participants |
| `wallet` | Coins, transactions, M-Coins leaderboard |
| `item` | Templates, inventory, transfer |
| `location` | Map, fog of war |
| `qrcode` | QR scan & rewards |
| `secret` | Player secrets |
| `event` | Game events + WebSocket |
| `numbers` | Collectible numbers |
| `quest` | Quest system |
| `trade` | Player trades |
| `admin` | Admin dashboard & session control |
| `audit` | Audit log |
| `seed` | Demo data (`dev` profile) |

---

## Frontend modules

| Path | Responsibility |
|------|----------------|
| `src/pages/` | Player UI (dashboard, map, inventory, …) |
| `src/pages/admin/` | Admin panel (14 pages) |
| `src/api/` | REST client (auto-JWT) |
| `src/components/ui/` | Shared UI (PageState, Toast, ConfirmDialog, …) |
| `src/stores/` | Auth & toast state |
| `src/types/` | TypeScript DTOs |

---

## Game flow

1. Admin starts game session (`STARTING` → `ACTIVE`)
2. Players log in → JWT bound to session
3. Explore map, scan QR, redeem secrets, collect numbers
4. Complete quests (self-complete social tasks)
5. Trade items/coins with other players
6. Collect M-Coins toward the auction (leaderboard by balance)
7. Admin can pause/finish session, manage entities via admin panel

---

## Authentication

- `POST /api/auth/login` → JWT
- Token contains `userId`, `gameSessionId`, `role`
- Player routes require authentication; `/api/admin/**` requires `ROLE_ADMIN`
- Logout is client-side (stateless JWT)

Demo users (with demo seed):

| User | Password | Role |
|------|----------|------|
| admin | admin123 | ADMIN |
| alice | demo123 | PLAYER |
| bob | demo123 | PLAYER |

---

## Docker

### Production (full stack) — одна команда

```powershell
copy .env.example .env
npm start
```

Или без npm:

```powershell
.\start.ps1
```

Двойной клик по `start.bat` (Windows).

Остановка: `npm run stop` или `docker compose down`

| Service | Role |
|---------|------|
| postgres | Database (`mos_pgdata` volume) |
| backend | Spring Boot JAR |
| frontend | nginx + React build |
| nginx | Reverse proxy (`/` → frontend, `/api` & `/ws` → backend) |

App: http://localhost  
Health: http://localhost/actuator/health  
Swagger: http://localhost/swagger-ui.html (dev/direct backend: http://localhost:8080/swagger-ui.html)

### Development (DB only)

```powershell
docker compose -f docker-compose.dev.yml up -d
```

---

## Development

### Всё сразу (PostgreSQL + Backend + Frontend)

```powershell
npm run dev
```

Откроются два окна терминала (backend и frontend). Frontend: http://localhost:5173

### По отдельности

#### Backend

```powershell
cd backend
mvn spring-boot:run
```

Health: http://localhost:8080/actuator/health

#### Frontend

```powershell
cd frontend
npm install
npm run dev
```

App: http://localhost:5173  
Set `VITE_API_URL=http://localhost:8080` in `frontend/.env`

---

## Testing

### Backend (104 integration tests)

```powershell
cd backend
mvn test
```

Requires PostgreSQL on `localhost:5432` (database `mos`). Demo seed is disabled in test profile.

### Frontend build

```powershell
cd frontend
npm run build
```

### Docker compose validation

```powershell
docker compose config
docker compose up --build
```

---

## Project structure

```
M-OS/
├── backend/                 # Spring Boot API + Dockerfile
│   └── src/main/java/com/mos/
├── frontend/                # React SPA + Dockerfile
├── nginx/                   # Production reverse proxy
├── database/                # Scripts & seeds
├── docs/                    # Extended documentation
├── docker-compose.yml       # Production stack
├── docker-compose.dev.yml   # Dev PostgreSQL only
├── start.ps1 / start.bat    # Запуск production одной командой
└── package.json             # npm start / npm run dev
```

---

## Documentation

- **[User guide (RU) — участники и админ](docs/user-guide.md)**
- [Architecture v4](docs/architecture.md)
- [API reference](docs/api.md)
- [Deployment](docs/deployment.md)
- [Game rules](docs/game-rules.md)
- **Swagger UI** — interactive API docs at `/swagger-ui.html`

---

## Status

**Stage 18** — production hardening: logging, validation, OpenAPI, UX polish, test isolation.
