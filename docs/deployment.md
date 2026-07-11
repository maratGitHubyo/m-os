# M-OS Deployment

## Текущий этап (разработка)

### Требования

- Docker Desktop
- Java 21
- Node.js 20+

### PostgreSQL

```bash
docker compose -f docker-compose.dev.yml up -d
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Windows:

```powershell
cd backend
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Проверка

- Backend health: http://localhost:8080/actuator/health
- Frontend: http://localhost:5173

## Production (перед дачей — этап 15)

> Будет добавлено позже.

Планируется:

- `docker-compose.yml` — full stack
- Nginx reverse proxy на порту 80
- Доступ по LAN IP роутера (например `http://192.168.1.105`)
- WebSocket proxy для `/ws`
- `.env.production` для секретов

### LAN-доступ

1. ПК подключён к роутеру
2. Docker поднимает nginx на `:80`
3. Гости открывают `http://<LAN-IP>` с телефонов
4. QR-коды ведут на `http://<LAN-IP>/scan?code=...`

### Firewall (Windows)

Разрешить входящие подключения на порт 80.

### Backup

Скрипты резервного копирования — в `database/scripts/`.
