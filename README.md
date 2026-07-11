# M-OS

**Marat Operating System** — игровая веб-система для проведения дня рождения.

Создаёт дополнительный игровой слой поверх праздника: исследование территории, QR-коды, предметы, М-коины, задания и обмены.

## Требования

- **Java 21**
- **Maven 3.9+**
- **Node.js 20+**
- **Docker Desktop** (для PostgreSQL)

## Быстрый старт

### 1. PostgreSQL

```bash
docker compose -f docker-compose.dev.yml up -d
```

Проверка:

```bash
docker compose -f docker-compose.dev.yml ps
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

Windows:

```powershell
cd backend
mvn spring-boot:run
```

Health check: http://localhost:8080/actuator/health

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Приложение: http://localhost:5173

## Переменные окружения

Скопируйте `.env.example` в `.env` и при необходимости измените значения.

Frontend использует `frontend/.env`:

```
VITE_API_URL=http://localhost:8080
```

## Структура проекта

```
M-OS/
├── backend/          # Spring Boot API
├── frontend/         # React SPA
├── database/         # seeds, scripts
├── docs/             # документация
└── docker-compose.dev.yml
```

## Документация

- [Архитектура v4](docs/architecture.md)
- [API](docs/api.md)
- [Деплой](docs/deployment.md)
- [Правила игры](docs/game-rules.md)

## Статус

**Этап 0** — инфраструктурный scaffold (без бизнес-логики).
